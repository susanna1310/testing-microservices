package cancel.integration;

import com.alibaba.fastjson.JSONObject;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/cancelservice(cancel/{orderid}/{loginId} endpoint.
 * This endpoint is responsible for canceling orders. It interacts with external services, such as ts-order-service or ts-inside-payment-service,
 * to validate the cancellation process. The cancellation of an order is handled based on the given orderId and loginId.
 * First the order is tried to be retrieved from ts-order-service. If not found, it is tried to be retrieved from ts-order-other-service.
 * Then the order status is checked, if it is 'NOTPAID', 'PAID' or 'CHANGE', the cancellation is allowed and the status is set to 'CANCEL'.
 * The order status is tried to be updated in the respective order service. If update allowed, the refund amount is calculated.
 * Interacting with the ts-inside-payment-service, the refund amount is tried to be drawn back.
 * If refund process is successful, the user information is fetched sending a GET request to the ts-user-service, to send a notification email.
 *
 * The response to the GET request to this tested endpoint, when the money get drawn back, is Response<>(1, "Success.", null)
 * even if the drawing back money request returns a status 0. So the response message does not make a lot of sense, when responding success, even though the money was not drawn back.
 * We mock the ts-notification-service, because this service tries to send an email.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetCancelTest
{
    @Autowired
    private MockMvc mockMvc;
    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer insidePaymentServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-mongo");

    @Container
    public static MongoDBContainer userServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");


    @Container
    private static GenericContainer<?> insidePaymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-inside-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18673)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service")
            .dependsOn(insidePaymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> userServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-user-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12342)
            .withNetwork(network)
            .withNetworkAliases("ts-user-service")
            .dependsOn(userServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @RegisterExtension
    static WireMockExtension notificationServiceWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(17853)).build();

    @BeforeAll
    static void setUpWireMock() {
        configureFor("localhost", 17853);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.notification.service.url", () -> "localhost");
        registry.add("ts.notification.service.port", () -> "17853");
        registry.add("ts.inside.payment.service.url", insidePaymentServiceContainer::getHost);
        registry.add("ts.inside.payment.service.port", () -> insidePaymentServiceContainer.getMappedPort(18673));
        registry.add("ts.user.service.url", userServiceContainer::getHost);
        registry.add("ts.user.service.port", () -> userServiceContainer.getMappedPort(12342));
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
    }

    String loginId = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";

    /*
     * Test case to verify that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' cannot be canceled through the ts-order-service.
     * The expected behavior is that the message "Order Status Cancel Not Permitted" is returned with status 0.
     * The orderId belongs to an order in the repository in ts-order-service with the order status COLLECTED.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    void testStatusOtherOrderService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d7";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertEquals(response.getData(), null);
    }

    /*
     * Test case to verify that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' cannot be canceled through the ts-order-other-service.
     * The expected behavior is that the message "Order Status Cancel Not Permitted" is returned with status 0.
     * The orderId belongs to an order in the repository in ts-order-other-service with the order status COLLECTED.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    void testStatusOtherOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d4";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' can be successfully canceled through the ts-order-service,
     * even if the payment refund fails. The expected behavior is still to get back a response "Success".
     * We mock the response of ts-notification-service to return true.
     * The orderId belongs to an order in the repository in ts-order-service with the order status NOTPAID.
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success.", null) (even though that response message does not make a lot of sense)
     */
    @Test
    void testStatusNotPaidCancelSuccessOrderService() throws Exception {
        String id = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992b";
        stubNotificationService();


        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));

        verifyStubNotificationService();
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' can be successfully canceled through the ts-order-service,
     * even if the payment refund fails. The expected behavior is still to get back a response "Success".
     * We do not mock the response of ts-notification-service, so the service is not available.
     * The orderId belongs to an order in the repository in ts-order-service with the order status NOTPAID.
     * The test verifies that the test will fail, because of the unavailability of one service.
     */
    @Test
    void invalidTestStatusNotPaidNotificationServiceNotMocked() throws Exception {
        String id = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992b";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertNull(JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' can be successfully canceled through the ts-order-other-service,
     * and the refund successfully drawn back. The expected behavior is to get back a response "Success".
     * The orderId belongs to an order in the repository in ts-order-other-service with the order status NOTPAID.
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success.", null)
     */
    @Test
    void testStatusNotPaidCancelSuccessOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d6";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' cannot be canceled if the user information is incorrect, so when the user cannot be found.
     * The orderId belongs to an order in the repository in ts-order-service with the order status COLLECTED.
     * The order is created in the init class of ts-order-service where the accountId does not match with a user saved in ts-user-service to test the communication between ts-user-service and ts-cancel-service
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Cann't find userinfo by user id.", null)
     */
    @Test
    void testStatusNotPaidWrongUserInfoOrderService() throws Exception {
        String id = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992c";
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Cann't find userinfo by user id.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that the endpoint handles non-existing orderIds correctly. So that no order with that orderId is found in the order service or in the order-other service
     * The orderId belongs neither to an order in the repository in ts-order-service or to an order in the repository in ts-order-other-service
     * The test verifies that the response i equal to the expected response:
     * Response<>(0, "Order Not Found.", null)
     */
    @Test
    void invalidTestNonExistingId() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", UUID.randomUUID().toString(), loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Order Not Found.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    void testOrderContainerStopped() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d7";

        orderServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertNull(response);
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    void testOrderOtherContainerStopped() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d4";

        orderOtherServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", id, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertNull(response);
    }

    private void stubNotificationService() {
        stubFor(post(urlEqualTo("/api/v1/notifyservice/notification/order_cancel_success"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
    }

    private void verifyStubNotificationService() {
        verify(postRequestedFor(urlEqualTo("/api/v1/notifyservice/notification/order_cancel_success"))
                .withRequestBody(containing("fdse_microservice")));
    }
}