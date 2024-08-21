package admintravel.component;

import admintravel.entity.TravelInfo;
import admintravel.entity.TripId;
import com.alibaba.fastjson.JSONArray;
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
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/admintravelservice/admintravel endpoint.
 * This endpoint allows adding new travel information to two different services (`ts-travel-service` and `ts-travel2-service`),
 * depending on the first letter of tripId attribute.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminTravelTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private TravelInfo travelInfo;
    private TravelInfo travelInfo2;

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

        travelInfo = new TravelInfo();
        travelInfo.setTripId("G1234");
        travelInfo.setTrainTypeId("G1234");
        travelInfo.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        travelInfo.setStartingStationId("muenchen");
        travelInfo.setStationsId("95");
        travelInfo.setTerminalStationId("berlin");
        travelInfo.setStartingTime(new Date(2020, 12, 4, 15, 0));
        travelInfo.setEndTime(new Date(2020, 12, 4, 20, 30));

        travelInfo2 = new TravelInfo();
        travelInfo2.setTripId("B1234");
        travelInfo2.setTrainTypeId("B1234");
        travelInfo2.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        travelInfo2.setStartingStationId("muenchen");
        travelInfo2.setStationsId("95");
        travelInfo2.setTerminalStationId("berlin");
        travelInfo2.setStartingTime(new Date(2020, 12, 4, 15, 0));
        travelInfo2.setEndTime(new Date(2020, 12, 4, 20, 30));
    }


    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Tests the successful addition of travel information to `ts-travel-service`.
     * Mocks a response indicating successful creation and verifies the expected response status and message.
     */
    @Test
    void validTestCorrectObjectTravelService() throws Exception {
        // tripId starts with 'G', add to ts-travel-service
        Response<Object> expectedResponse = new Response<>(1, "Create trip:" + "G1234" + ".", null);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("loginId", travelInfo.getLoginId());
        json.put("tripId", travelInfo.getTripId());
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", travelInfo.getStartingTime());
        json.put("endTime", travelInfo.getEndTime());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> re = JSONObject.parseObject(actualResponse, Response.class);
        Assertions.assertEquals(1, re.getStatus());
        Assertions.assertEquals("[Admin Travel Service][Admin add new travel]", re.getMsg());
        Assertions.assertNull(re.getData());
    }

    /*
     * Tests the successful addition of travel information to `ts-travel2-service`.
     * Mocks a response indicating successful creation and verifies the expected response status and message.
     */
    @Test
    void validTestCorrectObjectTravel2Service() throws Exception {
        // Change tripId to not start with 'G' or 'D' to add travel to ts-travel2-service
        Response<Object> expectedResponse2 = new Response<>(1, "Create trip info:" + "B1234" + ".", null);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse2)));

        JSONObject json2 = new JSONObject();
        json2.put("loginId", travelInfo2.getLoginId());
        json2.put("tripId", travelInfo2.getTripId());
        json2.put("trainTypeId", travelInfo2.getTrainTypeId());
        json2.put("routeId", travelInfo2.getRouteId());
        json2.put("startingStationId", travelInfo2.getStartingStationId());
        json2.put("stationsId", travelInfo2.getStationsId());
        json2.put("terminalStationId", travelInfo2.getTerminalStationId());
        json2.put("startingTime", travelInfo2.getStartingTime());
        json2.put("endTime", travelInfo2.getEndTime());

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json2.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<Object> re2 = JSONObject.parseObject(actualResponse2, Response.class);
        Assertions.assertEquals(1, re2.getStatus());
        Assertions.assertEquals("[Admin Travel Service][Admin add new travel]", re2.getMsg());
        Assertions.assertNull(re2.getData());
    }

    /*
     * Tests the scenario where multiple travelInfo objects are sent in the request body,
     * which should result in a client error response
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(travelInfo);
        jsonArray.add(travelInfo);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admntravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }


    /*
     * Tests the scenario where a duplicate travelInfo object is added to `ts-travel-service`.
     * Mocks a response indicating that the trip already exists and verifies the expected response status and message.
     * The test fails, because the response of ts-travel-service always has status 1.
     */

    @Test
    void validTestDuplicateObjectTravelService() throws Exception {
        // Add duplicate to ts-travel-service
        Response<Object> expectedResponse = new Response<>(1, "Trip " + travelInfo.getTripId() + " already exists", null);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("loginId", travelInfo.getLoginId());
        json.put("tripId", travelInfo.getTripId());
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", travelInfo.getStartingTime());
        json.put("endTime", travelInfo.getEndTime());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockServer.verify();

        Response<Object> re = JSONObject.parseObject(actualResponse, Response.class);
        Assertions.assertEquals(0, re.getStatus());
        Assertions.assertEquals("Admin add new travel failed", re.getMsg());
        Assertions.assertEquals(null, re.getData());
    }

    /*
     * Tests the scenario where a duplicate travelInfo object is added to `ts-travel2-service`.
     *   Mocks a response indicating that the trip already exists and verifies the expected response status and message.
     * Again the test fails, because ts-travel2-service always returns response 1, even when it actually should be 0.
     */
    @Test
    void validTestDuplicateObjectTravel2Service() throws Exception {
        // Add duplicate to ts-travel2-service
        travelInfo.setTrainTypeId("B1234");

        JSONObject json = new JSONObject();
        json.put("loginId", travelInfo.getLoginId());
        json.put("tripId", travelInfo.getTripId());
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", travelInfo.getStartingTime());
        json.put("endTime", travelInfo.getEndTime());

        mockServer = MockRestServiceServer.createServer(restTemplate);

        Response<Object> expectedResponse2 = new Response<>(1, "Trip " + travelInfo.getTripId() + " already exists", null);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse2)));

        json.put("request", travelInfo);
        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> re2 = JSONObject.parseObject(actualResponse2, Response.class);
        Assertions.assertEquals(0, re2.getStatus());
        Assertions.assertEquals("Admin add new travel failed", re2.getMsg());
        Assertions.assertEquals(null, re2.getData());
    }

    /*
     * Tests the scenario where the request body contains malformed JSON data.
     * Verifies that the endpoint returns a bad request response
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{loginId: 1, tripId: G1234}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests the scenario where the request body is missing.
     * Verifies that the endpoint returns a bad request response
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
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
     * Tests the scenario where tripId in the request body is set to null.
     * Verifies that the endpoint handles this case gracefully without errors.
     */
    @Test
    void bodyVar_tripId_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("loginId", "1234");
        json.put("tripId", null);
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", travelInfo.getStartingTime());
        json.put("endTime", travelInfo.getEndTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests the scenario where startingTime in the request body is set after endTime.
     * Verifies that the endpoint handles this case without errors.
     */
    @Test
    void bodyVar_startingtime_validTestAfterEndTime() throws Exception {
        JSONObject json = new JSONObject();
        json.put("loginId", null);
        json.put("tripId", travelInfo.getTripId());
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", travelInfo.getEndTime());
        json.put("endTime", travelInfo.getStartingTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests the scenario where startingTime in the request body is in an incorrect date format.
     * Verifies that the endpoint returns a bad request response
     */
    @Test
    void bodyVar_startingtime_invalidTestDateIsInWrongFormat() throws Exception {
        JSONObject json = new JSONObject();
        json.put("loginId", null);
        json.put("tripId", travelInfo.getTripId());
        json.put("trainTypeId", travelInfo.getTrainTypeId());
        json.put("routeId", travelInfo.getRouteId());
        json.put("startingStationId", travelInfo.getStartingStationId());
        json.put("stationsId", travelInfo.getStationsId());
        json.put("terminalStationId", travelInfo.getTerminalStationId());
        json.put("startingTime", "notADate");
        json.put("endTime", travelInfo.getEndTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
