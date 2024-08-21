package admintravel.component;

import admintravel.entity.*;
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
import java.util.Arrays;
import java.util.Date;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/admintravelservice/admintravel endpoint.
 * This endpoint retrieves all travel information from both ts-travel-service and ts-travel2-service and saves objects from both services in one arraylist.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAdminTravelTest
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
     * Validates the retrieval of all travelInfo objects from two different services (`ts-travel-service` and `ts-travel2-service`).
     * Mocks responses from both services with predefined travel information and verifies that the response status OK ,
     * and the returned data matches the expected values.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        TravelInfo travelInfo = new TravelInfo("1234", "G1234", "trainType", "routeId", "startingStation", "stations", "terminalStation", new Date(), new Date());
        TravelInfo travelInfo2 = new TravelInfo("321", "B1234", "trainType2", "route2", "startingStation2", "sttions2", "terminalStation2", new Date(), new Date());

        ArrayList<AdminTrip> adminTripsFromService = new ArrayList<>();
        AdminTrip adminTrip = new AdminTrip();
        adminTrip.setTrip(new Trip(new TripId(travelInfo.getTripId()), travelInfo.getTrainTypeId(), travelInfo.getRouteId()));
        adminTrip.setTrainType(new TrainType(adminTrip.getTrip().getTrainTypeId(), 1, 2));

        Route route = new Route();
        route.setId(adminTrip.getTrip().getRouteId());
        route.setStations(new ArrayList<>(Arrays.asList("Station1", "Station2", "Station3")));
        route.setDistances(new ArrayList<>(Arrays.asList(10, 20, 30)));
        route.setTerminalStationId(adminTrip.getTrip().getTerminalStationId());
        route.setStartStationId(adminTrip.getTrip().getStartingStationId());
        adminTrip.setRoute(route);

        adminTripsFromService.add(adminTrip);

        ArrayList<AdminTrip> adminTripsFromService2 = new ArrayList<>();
        AdminTrip adminTrip2 = new AdminTrip();
        adminTrip2.setTrip(new Trip(new TripId(travelInfo2.getTripId()), travelInfo2.getTrainTypeId(), travelInfo2.getRouteId()));
        adminTrip2.setTrainType(new TrainType(adminTrip2.getTrip().getTrainTypeId(), 2, 3));

        Route route2 = new Route();
        route2.setId(adminTrip2.getTrip().getRouteId());
        route2.setStations(new ArrayList<>(Arrays.asList("Station2", "Station3", "Station4")));
        route2.setDistances(new ArrayList<>(Arrays.asList(20, 30, 40)));
        route2.setStartStationId(adminTrip2.getTrip().getStartingStationId());
        route2.setTerminalStationId(adminTrip2.getTrip().getTerminalStationId());
        adminTrip2.setRoute(route2);

        adminTripsFromService2.add(adminTrip2);

        Response<ArrayList<AdminTrip>> expectedResponseFromService = new Response<>(1, "Success", adminTripsFromService);
        Response<ArrayList<AdminTrip>> expectedResponseFromService2 = new Response<>(1, "Travel Service Admin Query All Travel Success", adminTripsFromService2);

        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponseFromService)));

        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponseFromService2)));

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admintravelservice/admintravel")
                .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<ArrayList<AdminTrip>> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<AdminTrip>>>(){});

        Assertions.assertEquals(1, actualResponse.getStatus());
        Assertions.assertEquals(expectedResponseFromService2.getMsg(), actualResponse.getMsg());
        Assertions.assertEquals(2, actualResponse.getData().size());
        Assertions.assertTrue(actualResponse.getData().contains(adminTrip));
        Assertions.assertTrue(actualResponse.getData().contains(adminTrip2));
    }

    /*
     * Tests the scenario where both services return no travelInfo objects. Mocks responses with a status indicating no content,
     * and ensures that the response status OK and an empty list of data is returned.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Response<Object> expectedResponse = new Response<>(0, "No Content", null);
        Response<Object> expectedResponse2 = new Response<>(0, "No Content", null);

        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse2)));

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admintravelservice/admintravel")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<ArrayList<AdminTrip>> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<AdminTrip>>>(){});

        Assertions.assertEquals(0, actualResponse.getStatus());
        Assertions.assertEquals("No Content", actualResponse.getMsg());
        Assertions.assertEquals(new ArrayList<>(), actualResponse.getData()); // Empty Arraylist
    }
}
