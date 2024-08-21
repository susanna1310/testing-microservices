package adminroute.component;

import adminroute.entity.RouteInfo;
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

import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/adminrouteservice/adminroute/{routeId} endpoint.
 * This endpoint send a DELETE request to ts-route-service with a routeId, to delete a specific route object.
 * In the test suites, the response of the route service is always mocked.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminRouteTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private RouteInfo routeInfo;
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

        routeInfo = new RouteInfo();
        routeInfo.setId(UUID.randomUUID().toString());
        routeInfo.setEndStation("muenchen");
        routeInfo.setStationList("mannheim, stuttgart, ulm, augsburg, muenchen");
        routeInfo.setDistanceList("130, 200, 300, 350");
        routeInfo.setStartStation("mannheim");
        routeInfo.setLoginId(UUID.randomUUID().toString());
    }

    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case to validate the DELETE request with a correct RouteInfo object.
     * Ensures that the response status is OK and the delete operation is successful.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<String> response = new Response<>(1, "Delete Success", routeInfo.getId());

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + routeInfo.getId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}", routeInfo.getId())
                .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case to validate the DELETE request with multiple route IDs.
     * Ensures that the response status is Ok, because only the first path variable is used, and the other ones are ignored.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}", 1, 2))
                .andExpect(status().isNotFound());
    }

    /*
     * Test case to validate the DELETE request with a malformed route ID.
     * Ensures that the response status is a client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}", "1/2"))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case to validate the DELETE request with a missing route ID.
     * Ensures that an IllegalArgumentException is thrown.
     */
    @Test
    void invalidTestMissingObject() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case to validate the DELETE request with a non-existing route ID.
     * Ensures that the response contains the appropriate message.
     */
    @Test
    void validTestNonexistingId() throws Exception {
        Response<String> response = new Response<>(0, "Delete failed, Reason unKnown with this routeId", "nonExistingId");

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + "nonExistingId"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}", "nonExistingId")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
}

    /*
     *  Test case to validate the DELETE request with a route ID in an incorrect format or containing special characters.
     * Ensures that the response status is OK.
     */
    @Test
    void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        int nonCorrectFormatId = 1;
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminrouteservice/adminroute/{routeId}", nonCorrectFormatId)
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }
}
