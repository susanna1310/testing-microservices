package adminroute.component;

import adminroute.entity.RouteInfo;
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
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/adminrouteservice/adminroute endpoint.
 * This endpoint retrieves all routes from ts-route-service by sending a GET request to that service.
 * The response of ts-route-service is always mocked, in every test case.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAdminRouteTest
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
     * Test case to validate the GET request to retrieve all RouteInfo objects.
     * Ensures that the response status is OK and the returned list of routes matches the expected response.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setId(UUID.randomUUID().toString());
        routeInfo.setEndStation("muenchen");
        routeInfo.setStationList("mannheim, stuttgart, ulm, augsburg, muenchen");
        routeInfo.setDistanceList("130, 200, 300, 350");
        routeInfo.setStartStation("mannheim");
        routeInfo.setLoginId(UUID.randomUUID().toString());

        RouteInfo routeInfo2 = new RouteInfo();
        routeInfo2.setId(UUID.randomUUID().toString());
        routeInfo2.setStartStation("muenchen");
        routeInfo2.setEndStation("mannheim");
        routeInfo2.setStationList("muenchen, augsburg, ulm, stuttgart, mannheim");
        routeInfo2.setDistanceList("50, 150, 230, 350");
        routeInfo2.setLoginId(UUID.randomUUID().toString());

        ArrayList<RouteInfo> routes = new ArrayList<>();
        routes.add(routeInfo);
        routes.add(routeInfo2);

        Response<ArrayList<RouteInfo>> response = new Response<>(1, "Success", routes);

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminrouteservice/adminroute")
                .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<ArrayList<RouteInfo>> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<ArrayList<RouteInfo>>>(){});
        Assertions.assertEquals(response, re);
        Assertions.assertEquals(re.getData().size(), 2);
    }

    /*
     * Test case to validate the GET request when there are no RouteInfo objects.
     * Ensures that the response status is OK and the response indicates "No Content".
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Response<Object> response = new Response<>(0, "No Content", null);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, Response.class));
    }
}
