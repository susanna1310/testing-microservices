package adminbasic.component.configs;

import adminbasic.entity.Config;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import com.alibaba.fastjson.JSONObject;
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
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/*
 * Test class for POST /api/v1/adminbasicservice/adminbasic/configs endpoint.
 * This endpoint send a POST request to ts-configs-service to create a new config object.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminBasicConfigsTest
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
        config = new Config("configName", "12345", "description");
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Verifies the creation of a configuration object with correct data.
     * Sets up a mock server to expect a POST request to create a configuration object in the ts-config-service. Returns a mock response containing the created Config object.
     * Performs a POST request to the endpoint /api/v1/adminbasicservice/adminbasic/configs with JSON payload containing the configuration data. Validates that the response matches the expected Response<Config>.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Config> expectedResponse = new Response<>(1, "Create success", config);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Tests the scenario where multiple configuration objects are attempted to be created.
     * Attempts to perform a POST request with multiple objects saved in the request body.
     * Test expects a client error status.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Tests the scenario where a duplicate configuration object is attempted to be created.
     * Sets up a mock server to expect a POST request to create a configuration object. Returns a mock response indicating that the configuration already exists.
     * Performs a POST request to the endpoint /api/v1/adminbasicservice/adminbasic/configs and validates the response against the expected Response<Object>.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Response<Object> response = new Response<>(0, "Config createdConfig already exists", null);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Tests the scenario where the JSON payload for creating a configuration object is malformed.
     * Performs a POST request with a malformed JSON payload. Expects a Bad Request status code.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{name: 'Name', value: '1'}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests the scenario where the JSON payload for creating a configuration object is missing.
     * Performs a POST request without any JSON payload. Expects a Bad Request status code.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/configs")
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
     * Tests the name field in the JSON payload with special characters.
     * Performs a POST request with a JSON payload containing special characters in the name field. Verifies that the request succeeds with an OK status.
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
     *  Tests the name field in the JSON payload with a string that exceeds the maximum length.
     * Performs a POST request with a JSON payload containing a very long name value. Verifies that the request succeeds with an OK status.
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
     * Performs a POST request with a JSON payload where the name field is set to null. Verifies that the request succeeds with an OK status.
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
