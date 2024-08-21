package adminroute.component;

import adminroute.entity.Route;
import adminroute.entity.RouteInfo;
import com.alibaba.fastjson.JSONArray;
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

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/adminrouteservice/adminroute endpoint.
 * This endpoint send a POST request to ts-route service, to update a route object if that route is already existing,
 * and to create a new route object if not existing yet.
 * The response of ts-route-service is always mocked in the test cases.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminRouteTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private RouteInfo routeInfo;

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

        routeInfo = new RouteInfo();
        routeInfo.setId(UUID.randomUUID().toString());
        routeInfo.setEndStation("muenchen");
        routeInfo.setStationList("mannheim, stuttgart, ulm, augsburg, muenchen");
        routeInfo.setDistanceList("130, 200, 300, 350");
        routeInfo.setStartStation("mannheim");
        routeInfo.setLoginId(UUID.randomUUID().toString());

    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case to validate the POST request with a valid RouteInfo object.
     * This test ensures that when a valid RouteInfo object is posted,
     * the route is either created or updated (in case of duplicate) in the ts-route-service.
     * The response status should be OK and the response should match the expected Route object.
     */
    @Test
    void validTestCorrectObjectAndDuplicateObject() throws Exception {
        Route route = new Route();
        route.setId(route.getId());
        route.setStartStationId(routeInfo.getStartStation());
        route.setTerminalStationId(routeInfo.getEndStation());
        route.setStations(Arrays.asList(routeInfo.getStationList().split(", ")));
        route.setDistances(Arrays.stream(routeInfo.getDistanceList().split(", "))
                .map(Integer::parseInt)
                .collect(Collectors.toList()));

        Response<Route> expectedResponse = new Response<>(1, "Modify success", route);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("request", routeInfo);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Route> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Route>>(){});
        Assertions.assertEquals(expectedResponse, re);
        Assertions.assertEquals(re.getData().getId(), route.getId());
        Assertions.assertEquals(re.getData().getStations(), route.getStations());
        Assertions.assertEquals(re.getData().getDistances(), route.getDistances());
        Assertions.assertEquals(re.getData().getStartStationId(), route.getStartStationId());
        Assertions.assertEquals(re.getData().getTerminalStationId(), route.getTerminalStationId());
    }

    /*
     * Test case to validate the POST request with multiple RouteInfo objects.
     * This test ensures that posting multiple RouteInfo objects at once results in a client error,
     * as the endpoint is expected to handle only single RouteInfo objects.
     * The response status should indicate a client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(routeInfo);
        jsonArray.add(routeInfo);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * [Test case to validate the POST request with a malformed JSON object.
     * This test ensures that when a malformed JSON object is posted,
     * the service responds with a Bad Request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: 1, endStation: muenchen}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to validate the POST request with a missing RouteInfo object.
     * This test ensures that when the request body is missing,
     * the service responds with a Bad Request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
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
     * Test case to validate the POST request with a RouteInfo object having an ID of valid length and characters.
     * This test ensures that when a RouteInfo object with an ID of valid length and special characters is posted,
     * the service correctly processes it and responds with the expected Route object.
     */
    @Test
    void bodyVar_id_validTestCorrectLengthAndAnyCharacters() throws Exception {
        routeInfo.setId("%%%%%%%%%%");
        Route route = new Route();
        route.setId(route.getId());
        route.setStartStationId(routeInfo.getStartStation());
        route.setTerminalStationId(routeInfo.getEndStation());
        route.setStations(Arrays.asList(routeInfo.getStationList().split(", ")));
        route.setDistances(Arrays.stream(routeInfo.getDistanceList().split(", "))
                .map(Integer::parseInt)
                .collect(Collectors.toList()));

        Response<Route> expectedResponse = new Response<>(1, "Modify success", route);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("request", routeInfo);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Route> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Route>>(){});
        Assertions.assertEquals(expectedResponse, re);
        Assertions.assertEquals(re.getData().getId(), route.getId());
        Assertions.assertEquals(re.getData().getStations(), route.getStations());
        Assertions.assertEquals(re.getData().getDistances(), route.getDistances());
        Assertions.assertEquals(re.getData().getStartStationId(), route.getStartStationId());
        Assertions.assertEquals(re.getData().getTerminalStationId(), route.getTerminalStationId());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having an ID that is too short.
     * This test ensures that when a RouteInfo object with an ID shorter than expected is posted,
     * a new route ID is generated and the route is created successfully.
     * The response status should be OK and the response should match the expected Route object.
     */
    @Test
    void bodyVar_id_invalidTestStringTooShort() throws Exception {
        // when length of id of routeInfo < 10, then new route id created and attributes from routId
        routeInfo.setId("123"); //id.length < 10

        Route route = new Route();
        route.setId(UUID.randomUUID().toString());
        route.setStartStationId(routeInfo.getStartStation());
        route.setTerminalStationId(routeInfo.getEndStation());
        route.setStations(Arrays.asList(routeInfo.getStationList().split(", ")));
        route.setDistances(Arrays.stream(routeInfo.getDistanceList().split(", "))
                .map(Integer::parseInt)
                .collect(Collectors.toList()));

        Response<Route> expectedResponse = new Response<>(1, "Save Success", route);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("request", routeInfo);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Route> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Route>>(){});
        Assertions.assertEquals(expectedResponse, re);
        Assertions.assertEquals(re.getData().getId(), route.getId());
        Assertions.assertEquals(re.getData().getStations(), route.getStations());
        Assertions.assertEquals(re.getData().getDistances(), route.getDistances());
        Assertions.assertEquals(re.getData().getStartStationId(), route.getStartStationId());
        Assertions.assertEquals(re.getData().getTerminalStationId(), route.getTerminalStationId());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having a null ID.
     * This test ensures that when a RouteInfo object with a null ID is posted,
     * a new route ID is generated and the route is created successfully.
     * The response status should be OK and the response should match the expected Route object.
     */
    @Test
    void bodyVar_id_invalidTestStringIsNull() throws Exception {
        // when id of routeInfo is null, then new route id and other attributes from routeInfo
        routeInfo.setId(null);

        Route route = new Route();
        route.setId(UUID.randomUUID().toString());
        route.setStartStationId(routeInfo.getStartStation());
        route.setTerminalStationId(routeInfo.getEndStation());
        route.setStations(Arrays.asList(routeInfo.getStationList().split(", ")));
        route.setDistances(Arrays.stream(routeInfo.getDistanceList().split(", "))
                .map(Integer::parseInt)
                .collect(Collectors.toList()));

        Response<Route> expectedResponse = new Response<>(1, "Save Success", route);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("request", routeInfo);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Route> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Route>>(){});
        Assertions.assertEquals(expectedResponse, re);
        Assertions.assertEquals(re.getData().getId(), route.getId());
        Assertions.assertEquals(re.getData().getStations(), route.getStations());
        Assertions.assertEquals(re.getData().getDistances(), route.getDistances());
        Assertions.assertEquals(re.getData().getStartStationId(), route.getStartStationId());
        Assertions.assertEquals(re.getData().getTerminalStationId(), route.getTerminalStationId());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having an ID that is too long.
     * This test ensures that when a RouteInfo object with an excessively long ID is posted,
     * the service responds with a Bad Request status.
     */
    @Test
    void bodyVar_id_invalidTestStringTooLong() throws Exception {
        String jsonString = "{\"loginId\": \"57dbd8af-2bf3-424f-8c32-8763f08c81cc\","+
                "\"startStation\": \"mannheim\","+
                "\"endStation\": \"muenchen\","+
                "\"stationList\": 1, 2, 3, 4,"+
                "\"distanceList\": \"130, 200, 300, 350\", "+
                "\"id\": \"10dd2057-1814-4ab6-ae00-8763f08c81cc-8763f08c81cc\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having a mismatch in the lengths of stationList and distanceList.
     * This test ensures that when the lengths of stationList and distanceList do not match,
     * the service responds with an appropriate error message and status indicating the mismatch.
     */
    @Test
    void bodyVar_stationlistdistancelist_invalidTestNotSameLength() throws Exception {
        routeInfo.setDistanceList("130, 200, 300");

        Response<Object> expectedResponse = new Response<>(0, "Station Number Not Equal To Distance Number", null);
        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("request", routeInfo);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Route> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Route>>(){});
        Assertions.assertEquals(expectedResponse, re);
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having invalid characters in the stationList.
     * This test ensures that when the stationList contains invalid characters,
     * the service responds with a Bad Request status.
     */
    @Test
    void bodyVar_stationlist_invalidTestStringContainsWrongCharacters() throws Exception {
        String jsonString = "{\"id\": \"57dbd8af-2bf3-424f-8c32-8763f08c81cc\","+
                "\"startStation\": \"mannheim\","+
                "\"endStation\": \"muenchen\","+
                "\"stationList\": 1, 2, 3, 4,"+
                "\"distanceList\": \"130, 200, 300, 350\", "+
                "\"loginId\": \"10dd2057-1814-4ab6-ae00-8763f08c81cc\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having an empty stationList.
     * This test ensures that when the stationList is empty,
     * the service responds with a Bad Request status.
     */
    @Test
    void bodyVar_stationlistlist_invalidTestEmptyList() throws Exception {
        String jsonString = "{\"id\": \"57dbd8af-2bf3-424f-8c32-8763f08c81cc\","+
                "\"startStation\": \"mannheim\","+
                "\"endStation\": \"muenchen\","+
                "\"stationList\": ,"+
                "\"distanceList\": \"130, 200, 300, 350\", "+
                "\"loginId\": \"10dd2057-1814-4ab6-ae00-8763f08c81cc\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having a very long startStation string.
     * This test ensures that when the startStation string is very long but within acceptable limits,
     * the service correctly processes the request and responds with an OK status.
     */
    @Test
    void bodyVar_startstation_validTestStringVeryLong() throws Exception {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String tooLongName = new String(chars);

        String jsonString = "{\"id\": \"57dbd8af-2bf3-424f-8c32-8763f08c81cc\","+
                "\"startStation\": \"" + tooLongName + "\","+
                "\"endStation\": \"muenchen\","+
                "\"stationList\": \"mannheim, stuttgart, ulm, augsburg, muenchen\","+
                "\"distanceList\": \"130, 200, 300, 350\","+
                "\"loginId\": \"10dd2057-1814-4ab6-ae00-8763f08c81cc\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case to validate the POST request with a RouteInfo object having a null startStation string.
     * This test ensures that when the startStation string is null,
     * the service responds with a Bad Request status.
     */
    @Test
    void bodyVar_startstation_invalidTestStringIsNull() throws Exception {
        String jsonString = "{\"id\": \"57dbd8af-2bf3-424f-8c32-8763f08c81cc\","+
                "\"startStation\": ,"+
                "\"endStation\": \"muenchen\","+
                "\"stationList\": \"mannheim, stuttgart, ulm, augsburg, muenchen\","+
                "\"distanceList\": \"130, 200, 300, 350\","+
                "\"loginId\": \"10dd2057-1814-4ab6-ae00-8763f08c81cc\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminrouteservice/adminroute")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
