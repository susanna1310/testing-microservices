package fdse.basic.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
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
* Test class for GET /api/v1/basicservice/basic/{stationName} endpoint.
* This endpoint sends requests to ts-station-service to retrieve the stationId for the given station name.
*/
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetBasicTest
{
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
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for the station id for a valid station name.
     * This test verifies that the service returns the correct response:
     * Response<>(1, "Success", "muenchen")
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        String stationName = "Muenchen";

        Response<String> expectedResponse = new Response<>(1, "Success", "muenchen");
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + stationName))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case for getting no station id, when the station name does not exist.
     * This test verifies that the service returns the correct response when a non-existent station name is provided:
     * Response<>(0, "Not exists", stationName)
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String stationName = "StationA";

        Response<String> expectedResponse = new Response<>(0, "Not exists", stationName);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + stationName))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for an invalid station name with incorrect format (empty string).
     * This test verifies that the service returns a 404 status when the station name is an empty string.
     */
    @Test
    void invalidTestNonCorrectFormatName() throws Exception {
        String stationName = "";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /*
     * Test case for missing station name parameter in the URL.
     * This test verifies that the service throws an IllegalArgumentException when the station name is missing.
     */
    @Test
    void invalidTestMissingParameter() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/")));
    }

    /*
     * Test case for a malformed station name parameter in the URl.
     * This test verifies that the service returns a 404 status when the parameter is malformed.
     */
    @Test
    void invalidTestMalformedParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/", "invalid/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
