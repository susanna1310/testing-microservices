package admintravel.component;

import admintravel.entity.TravelInfo;
import admintravel.entity.Trip;
import admintravel.entity.TripId;
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
import org.springframework.test.web.client.ExpectedCount;
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
 * Test class for PUT /api/v1/admintravelservice/admintravel endpoint.
 * This endpoint sendsGET request to ts-travel-service or ts-travel2-service, depending on the first letter of tripId,
 * to update a travelInfo object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminTravelTest
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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Test case to validate the PUT request with a correct TravelInfo object for the ts-travel-service.
     * Ensures that the response matches the expected outcome and the trip information is updated correctly.
     */
    @Test
    void validTestCorrectObjectTravelService() throws Exception {
        // Update trip in ts-travel-service
        TripId ti = new TripId(travelInfo.getTripId());
        Trip trip = new Trip(ti, travelInfo.getTrainTypeId(), travelInfo.getStartingStationId(), travelInfo.getStationsId(), travelInfo.getTerminalStationId(), travelInfo.getStartingTime(), travelInfo.getEndTime());
        trip.setRouteId(travelInfo.getRouteId());

        Response<Trip> expectedResponse = new Response<>(1, "Update trip:" + "G1234", trip);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockServer.verify();
        Response<Trip> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Trip>>(){});
        Assertions.assertEquals(expectedResponse, re);
    }

    /*
     * Test case to validate the PUT request with a correct TravelInfo object for the ts-travel2-service.
     * Ensures that the response matches the expected outcome and the trip information is updated correctly.
     */
    @Test
    void validTestCorrectObjectTravel2Service() throws Exception {
        // Update trip in ts-travel2-service
        TripId ti2 = new TripId(travelInfo2.getTripId());
        Trip trip2 = new Trip(ti2, travelInfo2.getTrainTypeId(), travelInfo2.getStartingStationId(),
                travelInfo2.getStationsId(), travelInfo2.getTerminalStationId(), travelInfo2.getStartingTime(), travelInfo2.getEndTime());
        trip2.setRouteId(travelInfo2.getRouteId());

        Response<Trip> expectedResponse2 = new Response<>(1, "Update trip info:" + "B1234", trip2);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json2.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<Trip> re2 = JSONObject.parseObject(actualResponse2, new TypeReference<Response<Trip>>(){});
        Assertions.assertEquals(expectedResponse2, re2);
    }

    /*
     * Test case to validate the PUT request updates the object correctly for ts-travel-service.
     * Ensures the trip information is updated correctly and matches the expected values.
     */
    @Test
    void validTestUpdatesObjectCorrectlyTravelService() throws Exception {
        // Update trip in ts-travel-service
        TripId ti = new TripId(travelInfo.getTripId());
        Trip trip = new Trip(ti, travelInfo.getTrainTypeId(), travelInfo.getStartingStationId(), travelInfo.getStationsId(), travelInfo.getTerminalStationId(), travelInfo.getStartingTime(), travelInfo.getEndTime());
        trip.setRouteId(travelInfo.getRouteId());

        Response<Trip> expectedResponse = new Response<>(1, "Update trip:" + "G1234", trip);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Trip> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<Trip>>(){});
        Assertions.assertEquals(re.getData().getTripId(), trip.getTripId());
        Assertions.assertEquals(re.getData().getStationsId(), trip.getStationsId());
        Assertions.assertEquals(re.getData().getTerminalStationId(), trip.getTerminalStationId());
        Assertions.assertEquals(re.getData().getStartingTime(), trip.getStartingTime());
        Assertions.assertEquals(re.getData().getRouteId(), trip.getRouteId());
        Assertions.assertEquals(re.getData().getEndTime(), trip.getEndTime());
        Assertions.assertEquals(re.getData().getStartingStationId(), trip.getStartingStationId());
    }

    /*
     * Test case to validate the PUT request updates the object correctly for ts-travel2-service.
     * Ensures the trip information is updated correctly and matches the expected values.
     */
    @Test
    void validTestUpdatesObjectCorrectlyTravel2Service() throws Exception {
        // Update trip in ts-travel2-service
        TripId ti2 = new TripId(travelInfo2.getTripId());
        Trip trip2 = new Trip(ti2, travelInfo2.getTrainTypeId(), travelInfo2.getStartingStationId(),
                travelInfo2.getStationsId(), travelInfo2.getTerminalStationId(), travelInfo2.getStartingTime(), travelInfo2.getEndTime());
        trip2.setRouteId(travelInfo2.getRouteId());

        Response<Trip> expectedResponse2 = new Response<>(1, "Update trip info:" + "B1234", trip2);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json2.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<Trip> re2 = JSONObject.parseObject(actualResponse2, new TypeReference<Response<Trip>>(){});
        Assertions.assertEquals(re2.getData().getTripId(), trip2.getTripId());
        Assertions.assertEquals(re2.getData().getStationsId(), trip2.getStationsId());
        Assertions.assertEquals(re2.getData().getTerminalStationId(), trip2.getTerminalStationId());
        Assertions.assertEquals(re2.getData().getStartingTime(), trip2.getStartingTime());
        Assertions.assertEquals(re2.getData().getRouteId(), trip2.getRouteId());
        Assertions.assertEquals(re2.getData().getEndTime(), trip2.getEndTime());
        Assertions.assertEquals(re2.getData().getStartingStationId(), trip2.getStartingStationId());
    }

    /*
     * Test case to validate the PUT request with multiple TravelInfo objects.
     * Ensures that the response status is a client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray  = new JSONArray();
        jsonArray.add(travelInfo);
        jsonArray.add(travelInfo);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case to validate the PUT request with a malformed JSON object.
     * Ensures that the response status is Bad Request.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{tripId: 1, trainTypeId: G1234}";
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    /*
     * [Test case to validate the PUT request with a missing JSON object.
     * Ensures that the response status is Bad Request.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
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
     * Test case to validate the PUT request with a null tripId.
     * Ensures that the response status is OK even when the tripId is null, becaus the object is directly sent to the other service
     * and is not checked in this service.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case to validate the PUT request for a tripId that does not exist in ts-travel-service.
     * Ensures that the response contains the appropriate message.
     */
    @Test
    void bodyVar_tripId_validTestDoesNotExistTravelService() throws Exception {
        // For ts-travel-service
        Response<Object> expectedResponse = new Response<>(1, "Trip" + "G1234" + "doesn 't exists", null);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));

    }

    /*
     * Test case to validate the PUT request for a tripId that does not exist in ts-travel2-service.
     * Ensures that the response contains the appropriate message.
     */
    @Test
    void bodyVar_tripId_validTestDoesNotExistTravel2Service() throws Exception {
        // For ts-travel2-service
        Response<Object> expectedResponse2 = new Response<>(1, "Trip" + "B1234" + "doesn 't exists", null);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
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

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json2.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse2, JSONObject.parseObject(actualResponse2, Response.class));
    }


    /*
     * Test case to validate the PUT request where the startingTime is after the endTime.
     * Ensures that the response status is OK.
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
     * Test case to validate the PUT request with a startingTime in the wrong format.
     * Ensures that the response status is Bad Request.
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
