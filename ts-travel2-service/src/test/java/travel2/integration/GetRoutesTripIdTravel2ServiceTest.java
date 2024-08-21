package travel2.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel2.entity.Route;
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.repository.TripRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/travel2service/routes/{routeId} endpoint.
 * This endpoint send a request to ts-route-service to retrieve the route with the given routeId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetRoutesTripIdTravel2ServiceTest {
    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");
    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);
    private final String url = "/api/v1/travel2service/routes/{routeId}";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178).toString());

        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint for retrieving the route works correctly, for a valid ID with a trip that exists in the database and a matching route.
     * It ensures that the endpoint returns a successful response with the appropriate message and that the data is not empty.
     */
    @Test
    void validTestGetRouteSuccess() throws Exception {
        String tripId = "Z1234";

        mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("[Get Route By Trip ID] Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route works correctly, for a valid ID with a trip that exists in the database and a matching route.
     * It ensures that the endpoint returns a successful response with the correct route.
     */
    @Test
    void validTestGetRouteCorrectly() throws Exception {
        String tripId = "Z1234";

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Route route = new Route();
        route.setId("0b23bd3e-876a-4af3-b920-c50a90c90b04");
        route.setStartStationId("shanghai");
        route.setTerminalStationId("taiyuan");

        route.setStations(new ArrayList<>(Arrays.asList("shanghai", "nanjing", "shijiazhuang", "taiyuan")));
        route.setDistances(new ArrayList<>(Arrays.asList(0, 350, 1000, 1300)));
        Response<Route> response = objectMapper.readValue(result, new TypeReference<Response<Route>>() {
        });
        Assertions.assertEquals(route.getId(), response.getData().getId());
        Assertions.assertEquals(route.getStartStationId(), response.getData().getStartStationId());
        Assertions.assertEquals(route.getTerminalStationId(), response.getData().getTerminalStationId());
        Assertions.assertEquals(route.getStations(), response.getData().getStations());
        Assertions.assertEquals(route.getDistances(), response.getData().getDistances());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case
     * when there is no trip associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetNoRoute() throws Exception {
        String tripId = "Z1239"; // tripId not existing in tripRepository
        // No Trip saved in tripRepository with that tripId

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<Route> response = objectMapper.readValue(result, Response.class);
        Assertions.assertEquals(new Response<>(0, "\"[Get Route By Trip ID] Trip Not Found:\" + tripId", null), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case
     * when there is no route that matches the trip. It ensures that the endpoint returns a response with the appropriate message and no content.
     *
     * The test fails because the getRouteByTripId() function in TravelServiceImpl checks for route != null but the return value from the ts-route-service sets route attributes to null
     * Actual return: Route{id='null', stations=null, distances=null, startStationId='null', terminalStationId='null'}
     */
    @Test
    void validTestGetNoRoutes() throws Exception {
        // Save a new trip in tripRepository with a tripId that does not exist in route service
        Trip trip = new Trip(new TripId("K1234"), "trainTypeId", "stationA", "stations", "stationB", new Date(), new Date());
        trip.setRouteId(UUID.randomUUID().toString());
        tripRepository.save(trip);

        // No route saved in routeRepository with the routeId of trip

        String tripId = "K1234";
        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<Route> response = objectMapper.readValue(result, new TypeReference<Response<Route>>() {
        });

        Assertions.assertEquals(new Response<>(0, "\"[Get Route By Trip ID] Route Not Found:\" + trip.getRouteId()", null), response);

        tripRepository.delete(trip);
    }
}
