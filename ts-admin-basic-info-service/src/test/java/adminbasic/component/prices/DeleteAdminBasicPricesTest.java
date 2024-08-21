package adminbasic.component.prices;

import adminbasic.entity.PriceInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.apache.http.HttpHeaders;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/*
 * Test class for DELETE /api/v1/adminbasicservice/adminbasic/prices endpoint
 * This endpoint sends a DELETE request to ts-price-service to delete a specified price object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminBasicPricesTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private PriceInfo price;

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
        price = new PriceInfo();
        price.setId(UUID.randomUUID().toString());
        price.setRouteId("2");
        price.setBasicPriceRate(10.0);
        price.setFirstClassPriceRate(20.0);
        price.setTrainType("trainType");
    }

    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Valid test case for deleting a specific price object.
     * Verifies that the DELETE operation succeeds with an OK status and the response matches expected result.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<PriceInfo> expected = new Response<>(1, "Delete success", price);

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expected)));

        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        String response = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expected, JSONObject.parseObject(response, new TypeReference<Response<PriceInfo>>(){}));
    }

    /*
     * Invalid test case for sending multiple price objects in a single delete request.
     * Verifies that the DELETE operation fails with a 4xx client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(price);
        jsonArray.add(price);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for sending a malformed JSON object.
     * Verifies that the DELETE operation fails with a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: 'Name', routeId: '1'}";

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for sending a delete request with a missing price object.
     * Verifies that the DELETE operation fails with a bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Valid test case for deleting a price object with a special ID containing various characters.
     * Verifies that the DELETE operation succeeds with an OK status.
     */
    @Test
    void bodyVar_id_validTestAnyLengthAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for deleting a price object with a null ID.
     * Verifies that the DELETE operation succeeds with an OK status.
     */
    @Test
    void bodyVar_id_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for deleting a price object with a negative basic price rate.
     * Verifies that the DELETE operation succeeds with an OK status.
     */
    @Test
    void bodyVar_basicpricerate_validTestValueAnyRangeOrNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", -1);
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for sending a delete request with a basic price rate as a string.
     * Verifies that the DELETE operation fails with a bad request status.
     */
    @Test
    void bodyVar_basicpricerate_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", "shouldNotBeString");
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Valid test case for deleting a price object with a null basic price rate.
     * Verifies that the DELETE operation succeeds with an OK status.
     */
    @Test
    void bodyVar_basicpricerate_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", null);
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
