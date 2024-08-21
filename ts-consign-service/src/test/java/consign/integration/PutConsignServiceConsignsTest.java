package consign.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import consign.entity.Consign;
import consign.entity.ConsignRecord;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to update a consign record based on a given consign via PUT.
 * It communicates with the ts-consign-price-service to update the consign price if needed.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PutConsignServiceConsignsTest extends BaseIntegrationTest {

    private final String url = "/api/v1/consignservice/consigns";


    /*
     * The test verifies the functionality of updating a consign object through a PUT request .
     * It ensures that when a valid consign object is updated, the endpoint correctly processes the request, updates the corresponding consign record in the repository,
     * and returns the updated consign record.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception {
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(2.0);
        consignRepository.save(consignRecord);
        consignRecord.setWeight(1.0);
        consignRecord.setPrice(8.0);


        String jsonRequest = objectMapper.writeValueAsString(consign);
        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Update consign success", consignRecord), JSONObject.parseObject(result,  new TypeReference<Response<ConsignRecord>>(){}));
    }

    /*
     * The test verifies the functionality of inserting a new consign record if the original record is not found.
     */
    @Test
    @Order(2)
    void validTestConsignRecordNotFound() throws Exception {
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setPrice(8.0);
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setAccountId(consign.getAccountId());

        String jsonRequest = objectMapper.writeValueAsString(consign);
        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<ConsignRecord> response = JSONObject.parseObject(result, new TypeReference<Response<ConsignRecord>>() {});
        consignRecord.setId(response.getData().getId());
        Assertions.assertEquals(new Response<>(1, "You have consigned successfully! The price is " + consignRecord.getPrice(), consignRecord), response);
    }

    /*
     * The test is designed as part of defect-based to  ensure the system handles edge case values correctly.
     *  Since the values are not tested in the implementation , MAX_VALUE is allowed.
     */
    @Test
    @Order(3)
    void validTestEdgeCaseWeight() throws Exception {
        Consign consign = createSampleConsign();
        consign.setWeight(Double.MAX_VALUE);
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(1.7976931348623157E308);
        consignRecord.setPrice(Double.MAX_VALUE);
        consignRepository.save(consignRecord);

        String jsonRequest = objectMapper.writeValueAsString(consign);
        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<ConsignRecord> response = JSONObject.parseObject(result, new TypeReference<Response<ConsignRecord>>() {});
        consignRecord.setId(response.getData().getId());
        Assertions.assertEquals(new Response<>(1, "Update consign success", consignRecord), response);
        Assertions.assertEquals(Double.MAX_VALUE, response.getData().getWeight());
    }

    /*
     * This test ensures that the application handles scenarios where the
     * ts-consign-price-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test is designed as part of defect-based testing to
     * simulate real-world scenarios where service dependencies may fail.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(4)
    void testServiceUnavailable() throws Exception {
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(2.0);
        consignRepository.save(consignRecord);
        consignRecord.setWeight(1.0);
        consignRecord.setPrice(8.0);

        // Stop the consign price service container to simulate service unavailability
        consignPriceContainer.stop();

        String jsonRequest = objectMapper.writeValueAsString(consign);
        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();

        Response<?> response = JSONObject.parseObject(result, new TypeReference<Response<?>>() {});
        Assertions.assertEquals(0, response.getStatus());
        //Just example response, how case could be handled in the implementation
        Assertions.assertEquals("Consign price service unavailable. Please try again later.", response.getMsg());
    }
}
