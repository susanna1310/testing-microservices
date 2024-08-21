package adminbasic.component.configs;

import adminbasic.entity.Config;
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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/adminbasicservice/adminbasic/configs/{name} endpoint.
 * This endpoint sends a DELETE request to ts-configs-service to delete a specified config object with given name.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminBasicConfigsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private Config config;

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
        config = new Config("configName", "123", "description");
    }

    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Verifies the successful deletion of a specific configuration object.
     * Sends a DELETE request with a valid config object name to the endpoint.
     * Expects an OK status and verifies the response structure matches the expected Response<Config>.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Config> expected = new Response<>(1, "Delete success", config);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs/" + config.getName()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expected)));

        String response = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}", config.getName())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expected, JSONObject.parseObject(response, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Tests an valid DELETE request where multiple object names are provided.
     * Expects a OK status, because only the first parameter is used.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}", "name1", "name2"))
                .andExpect(status().isOk());
    }

    /*
     * Tests a scenario where the object name contains malformed characters.
     * Sends a DELETE request with a malformed object name.
     * Expects a Client Error status.
     */
    @Test
    void validTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}", "1/2"))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Validates the handling of an invalid DELETE request where the object name is missing.
     * Expects an IllegalArgumentException to be thrown during the execution of the DELETE request.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Tests the scenario where the DELETE request is made for a non-existing object.
     * Mocks a response indicating that the specified config object (nonExisting) does not exist.
     * Expects an OK status and verifies the response structure matches the expected Response<Object>.
     */
    @Test
    void invalidTestNonexistingName() throws Exception {
        String result = "Config nonExisting doesn't exist.";
        Response<Object> response = new Response<>(0, result, null);
        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs/" + "nonExisting"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}", "nonExisting")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Tests a valid scenario where the object name is provided in a non-correct format (integer instead of string).
     *  Sends a DELETE request with an integer as the object name.
     * Expects an OK status indicating successful deletion, as the integer is automatically converted to a string when forming the URI.
     */
    @Test
    void validTestNonCorrectFormatNameOrSpecialCharacters() throws Exception {
        int nonCorrectFormatName = 1;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/configs/{name}", nonCorrectFormatName)
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
