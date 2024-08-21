package adminbasic.component.trains;

import adminbasic.entity.TrainType;
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
import org.mockito.Mock;
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

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/adminbasicservice/adminbasic/trains endpoint.
 * This endpoint send a POST request to ts-train-service to cerate a new train object.
 * The responses of the train service are being mocked in every test case.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminBasicTrainsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private TrainType trainType;

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
        trainType = new TrainType(UUID.randomUUID().toString(), 1, 2, 100);
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test verifies the correct behavior of the POST request to create a new train type object.
     * It mocks the successful response from the train service indicating the creation of the object.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Object> response = new Response<>(1, "create success", null);

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test verifies the behavior when attempting to create multiple train type objects in a single request,
     * which is not supported. It expects a client error response status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray json = new JSONArray();
        json.add(trainType);
        json.add(trainType);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * This test verifies the behavior when attempting to create a train type object that already exists.
     * It mocks the response from the train service indicating that the train type already exists.
     */
    @Test
    void validTestDuplicateObject() throws Exception {
        Response<TrainType> response = new Response<>(0, "train type already exist", trainType);

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<TrainType>>(){}));
    }

    /*
     * This test verifies the behavior when providing a malformed JSON object in the POST request body.
     * It expects a bad request response status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', economyClass: '2'}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    /*
     * [~doc]
     * [doc~]
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
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
     * This test verifies the behavior when sending a POST request with missing JSON payload.
     * It expects a bad request response status.
     */
    @Test
    void bodyVar_id_validTestStringAnyLengthOrSpecialCharacter() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when using various special characters in the 'id' field of the JSON payload.
     * It validates that the API correctly accepts such IDs and processes the request without errors.
     */
    @Test
    void bodyVar_id_invalidTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when the 'id' field in the JSON payload is set to null.
     * It expects the API to handle this scenario and process the request without errors.
     */
    @Test
    void bodyVar_economyclass_validTestValueInRangeAndNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", -2);
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when providing a negative value for 'economyClass' field in the JSON payload.
     * It ensures that the API correctly accepts negative values within the expected range and processes the request without errors.
     */
    @Test
    void bodyVar_economyclass_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", "shouldNotBeString");
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * This test verifies the behavior when providing a string value instead of an integer for 'economyClass' field in the JSON payload.
     * It expects a bad request response status as the API should reject incorrect variable types.
     */
    @Test
    void bodyVar_economyclass_invalidTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", null);
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
