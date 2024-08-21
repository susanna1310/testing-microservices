package cancel.component;

import cancel.entity.*;
import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/cancelservice(cancel/{orderid}/{loginId} endpoint.
 * This endpoint is responsible for canceling orders. It interacts with mocked external services, such as ts-order-service or ts-inside-payment-service,
 * to validate the cancellation process. The cancellation of an order is handled based on the given orderId and loginId.
 * First the order is tried to be retrieved from ts-order-service. If not found, it is tried to be retrieved from ts-order-other-service.
 * Then the order status is checked, if it is 'NOTPAID', 'PAID' or 'CHANGE', the cancellation is allowed and the status is set to 'CANCEL'.
 * The order status is tried to be updated in the respective order service. If update allowed, the refund amount is calculated.
 * Interacting with the ts-inside-payment-service, the refund amount is tried to be drawn back.
 * If refund process is successful, the user information is fetched sending a GET request to the ts-user-service, to send a notification email.
 *
 * The response to the GET request to this tested endpoint, when the money get drawn back, is Response<>(1, "Success.", null)
 * even if the drawing back money request returns a status 0. So the response message does not make a lot of sense, when responding success, even though the money was not drawn back.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetCancelTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }


    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case to verify that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' (here: 'USED') cannot be canceled through the ts-order-service.
     * The expected behavior is that the message "Order Status Cancel Not Permitted" is returned with status 0.
     * STEPS:
     * 1. Create an order object and set the status to 'USED'
     * 2. Mock the response of the order service to return the order with the given orderId
     * 3. Send a GET request to the tested endpoint
     * Because the order status is set to 'USED', it cannot be changed to 'CANCEL'.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    void validTestStatusOtherOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.USED.getCode());

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
    }

    /*
     * Test case to verify that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' (here: 'USED') cannot be canceled through the ts-order-other-service.
     * The expected behavior is that the message "Order Status Cancel Not Permitted" is returned with status 0.
     * STEPS:
     * 1. Create an order object and set the status to 'USED'
     * 2. Mock the response of the order service to return "Order Not Found" with status 0
     * 3. Mock the response of the order other service to return the order with the given orderId
     * 4. Send a GET request to the tested endpoint
     * Because the order status is set to 'USED', it cannot be changed to 'CANCEL'.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    void validTestStatusOtherOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.USED.getCode());

        Response<Object> responseOrderService = new Response<>(0, "Order Not Found", null);
        Response<Order> responseOrderOtherService = new Response<>(1, "Success", order);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderOtherService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' cannot be canceled if the order update fails in ts-order-service.
     * STEPS:
     * 1. Mock the response of order service to return the order with status 'NOTPAID'
     * 2. Change the order status to 'CANCEL'
     * 3. Mock the response of order-service to not update the order status of the order and return status 0
     * 4. Perform a GET request to the tested endpoint.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0,  "Order Not Found", null)
     */
    @Test
    void validTestStatusNotPaidCancelNotSuccessOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        Response<Object> responseUpdateOrderOrderService = new Response<>(0,  "Order Not Found", null);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUpdateOrderOrderService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(0,  "Order Not Found", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' cannot be canceled if the order update fails in ts-order-other-service.
     * STEPS:
     * 1. Mock the response of order service to "Order Not Found" and status 0
     * 2. Mock the response of order other service to return the order with status 'NOTPAID'
     * 3. Change the order status to 'CANCEL'
     * 4. Mock the response of order other service to not update the order status of the order and return status 0
     * 5. Perform a GET request to the tested endpoint.
     * The test verifies that the response is equal to the expected response:
     * Response<>(0,  "Order Not Found", null)
     */
    @Test
    void validTestStatusNotPaidCancelNotSuccessOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");

        Response<Object> responseOrderService = new Response<>(0, "Order Not Found", null);
        Response<Order> responseOrderOtherService = new Response<>(1, "Success", order);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderOtherService)));

        Response<Object> responseUpdateOrderOrderOtherService = new Response<>(0,  "Order Not Found", null);
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUpdateOrderOrderOtherService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(0, "Fail.Reason:" + "Order Not Found", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' can be successfully canceled through the ts-order-service,
     * even if the payment refund fails. The expected behavior is still to get back a response "Success".
     * We could simulate that the payment refund is successful, but then the test needs to interact with two other services
     * ts-user-service, to retrieve the user and ts-notification-service, to send a notification email.
     * To make the test case not too complicated, (and because the result is the same), we mock the response of the payment service to return status 0.
     * STEPS:
     * 1. Mock the response of order service to return the order with status 'NOTPAID'
     * 2. Change the order status to 'CANCEL'
     * 3. Mock the response of order service to return the successful updated order with status 'CANCEL'
     * 4. Calculate the refund money amount
     * 5. Mock the response of ts-inside-payment-service to return "Draw Back Money Failed" and status 0
     * 6. Send a GET request to the tested endpoint
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success.", null) (even though that response message does not make a lot of sense)
     */
    @Test
    void validTestStatusNotPaidCancelSuccessOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");
        order.setAccountId(UUID.randomUUID());

        String loginId = UUID.randomUUID().toString();

        // Mock the response of the GET request of ts-order-service
        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        order.setStatus(OrderStatus.CANCEL.getCode());
        Order updatedOrder = order;

        // Mock the response of the PUT request to ts-order-service
        Response<Object> responseUpdateOrderOrderService = new Response<>(1, "Success", updatedOrder);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUpdateOrderOrderService)));

        // Mock the response of the GET request to ts-inside-payment-service
        Response<Object> responseInsidePaymentService = new Response<>(0, "Draw Back Money Failed", null);
        mockServer.expect(requestTo("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/" + loginId + "/"
                        + "40.00"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseInsidePaymentService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), loginId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();


        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' can be successfully canceled through the ts-order-other-service,
     * and the refund successfully drawn back. The expected behavior is to get back a response "Success".
     * STEPS:
     * 1. Mock the response of order service to return "Order Not Found" with status 0
     * 1. Mock the response of order other service to return the order with status 'NOTPAID'
     * 2. Change the order status to 'CANCEL'
     * 3. Mock the response of order other service to return the successful updated order with status 'CANCEL'
     * 4. Calculate the refund money amount
     * 5. Mock the response of ts-inside-payment-service to return "Draw Back Money Success" and status 1
     * 6. Send a GET request to the tested endpoint
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success.", null)
     */
    @Test
    void validTestStatusNotPaidCancelSuccessOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");

        Response<Object> responseOrderService = new Response<>(0, "Order Not Found", null);
        Response<Order> responseOrderOtherService = new Response<>(1, "Success", order);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderOtherService)));

        order.setStatus(OrderStatus.CANCEL.getCode());
        Order updatedOrder = order;

        Response<Object> responseUpdateOrderOrderOtherService = new Response<>(1,  "Success", updatedOrder);
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUpdateOrderOrderOtherService)));

        Response<Object> responseInsidePaymentService = new Response<>(1, "Draw Back Money Success", null);
        mockServer.expect(requestTo("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/" + "loginId" + "/"
                        + "40.00"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseInsidePaymentService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that an order with status 'NOTPAID' cannot be canceled if the user information is incorrect, so when the user cannot be found.
     * STEPS:
     * 1. Mock the response of order service to return the order with status 'NOTPAID' with the given orderId
     * 2. Change the order status to 'CANCEL'
     * 3. Mock the response of order service to return the successful updated order with status 'CANCEL'
     * 4. Calculate the refund money amount
     * 5. Mock the response of ts-inside-payment-service to return "Draw Back Money Success" with status 1
     * 6. Mock the response of ts-user-service to return "No User" , so that no user with the accountId of the order is found
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Cann't find userinfo by user id.", null)
     */
    @Test
    void validTestStatusNotPaidWrongUserInfoOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");
        order.setAccountId(UUID.randomUUID());

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        order.setStatus(OrderStatus.CANCEL.getCode());
        Order updatedOrder = order;

        Response<Object> responseUpdateOrderOrderService = new Response<>(1,  "Success", updatedOrder);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUpdateOrderOrderService)));

        Response<Object> responseInsidePaymentService = new Response<>(1, "Draw Back Money Success", null);
        mockServer.expect(requestTo("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/" + order.getAccountId() + "/"
                        + "40.00"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseInsidePaymentService)));

        Response<User> responseUserService = new Response<>(0, "No User", null);
        mockServer.expect(requestTo("http://ts-user-service:12342/api/v1/userservice/users/id/" + order.getAccountId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseUserService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), order.getAccountId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(0, "Cann't find userinfo by user id.", null), JSONObject.parseObject(result, Response.class));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/


    /*
     * Test case to verify that the endpoint handles non-existing orderIds correctly. So that no order with that orderId is found in the order service or in the order-other service
     * Mock the responses of ts-order-service and ts-order-other-service, to return "Order Not Found"
     * The test verifies that the response i equal to the expected response:
     * Response<>(0, "Order Not Found.", null)
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID()); // Not existing

        Response<Object> responseOrderService = new Response<>(0, "Order Not Found", null);
        Response<Object> responseOrderOtherService = new Response<>(0, "Order Not Found", null);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderOtherService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", order.getId(), "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Assertions.assertEquals(new Response<>(0, "Order Not Found.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify that the endpoint handles a missing orderId parameter correctly.
     * The test verifies that te endpoint throws an IllegalArgumentException.
     */
    @Test
    void invalidTestMissingId() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}")));
    }

    /*
     * Test case to verify that the endpoint handles special characters in the orderId correctly.
     * he expected behavior is that a Not Found status is returned.
     * This is the case, because the orderId is of type UUID, and so has the special requirement that it only consists of numbers and letters.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        String uuid = "?=)(/&%ç-+*ç%-&/()-=)(/&%ç*";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", uuid, "loginId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
