package adminbasic.component.trains;

import adminbasic.entity.TrainType;
import com.alibaba.fastjson.JSONObject;
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
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for PUT /api/v1/adminbasicservice/adminbasic/trains" endpoint.
 * This endpoint send a PUT request to ts-train-service to update a train object.
 * The responses from the train service are being mocked in every test case.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminBasicTrainsTest
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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Valid test case for updating a correct train object.
     * Verifies that the update succeeds with an OK status and the response matches expected result.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Boolean> response = new Response<>(1, "update success", true);
        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("t", trainType);

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Valid test case for updating an object with modified attributes.
     * Verifies that the update succeeds with an OK status.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        TrainType updatedTrainType = new TrainType(trainType.getId(), 3, 4, 200);

        Response<Boolean> response = new Response<>(1, "update success", true);
        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("t", updatedTrainType);

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<Boolean> actualResponse = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(actualResponse.getData(), true);
        // Cannot check if attributes got updated correctly because updated Object is not being sent with in the response
    }

    /*
     * Invalid test case for handling multiple objects in a single request.
     * Expects a 4xx client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String jsonString = "[{\"id\": \"57dbd8af-2bf3-424f-8c32-68b4db4ac3e8-68b4db4ac3e8\",\"economyClass\": \"2\", \"confortClass\": \"1\", \"averageSpeed\": \"100\"}, {\"id\": \"62656158-2e82-424f-8c32-68b4db4ac3e8-68b4db4ac3e8\",\"economyClass\": \"2\", \"confortClass\": \"1\", \"averageSpeed\": \"100\"}]";
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for handling malformed JSON object.
     * Expects a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', economyClass: '2'}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for handling missing JSON object.
     * Expects a bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Valid test case for ID with any length or special characters.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringAnyLengthOrSpecialCharacter() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for ID with null value.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_invalidTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("economyClass", trainType.getEconomyClass());
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for economy class value within range and negative.
     * Expects an OK status.
     */
    @Test
    void bodyVar_economyclass_validTestValueInRangeAndNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", -2);
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for economy class with wrong variable type (string instead of integer).
     * Expects a bad request status.
     */
    @Test
    void bodyVar_economyclass_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", "shouldNotBeString");
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(org.apache.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for economy class with null value.
     * Expects an OK status.
     */
    @Test
    void bodyVar_economyclass_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", trainType.getId());
        json.put("economyClass", null);
        json.put("confortClass", trainType.getConfortClass());
        json.put("averageSpeed", trainType.getAverageSpeed());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/trains")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
