package adminbasic.component.prices;

import adminbasic.entity.PriceInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for PUT /api/v1/adminbasicservice/adminbasic/prices endpoint
 * This endpoint send a PUT request to ts-price-service to update a specific price object
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminBasicPricesTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private PriceInfo price;

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

        price = new PriceInfo();
        price.setId(UUID.randomUUID().toString());
        price.setRouteId("2");
        price.setBasicPriceRate(10.0);
        price.setFirstClassPriceRate(20.0);
        price.setTrainType("trainType");
    }

    /*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Valid test case for updating a correct price object.
     * Verifies that the PUT operation returns a success response with the updated price details.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        PriceInfo updatedPrice = new PriceInfo();
        updatedPrice.setId(price.getId());
        updatedPrice.setTrainType("traintypeOther");
        updatedPrice.setRouteId("2");
        updatedPrice.setBasicPriceRate(10.5);
        updatedPrice.setFirstClassPriceRate(20.5);

        Response<PriceInfo> response = new Response<>(1, "Update success", updatedPrice);

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<PriceInfo>>(){}));
    }

    /*
     * Valid test case for updating a price object with correct updated values.
     * Verifies that the PUT operation updates the price object correctly.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        PriceInfo updatedPrice = new PriceInfo();
        updatedPrice.setId(price.getId());
        updatedPrice.setBasicPriceRate(200.0);
        updatedPrice.setTrainType("updatedTrainType");
        updatedPrice.setRouteId("3");
        updatedPrice.setFirstClassPriceRate(300.0);

        Response<PriceInfo> expected = new Response<>(1, "Update success", updatedPrice);
        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expected)));

        JSONObject json = new JSONObject();
        json.put("pi", updatedPrice);

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<PriceInfo> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<PriceInfo>>(){});

        Assertions.assertEquals(actualResponse.getData().getBasicPriceRate(), 200.0);
        Assertions.assertEquals(actualResponse.getData().getFirstClassPriceRate(), 300.0);
        Assertions.assertEquals(actualResponse.getData().getRouteId(), "3");
        Assertions.assertEquals(actualResponse.getData().getTrainType(), "updatedTrainType");
    }

    /*
     * Invalid test case for updating multiple price objects.
     * Verifies that attempting to update multiple objects results in a client error
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(price);
        jsonArray.add(price);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for updating a malformed price object.
     * Verifies that attempting to update a malformed JSON results in a bad request response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', routeId: '2'}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for updating a missing price object.
     * Verifies that attempting to update without a JSON body results in a bad request response.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Valid test case for updating a price object with a non existing ID.
     * Verifies that the response is equal to the mocked Response of price service.
     */
    @Test
    void bodyVar_id_validTestNotExisting() throws Exception {
        PriceInfo updatedPrice = new PriceInfo();
        updatedPrice.setId(price.getId());
        updatedPrice.setTrainType("traintypeOther");
        updatedPrice.setRouteId("2");
        updatedPrice.setBasicPriceRate(10.5);
        updatedPrice.setFirstClassPriceRate(20.5);

        Response<PriceInfo> response = new Response<>(0, "Not that config", null);

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("pi", updatedPrice);

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Valid test case for updating a price object with a valid ID of any length and any characters.
     * Verifies that the PUT operation succeeds.
     */
    @Test
    void bodyVar_id_validTestAnyLengthAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for updating a price object with ID as null.
     * Verifies that the PUT operation succeeds.
     */
    @Test
    void bodyVar_id_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", price.getBasicPriceRate());
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     *  Valid test case for updating a price object with basic price rate having a valid value range (including negative).
     * Verifies that the PUT operation succeeds.
     */
    @Test
    void bodyVar_basicpricerate_validTestValueAnyRangeOrNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", -1);
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for updating a price object with basic price rate as a string instead of a number.
     * Verifies that the PUT operation fails with a bad request (400) response.
     */
    @Test
    void bodyVar_basicpricerate_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", "shouldNotBeString");
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Valid test case for updating a price object with basic price rate as null.
     * Verifies that the PUT operation succeeds.
     */
    @Test
    void bodyVar_basicpricerate_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", price.getId());
        json.put("trainType", price.getTrainType());
        json.put("routeId", price.getRouteId());
        json.put("basicPriceRate", null);
        json.put("firstClassPriceRate", price.getFirstClassPriceRate());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/prices")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
