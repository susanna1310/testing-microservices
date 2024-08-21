package adminorder.component;

import adminorder.entity.Order;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/adminorderservice/adminorder/{orderId}/{trainNumber} endpoint.
 * This endpoint send a DELETE request to ts-order-service or ts-order-other-service, depending on the first letter of trainNumber attribute,
 * to delete a specific order.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminOrderTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private Order order;
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }
    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(UUID.randomUUID());
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order.setContactsName("Name");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("FirstClass");
        order.setFrom("berlin");
        order.setTo("muenchen");
        order.setStatus(0);
        order.setPrice("100");
    }

    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Tests the delete operation for a valid order handled by ts-order-service.
     * Verifies that the correct order deletion response is received.
     */
    @Test
    void validTestCorrectObjectOrderService() throws Exception {
        // Train Number starts with 'G', so we expect the response from ts-order-service
        Response<Order> response = new Response<>(1, "Delete Order Success", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId().toString()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", order.getId().toString(), order.getTrainNumber())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Order> r = JSONObject.parseObject(actualResponse, new TypeReference<Response<Order>>(){});
        Assertions.assertEquals(response.getStatus(), r.getStatus());
        Assertions.assertEquals(response.getMsg(), r.getMsg());
        Assertions.assertEquals(response.getData(), r.getData());
    }

    /*
     * Tests the delete operation for a valid order handled by ts-order-other-service.
     * Verifies that the correct order deletion response is received.
     */
    @Test
    void validTestCorrectObjectOrderOtherService() throws Exception {
        // Change Train Number, so it does not start with 'G' or 'D'
        // Now we expect the response from ts-order-other-service
        order.setTrainNumber("B1234");

        Response<String> responseOther = new Response<>(1, "Sucess", order.getId().toString());
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId().toString()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(responseOther)));

        String actualOtherResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", order.getId().toString(), order.getTrainNumber())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<String> rOther = JSONObject.parseObject(actualOtherResponse, Response.class);
        Assertions.assertEquals(responseOther.getStatus(), rOther.getStatus());
        Assertions.assertEquals(responseOther.getMsg(), rOther.getMsg());
        Assertions.assertEquals(responseOther.getData(), rOther.getData());
    }

    /*
     * Tests the case where multiple objects are attempted to be deleted.
     * Verifies that the server responds with a OK status, because the first to parameters are used and the other ones a ignored.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", 1, "G1234", 2, "B1234"))
                .andExpect(status().isOk());
    }

    /*
     * Tests the case where the object ID or train number format is invalid.
     * Verifies that the server responds with a client error status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", "1/2", "G1234/G1234"))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Tests the case where the object ID or train number is missing.
     * Verifies that attempting to perform the delete operation throws an IllegalArgumentException.
     */
    @Test
    void invalidTestMissingObject() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Tests the case where a non-existing object ID is provided, handled by ts-order-service.
     * Verifies that the server responds with an appropriate error message indicating the order does not exist.
     */
    @Test
    void validTestNonexistingIdOrderService() throws Exception {
        // TrainNumber starts with 'G', so ts-order-service
        Response<Object> response = new Response<>(0, "Order Not Exist.", null);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/" + "nonExistingId"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", "nonExistingId", order.getTrainNumber())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Tests the case where a non-existing object ID is provided, handled by ts-order-other-service.
     * Verifies that the server responds with an appropriate error message indicating the order does not exist.
     */
    @Test
    void validTestNonexistingIdOrderOtherService() throws Exception {
        // Change Train Number, so it does not start with 'G' or 'D'
        // Now we expect the response from ts-order-other-service
        order.setTrainNumber("B1234");
        mockServer = MockRestServiceServer.createServer(restTemplate);

        Response<Object> responseOther = new Response<>(0, "Order Not Exist.", null);
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + "nonExistingId"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(responseOther)));

        String actualOtherResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", "nonExistingId", order.getTrainNumber())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(responseOther, JSONObject.parseObject(actualOtherResponse, Response.class));
    }

    /*
     * Tests the case where the object ID or train number format is incorrect or contains special characters.
     * Verifies that the delete operation completes successfully despite the format issue, because the the wrong type (here int) is being casted to an string.
     */
    @Test
    void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        int nonCorrectFormatId = 1;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", nonCorrectFormatId, order.getTrainNumber())
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests the case where the train number format is incorrect or contains special characters.
     * Verifies that the delete operation completes successfully despite the train number format issue.
     * We don't need tests with trainNumber, because trainNumber is not used to find an order
     * Only use is to say which service (ts-order-service or ts-order-other-service) is used
     * Because you only look at the first letter, and not if this trainNumber exists, status is always 200
     * Format tests, when trainNumber does not start with a letter, then ts-order-other-service is used
     */
    @Test
    void validTestNonCorrectFormatOrSpecialCharactersTrainNumber() throws Exception {
        ;mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}", order.getId(), "%%%%%")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
