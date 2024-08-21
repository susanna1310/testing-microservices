package travelplan.component;

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
import travelplan.entity.RoutePlanInfo;
import travelplan.entity.RoutePlanResultUnit;
import travelplan.entity.TravelAdvanceResultUnit;
import travelplan.entity.TripInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/travelplanservice/travelPlan/minStation endpoint
 * This endpoint wants to find the travel routes with the min amount of stations between two stations on a specific date and return detailed information about these routes.
 *
 * Multiple requests are sent to different services:
 * A POST request is sent to ts-route-service to get the min amount of routes based on the information in routePlanInfo and a list of RoutePlanResultUnit objects representing these routes is returned.
 * A POST request is sent to ts-station-service to retrieve an arraylist of stops.
 * A GET request is sent to ts-ticketinfo-service to retrieve the ids of the station.
 * A POST request is sent to ts-seat-service to retrieve the number left of tickets for the first and second class
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostTravelPlanMinStationTest {
    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private TripInfo tripInfo;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        tripInfo = new TripInfo("stationA", "stationB", new Date());
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test validates the endpoint with a correct object. It mocks the responses from the external services and checks if the endpoint returns the correct travel information.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        RoutePlanInfo routePlanInfo = new RoutePlanInfo(tripInfo.getStartingPlace(), tripInfo.getEndPlace(), tripInfo.getDepartureTime(), 5);
        ArrayList<RoutePlanResultUnit> routePlanResultUnits = new ArrayList<>();

        RoutePlanResultUnit routePlanResultUnit = new RoutePlanResultUnit();
        routePlanResultUnit.setFromStationName(routePlanInfo.getFormStationName());
        routePlanResultUnit.setToStationName(routePlanInfo.getToStationName());
        routePlanResultUnit.setTripId("tripId1");
        routePlanResultUnit.setStopStations(new ArrayList<>(Arrays.asList("stationA", "stationB")));
        routePlanResultUnit.setPriceForSecondClassSeat("50");
        routePlanResultUnit.setTrainTypeId("G1234");
        routePlanResultUnit.setPriceForSecondClassSeat("25");
        routePlanResultUnit.setStartingTime(tripInfo.getDepartureTime());
        routePlanResultUnit.setEndTime(new Date());

        routePlanResultUnits.add(routePlanResultUnit);

        Response<ArrayList<RoutePlanResultUnit>> responseRoutePlanService = new Response<>(1, "Success.", routePlanResultUnits);
        mockServer.expect(requestTo("http://ts-route-plan-service:14578/api/v1/routeplanservice/routePlan/minStopStations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseRoutePlanService)));

        List<String> stops = new ArrayList<>(Arrays.asList("StationA", "StationB"));
        Response<List<String>> responseStationService = new Response<>(1, "Success", stops);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/namelist"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseStationService)));


        String fromIdA = "stationA";
        String toIdB = "stationB";

        // First class
        Response<String> responseTicketInfoServiceA = new Response<>(1, "Success", fromIdA);
        mockServer.expect(requestTo("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/" + "StationA"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseTicketInfoServiceA)));

        Response<String> responseTicketInfoServiceB = new Response<>(1, "Success", toIdB);
        mockServer.expect(requestTo("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/" + "StationB"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseTicketInfoServiceB)));


        // Set Number of left Tickets to 15;
        int numOfLeftTicketFirst = 15;
        Response<Integer> responseSeatService = new Response<>(1, "Get Left Ticket of Internal Success", numOfLeftTicketFirst);
        mockServer.expect(requestTo("http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseSeatService)));

        // Second class
        Response<String> responseTicketInfoServiceFromA = new Response<>(1, "Success", fromIdA);
        mockServer.expect(requestTo("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/" + "StationA"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseTicketInfoServiceFromA)));

        Response<String> responseTicketInfoServiceToB = new Response<>(1, "Success", toIdB);
        mockServer.expect(requestTo("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/" + "StationB"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseTicketInfoServiceToB)));

        // Set Number of left Tickets to 150;
        int numOfLeftTicketSecond = 150;
        Response<Integer> responseSeatServiceSecond = new Response<>(1, "Get Left Ticket of Internal Success", numOfLeftTicketSecond);
        mockServer.expect(requestTo("http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseSeatServiceSecond)));

        TravelAdvanceResultUnit travelAdvanceResultUnit = new TravelAdvanceResultUnit();
        travelAdvanceResultUnit.setTripId(routePlanResultUnit.getTripId());
        travelAdvanceResultUnit.setToStationName(routePlanResultUnit.getToStationName());
        travelAdvanceResultUnit.setTrainTypeId(routePlanResultUnit.getTrainTypeId());
        travelAdvanceResultUnit.setFromStationName(routePlanResultUnit.getFromStationName());
        travelAdvanceResultUnit.setStopStations(stops);
        travelAdvanceResultUnit.setPriceForFirstClassSeat(routePlanResultUnit.getPriceForFirstClassSeat());
        travelAdvanceResultUnit.setPriceForSecondClassSeat(routePlanResultUnit.getPriceForSecondClassSeat());
        travelAdvanceResultUnit.setStartingTime(routePlanResultUnit.getStartingTime());
        travelAdvanceResultUnit.setEndTime(routePlanResultUnit.getEndTime());
        travelAdvanceResultUnit.setNumberOfRestTicketFirstClass(numOfLeftTicketFirst);
        travelAdvanceResultUnit.setNumberOfRestTicketSecondClass(numOfLeftTicketSecond);

        ArrayList<TravelAdvanceResultUnit> travelAdvanceResultUnitList = new ArrayList<>();
        travelAdvanceResultUnitList.add(travelAdvanceResultUnit);

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tripInfo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<ArrayList<TravelAdvanceResultUnit>> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<TravelAdvanceResultUnit>>>() {
        });
        Assertions.assertEquals(new Response<>(1, "Success", travelAdvanceResultUnitList), actualResponse);
        Assertions.assertTrue(actualResponse.getData().contains(travelAdvanceResultUnit));
    }

    /*
     * This test verifies that sending multiple objects in a single request to the endpoint results in a client error response.
     * It expects the server to return a client error status code indicating a bad request due to multiple objects being sent.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(tripInfo);
        jsonArray.add(tripInfo);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * This test verifies that sending a malformed JSON object to the endpoint results in a bad request response.
     * It expects the server to return a bad request status code due to the malformed JSON structure.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{startingPlace: stationA, endPlace: stationB}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    /*
     * This test verifies that sending a request to the endpoint without the required object results in a bad request response.
     * It expects the server to return a bad request status code indicating that the required object is missing.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * This test verifies the behavior of the endpoint when the starting place variable in the body is empty.
     * It expects the server to return a response indicating that no routes were found ("Cannot Find").
     */
    @Test
    void bodyVar_startingplace_validTestEmpty() throws Exception {
        ArrayList<RoutePlanResultUnit> routePlanResultUnits = new ArrayList<>(); // empty list
        Response<ArrayList<RoutePlanResultUnit>> responseRoutePlanService = new Response<>(1, "Success", routePlanResultUnits);
        mockServer.expect(requestTo("http://ts-route-plan-service:14578/api/v1/routeplanservice/routePlan/minStopStations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseRoutePlanService)));

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tripInfo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> actualResponse = JSONObject.parseObject(response, Response.class);
        Assertions.assertEquals(new Response<>(0, "Cannot Find", null), actualResponse);
    }
}
