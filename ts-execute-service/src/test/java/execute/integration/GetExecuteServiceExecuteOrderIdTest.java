package execute.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to get and execute the order with the status COLLECTED and changes it to USED via GET request.
 * To do that it communicates with the ts-order-service and the ts-order-other-service to first get the order and check if the status is COLLECTED
 * and then update the status to USED.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetExecuteServiceExecuteOrderIdTest extends BaseIntegrationTest{

    private final String url = "/api/v1/executeservice/execute/execute/{orderId}";

    /*
     * The equivalence test is designed to verify that the endpoint for executing the order works correctly for all valid path variables that have matching objects in the database for the order service.
     * The test uses the ts-order-service. It ensures that the endpoint returns a successful response with the appropriate message and no content.
     */
    @Test
    @Order(1)
    void validTestOrderService() throws  Exception {
        String result = mockMvc.perform(get(url, "d3c91694-d5b8-424c-8674-e14c89226e49")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
        Assertions.assertEquals(new Response<>(1, "Success.", null), response);
    }

    /*
     * The equivalence test is designed to verify that the endpoint for executing the order works correctly for all valid path variables that have matching objects in the database for the order other service.
     * The test first communicates the ts-order-service and then with the ts-order-other-service. It ensures that the endpoint returns a successful response with the appropriate message and no content.
     */
    @Test
    @Order(2)
    void validTestOrderOtherService() throws  Exception {
        String result = mockMvc.perform(get(url, "4d2a46c7-71cb-4cf1-a5bb-b68406d9da6e")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Success", null), JSONObject.parseObject(result,  new TypeReference<Response<String>>(){}));
    }

    /*
     * The equivalence test is designed to verify that the endpoint for executing the order works correctly for an order that is not found in the database.
     * The test first communicates the ts-order-service and then with the ts-order-other-service. It ensures that the endpoint returns the correct response with the appropriate message and no content.
     */
    @Test
    @Order(3)
    void validTestOrderNotFound() throws  Exception {
        String result = mockMvc.perform(get(url, "4d2a46c7-71cb-4bf1-c2bb-b68406d9da60")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(0, "Order Not Found", null), JSONObject.parseObject(result,  new TypeReference<Response<String>>(){}));
    }

    /*
     * This  defect-based test ensures that the application handles scenarios where the
     * ts-order-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(4)
    void testOrderServiceUnavailable() throws Exception {
        // Stop the order service container to simulate service unavailability
        orderContainer.stop();

        String result = mockMvc.perform(get(url, "d3c91694-d5b8-424c-8674-e14c89226e49")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<?> response = JSONObject.parseObject(result, new TypeReference<Response<?>>() {});
        //Just example response, how case could be handled in the implementation
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Station service unavailable. Please try again later.", response.getMsg());
    }


    /*
     * This  defect-based test ensures that the application handles scenarios where the
     * ts-order-other-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(5)
    void testOrderOtherServiceUnavailable() throws Exception {
        // Stop the order service container to simulate service unavailability
        orderOtherContainer.stop();

        String result = mockMvc.perform(get(url, "4d2a46c7-71cb-4cf1-a5bb-b68406d9da6e")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<?> response = JSONObject.parseObject(result, new TypeReference<Response<?>>() {});
        //Just example response, how case could be handled in the implementation
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Station service unavailable. Please try again later.", response.getMsg());
    }
}
