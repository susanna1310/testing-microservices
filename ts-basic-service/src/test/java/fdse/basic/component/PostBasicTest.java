package fdse.basic.component;

import com.alibaba.fastjson.JSONArray;
import edu.fudan.common.util.Response;
import fdse.basic.entity.*;
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
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/basicservice/basic/travel endpoint.
 * The endpoint sends GET requests to the mocked ts-station-service to retrieve the starting and end place id.
 * Sends GET request to the mocked ts-train-service to retrieve the trainType.
 * Sends GET request to the mocked ts-route-service to retrieve the route.
 * Sends GET request to the mocked ts-price-service to retrieve the price config.
 * The endpoint saves a TravelResult which is also send as the data of the response.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostBasicTest
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
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test config for a valid request where all required ojects are correct.
     * This test expects a successful response with the appropriate travel result:
     * Result<>(1, "Success", travelResult)
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse1 = new Response<>(1, "Success", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse2)));


        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(true);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(travel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(travelResult.isStatus()))
                .andExpect(jsonPath("$.data.trainType").value(travelResult.getTrainType()))
                .andExpect(jsonPath("$.data.percent").value(travelResult.getPercent()));

        mockServer.verify();
    }

    /*
     * Test case for an invalid request with multiple objects sent in the body.
     * The test expects a client error response.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(travel);
        jsonArray.add(travel);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request with a malformed JSON object.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{startingPlace: startingPlace, endPlace: endPlace}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for an invalid request with a missing body.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/


    /*
     * Test case for an invalid request where the starting place does not exist.
     * The test expects response status 0 and message indicating that starting place does not exist.
     */
    @Test
    void bodyVar_startingPlace_validTestNotExisting() throws Exception {
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("NotstartingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("NotstartingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse1 = new Response<>(0, "Not exists", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/NotstartingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/NotstartingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse2)));

        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(false);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);


        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0)) // Expecting status 0 (failure)
                .andExpect(jsonPath("$.msg").value("Start place or end place not exist!"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").value(trainType))
                .andExpect(jsonPath("$.data.percent").value(1.0));


        mockServer.verify();
    }


    /*
     * Test case for a valid request with a non-existing trainType id.
     * The test is valid, because when trainType does not exist, it gets replaced with an empty string "".
     */
    @Test
    void bodyVar_traintypeid_validTestNotExisting() throws Exception {
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        Route route = new Route();
        String trainType = "";
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType, trip.getRouteId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse = new Response<>(1, "Success", trip.getStationsId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<Object> trainResponse = new Response<>(0, "here is no TrainType with the trainType id: " + trip.getTrainTypeId(), null);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trainType))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(stationResponse2)));

        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(false);
        travelResult.setPercent(1.0);


        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0)) // Expecting status 0 (failure)
                .andExpect(jsonPath("$.msg").value("Train type doesn't exist"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").isEmpty())
                .andExpect(jsonPath("$.data.percent").value(1.0));


        mockServer.verify();
    }
}
