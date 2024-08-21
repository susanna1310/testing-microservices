package adminbasic.component.configs;

import adminbasic.entity.Config;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for PUT /api/v1/adminbasicservice/adminbasic/configs endpoint.
 * This endpoint send a PUT request to ts-configs-service to update a specified config object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminBasicConfigsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private Config config;

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
        config = new Config("configName", "123", "description");
    }

    /*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Verifies the successful update of a configuration object with correct data.
     * Sets up a mock server to expect a PUT request to update a configuration object in the ts-config-service. Returns a mock response containing the updated Config object.
     * Performs a PUT request to the endpoint /api/v1/adminbasicservice/adminbasic/configs with JSON payload containing the configuration data. Validates that the response matches the expected Response<Config>.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Config> expected = new Response<>(1, "Update success", config);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expected)));

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expected, JSONObject.parseObject(result, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Tests that the updated configuration object matches the expected values after a successful update.
     * Performs a PUT request with a JSON payload containing the configuration data.
     * Expects a successful response with an OK status and verifies that the updated Config object in the response matches the original configuration data.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        Response<Config> expected = new Response<>(1, "Update sucess", config);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expected)));

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Config> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<Config>>() {});

        Assertions.assertEquals(actualResponse.getData().getName(), config.getName());
        Assertions.assertEquals(actualResponse.getData().getValue(), config.getValue());
        Assertions.assertEquals(actualResponse.getData().getDescription(), config.getDescription());
    }

    /*
     * Tests the scenario where multiple configuration objects are attempted to be updated.
     * Test expects a cleint error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Tests the scenario where the JSON payload for updating a configuration object is malformed.
     * Performs a PUT request with a malformed JSON payload. Expects a Bad Request status code.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{name: '1', value: 'Test Name'}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests the scenario where the JSON payload for updating a configuration object is missing.
     * Performs a PUT request without any JSON payload. Expects a Bad Request status code.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
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
     * Tests the scenario where the specified configuration name does not exist for updating.
     *  Sets up a mock server to expect a PUT request to update a configuration object. Returns a mock response indicating that the configuration does not exist.
     * Performs a PUT request to the endpoint /api/v1/adminbasicservice/adminbasic/configs with JSON payload containing the configuration data. Validates that the response matches the expected Response<Object>.
     */
    @Test
    void bodyVar_name_validTestNotExisting() throws Exception {
        Response<Object> expected = new Response<>(0, "Config " + config.getName() + " doesn't exist", null);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expected)));

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expected, JSONObject.parseObject(result, Response.class));
    }

    /*
     *  Tests the name field in the JSON payload with special characters.
     * Performs a PUT request with a JSON payload containing special characters in the name field. Verifies that the request succeeds with an OK status.
     */
    @Test
    void bodyVar_name_validTestCorrectLengthAndSpecialCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name","%%%%");
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests the name field in the JSON payload with a string that exceeds the maximum length.
     * Performs a PUT request with a JSON payload containing a very long name value. Verifies that the request succeeds with an OK status.
     */
    @Test
    void bodyVar_name_validTestStringLong() throws Exception {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String tooLongName = new String(chars);

        JSONObject json = new JSONObject();
        json.put("name", tooLongName);
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests the name field in the JSON payload with a null value.
     * Performs a PUT request with a JSON payload where the name field is set to null. Verifies that the request succeeds with an OK status.
     */
    @Test
    void bodyVar_name_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", null);
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
