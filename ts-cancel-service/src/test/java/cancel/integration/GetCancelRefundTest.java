package cancel.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/cancelservice/cancel/refound/{orderId} endpoint.
 * This test class tests the refund process of the cancellation service.
 * It verifies the correct behaviour of the service under various conditions with the
 * responses from ts-order-service and ts-order-other-service.
 * First a GET request to ts-order-service is sent, to retrieve the order with the given orderId.
 * If the order exists in that service, different OrderStatus are checked.
 * if the order does not exist in that service, a GET request to the ts-order-other-service is sent to retrieve the order.
 * Again if the order exists in that service, different OrderStatus are checked.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetCancelRefundTest
{
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();
    private final static Network network = Network.newNetwork();

    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer =
            new MongoDBContainer("mongo:latest")
                    .withNetwork(network)
                    .withNetworkAliases("ts-order-mongo");


    @Container
    public static final MongoDBContainer orderOtherServiceMongoDBContainer =
            new MongoDBContainer("mongo:latest")
                    .withNetwork(network)
                    .withNetworkAliases("ts-order-other-mongo");


    @Container
    public static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);


    @Container
    public static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @BeforeEach
    void setup() {
        orderServiceContainer.start();
        orderOtherServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for verifying the refund process when the order status is 'NOTPAID' as returned by the ts-order-service.
     * The expected behaviour is that the refund amount should be '0' and status code 1.
     * The orderId used belongs to an order in the repository of ts-order-service with status code NOTPAID.
     * Response<>(1, "Success. Refoud 0", "0")
     */
    @Test
    @Order(1)
    void testOrderStatusNotPaidFromOrderService() throws Exception {
        String id = "5ad7750b-a68b-49c0-a8c0-32776b067703";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success. Refoud 0", "0"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify the refund process when the order status is 'PAID' as returned by the ts-order-service.
     * The expected behaviour is that the refund should be processed successfully with status code 1.
     * The orderId belongs to an order in the repository in ts-order-service with the order status PAID.
     * The data of the response is only checked to be not empty, because it contains the result of the calculateRefund() method, which is private, so cannot be used in this test class.
     */
    @Test
    @Order(2)
    void testOrderStatusPaidFromOrderService() throws Exception {
        String id = "5f50d821-5f22-44f6-b2de-5e79d4b29c68";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<String> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(1, response.getStatus());
        Assertions.assertEquals("Success. ", response.getMsg());
        Assertions.assertEquals("80,00", response.getData());
    }

    /*
     * Test case to verify the refund process when the order status is neither 'NOTPAID' nor 'PAID' (in this case 'CANCEL').
     * The expected behavior is that the refund should not be permitted.
     * The orderId belongs to an order in the repository in ts-order-service with the order status COLLECTED.
     * The test should verify that the response is equal to the expected response:
     * Response<>(0,  "Order Status Cancel Not Permitted, Refound error", null)
     */
    @Test
    @Order(3)
    void testOrderStatusOtherFromOrderService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d7";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted, Refound error");
        Assertions.assertEquals(response.getData(), null);
    }

    /*
     * Test case to verify the refund process when the order with status 'NOTPAID' is not found by the ts-order-service,
     * but by the ts-order-other-service. The expected behavior is that the refund amount is '0'.
     * The orderId belongs to an order in the repository in ts-order-other-service with the order status NOTPAID.
     * The test verifies that the response of the ts-cancel-service is equal to the expected response:
     * Response<>(1, "Success, Refound 0", "0")
     */
    @Test
    @Order(4)
    void testOrderStatusNotPaidFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d6";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success, Refound 0", "0"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify the refund process when the order with order status 'PAID' is not found by the order service,
     * but is found in the ts-order-other-service.
     * The expected behavior is that the refund is processed successfully.
     * The orderId belongs to an order in the repository in ts-order-other-service with the order status PAID.
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success", calculateRefund(order))
     */
    @Test
    @Order(5)
    void testOrderStatusPaidFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d5";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 1);
        Assertions.assertEquals(response.getMsg(), "Success");
        Assertions.assertNotNull(response.getData());
    }

    /*
     * Test case to verify the refund process when the order service does not find the order with status neither 'NOTPAID' nor 'PAID',
     * but the order other service does. The expected behavior is that the refund is not permitted.
     * The orderId belongs to an order in the repository in ts-order-other-service with the order status COLLECTED.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    @Order(6)
    void testOrderStatusOtherFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d4";

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
    }

    /*
     * Test case to verify the refund process when the order with the given orderId does not exist
     * in order service and order other service. The expected behavior is that and "Order Not Found" message should be returned with status 0.
     * The orderId belongs to no order in the repositories of ts-order-service or ts-order-other-service.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Not Found", null)
     */
    @Test
    @Order(7)
    void testNonExistingId() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Order Not Found", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(8)
    void testOrderContainerStopped() throws Exception {
        String id = "5ad7750b-a68b-49c0-a8c0-32776b067703";

        orderServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(9)
    void testOrderOtherContainerStopped() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d5";

        orderOtherServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(new Response<>(null, null, null), response);
    }
}