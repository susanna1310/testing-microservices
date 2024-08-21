package cancel.component;

import cancel.entity.Order;
import cancel.entity.OrderStatus;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/cancelservice/cancel/refound/{orderId} endpoint.
 * This test class tests the refund process of the cancellation service.
 * It verifies the correct behaviour of the service under various conditions by mocking the
 * responses from ts-order-service and ts-order-other-service with MockRestServiceServer.
 * First a GET request to ts-order-service is sent, to retrieve the order with the given orderId.
 * If the order exists in that service, different OrderStatus are checked.
 * if the order does not exist in that service, a GET request to the ts-order-other-service is sent to retrieve the order.
 * Again if the order exists in that service, different OrderStatus are checked.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetCancelRefoundTest
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
     * Test case for verifying the refund process when the order status is 'NOTPAID' as returned by the ts-order-service.
     * The expected behaviour is that the refund amount should be '0' and status code 1.
     * STEPS:
     * 1. Create an order with status 'NOTPAID'
     * 2. Mock the response of the ts-order-service to return the order with status 1
     * 3. Perform a GET request with the orderId of the order
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success. Refoud 0", "0")
     */
    @Test
    void validTestOrderStatusNotPaidFromOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success. Refoud 0", "0"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify the refund process when the order status is 'PAID' as returned by the ts-order-service.
     * The expected behaviour is that the refund should be processed successfully with status code 1.
     * STEPS:
     * 1. create an order with the status 'PAID' and set travel date, time and price needed by the calculateRefund() method
     * 2. Mock the response of the order service to return the order
     * 3. Perform a GET request with the orderId of the order
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success. ", calculateRefund(order))
     * The data of the response is only checked to be not empty, because it contains the result of the calculateRefund() method, which is private, so cannot be used in this test class.
     */
    @Test
    void validTestOrderStatusPaidFromOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(new Date());
        order.setTravelTime(new Date());
        order.setPrice("50.0");

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Success. "))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty());

        mockServer.verify();
    }

    /*
     * Test case to verify the refund process when the order status is neither 'NOTPAID' nor 'PAID' (in this case 'CANCEL').
     * The expected behavior is that the refund should not be permitted.
     * STEPS:
     * 1. Create an order object with status 'CANCEL'
     * 2. Mock the response of the order service to return the order
     * 3. Perform a GET request the endpoint of the cancel service with the orderId of the order
     * The test should verify that the response is equal to the expected response:
     * Response<>(0,  "Order Status Cancel Not Permitted, Refound error", null)
     */
    @Test
    void validTestOrderStatusOtherFromOrderService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.CANCEL.getCode());

        Response<Order> responseOrderService = new Response<>(1, "Success.", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseOrderService)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted, Refound error");
        Assertions.assertNull(response.getData());
    }

    /*
     * Test case to verify the refund process when the order with status 'NOTPAID' is not found by the ts-order-service,
     * but by the ts-order-other-service. The expected behavior is that the refund amount is '0'.
     * STEPS:
     * 1. Create an order object with order status 'NOTPAID'
     * 2. Mock the response of the order service to return status code 0 and "order not found"
     * 3. Mock the response of the order other service to return the order
     * 4. Perfrom a GET request with the orderId of the order
     * The test verifies that the response of the ts-cancel-service is equal to the expected response:
     * Response<>(1, "Success, Refound 0", "0")
     */
    @Test
    void validTestOrderStatusNotPaidFromOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NOTPAID.getCode());

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

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success, Refound 0", "0"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case to verify the refund process when the order with order status 'PAID' is not found by the order service,
     * but is found in the ts-order-other-service.
     * The expected behavior is that the refund is processed successfully.
     * STEPS:
     * 1. Create an order object with order status 'PAID' and set travelDate, travelTime and price for the calculation of calculateRefund() method
     * 2. Mock the response of the order service to return status code 0 and "order not found"
     * 3. Mock the response of the order other service to return the order
     * 4. Perfrom a GET request with the orderId of the order
     * The test verifies that the response is equal to the expected response:
     * Response<>(1, "Success", calculateRefund(order))
     */
    @Test
    void validTestOrderStatusPaidFromOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PAID.getCode());
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

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 1);
        Assertions.assertEquals(response.getMsg(), "Success");
    }

    /*
     * Test case to verify the refund process when the order service does not find the order with status neither 'NOTPAID' nor 'PAID' (here: 'CANCEL'),
     * but the order other service does. The expected behavior is that the refund is not permitted.
     * STEPS:
     * 1. Create an order object with status 'CANCEL'
     * 2. Mock the response of the order service to return "Order Not Found" with status 0
     * 3. Mock the response of the order other service to return the order
     * 4. Send a GET request to the tested endpoint with the orderId
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Status Cancel Not Permitted", null)
     */
    @Test
    void validTestOrderStatusOtherFromOrderOtherService() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.CANCEL.getCode());

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

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case to verify the refund process when the order with the given orderId does not exist
     * in order service and order other service. The expected behavior is that and "Order Not Found" message should be returned with status 0.
     * STEPS:
     * 1. Create a non-existing orderId
     * 2. Mock the response of the order service to return "Order Not Found" and status 0
     * 3. Mock the response of the order other service to return "Order Not Found" and status 0
     * 4. Perform a GET request to the tested endpoint with the non-existing orderId
     * The test verifies that the response is equal to the expected response:
     * Response<>(0, "Order Not Found", null)
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

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Assertions.assertEquals(new Response<>(0, "Order Not Found", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case that verifies the behavior of the endpoint when the orderId parameter is missing in the request.
     * The expected behavior is that an IllegalArgumentException is thrown.
     */
    @Test
    void invalidTestMissingId() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}")));
    }

    /*
     * Test case to verify that, when multiple orderIds are provided in the request, the response status is OK.
     * Response status is OK because only the first provided orderId gets used and the second one ignored.
     * The expected behavior is that the service processes the request without errors.
     */
    @Test
    void validTestMultipleids() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    /*
     * Test case to verify the behavior of the endpoint when an orderId with special characters is provided.
     * The expected behavior is that a Not Found status is returned.
     * This is the case, because the orderId is of type UUID, and so has the special requirement that it only consists of numbers and letters.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        String uuid = "?=)(/&%ç-+*ç%-&/()-=)(/&%ç*";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cancelservice/cancel/refound/{orderId}", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
