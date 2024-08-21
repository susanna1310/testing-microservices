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
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/adminbasicservice/adminbasic/configs endpoint.
 * This endpoint send a GET request to ts-configs-service to retrieve all existing config objects.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetAdminBasicConfigsTest
{
    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

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
    }

    /*
     * Verifies the retrieval of all configuration objects.
     * Sets up a mock server to expect a GET request to retrieve all configurations from the ts-config-service.
     * Returns a mock response containing a list of Config objects. Performs a GET request to the endpoint /api/v1/adminbasicservice/adminbasic/configs and validates that the response matches the expected Response<List<Config>>.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Config config = new Config("configName", "123", "description");
        Config config2 = new Config("configName2", "321", "description2");

        List<Config> configList = new ArrayList<>();
        configList.add(config);
        configList.add(config2);

        Response<List<Config>> response = new Response<>(1, "Find all  config success", configList);
        String json = JSONObject.toJSONString(response);

        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<List<Config>>>(){}));
    }

    /*
     * Tests the scenario where there are no configuration objects to retrieve
     * Sets up a mock server to expect a GET request to retrieve configurations. Returns a mock response indicating no content.
     * Performs a GET request to the endpoint /api/v1/adminbasicservice/adminbasic/configs and validates that the response matches the expected Response<Object>.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Response<Object> response = new Response<>(0, "No content", null);
        mockServer.expect(requestTo("http://ts-config-service:15679/api/v1/configservice/configs"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsBytes(response)));

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminbasicservice/adminbasic/configs")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }
}
