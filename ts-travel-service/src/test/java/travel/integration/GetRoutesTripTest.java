package travel.integration;

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
import travel.entity.Route;
import travel.entity.Trip;
import travel.entity.TripId;
import travel.repository.TripRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration test for the GET Routes Trip Endpoint in TravelService.
 *
 * Following service are connected to this Endpoint:
 * - RouteService
 * - TrainService
 *
 * Test containers are used to create real service instances for testing.
 * MongoDB is used to create a real database instance for testing.
 *
 * Endpoint: "/api/v1/travelservice/routes/{routeId}"
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetRoutesTripTest {
    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");
    @Container
    private static final GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);
    private final String url = "/api/v1/travelservice/routes/{routeId}";
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
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case when
     * the request is valid.
     */
    @Test
    void validTestGetRoute() throws Exception {
        String validToken = generateJwtTokenUser();
        String authorizationHeader = "Bearer " + validToken;
        String tripId = "G1234";

        mockMvc.perform(get(url, tripId)
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case when
     * the request is valid and checks the response data.
     */
    @Test
    void validTestGetCorrectRoute() throws Exception {
        String tripId = "G1234";
        String expectedRouteId = "92708982-77af-4318-be25-57ccb0ff69ad";
        List<String> expectedStations = Arrays.asList("nanjing", "zhenjiang", "wuxi", "suzhou", "shanghai");
        List<Integer> expectedDistances = Arrays.asList(0, 100, 150, 200, 250);

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<Route> response = objectMapper.readValue(result, new TypeReference<Response<Route>>() {
        });

        Route actualRoute = response.getData();
        Assertions.assertEquals(expectedRouteId, actualRoute.getId());
        Assertions.assertEquals("nanjing", actualRoute.getStartStationId());
        Assertions.assertEquals("shanghai", actualRoute.getTerminalStationId());
        Assertions.assertEquals(expectedStations, actualRoute.getStations());
        Assertions.assertEquals(expectedDistances, actualRoute.getDistances());
    }


    /*
     * This test is designed to verify that the endpoint for retrieving the route correctly handles the case when
     * there is no route that matches the trip.
     */
    @Test
    void validTestGetWrongId() throws Exception {
        String tripId = "no-existing";

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<Route> response = objectMapper.readValue(result, new TypeReference<Response<Route>>() {
        });
        Response<Route> expectedResponse = new Response<>(0, "No Content", null);
        Assertions.assertEquals(expectedResponse, response);
    }

    /*
     * This test is designed to verify that the endpoint for retrieving the route correctly handles the case when
     * the request is valid, but there are no routes in the database.
     *
     * This case fails!
     *
     * Actual reponse: Route{id='null', stations=null, distances=null, startStationId='null', terminalStationId='null'}
     */
    @Test
    void invalidTestGetNoRoutes() throws Exception {
        Trip trip = new Trip(new TripId("K1234"), "trainTypeId", "stationA", "stations", "stationB", new Date(), new Date());
        trip.setRouteId(UUID.randomUUID().toString()); // Set a routeId
        tripRepository.save(trip);

        String tripId = "K1234";

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<Route> response = objectMapper.readValue(result, new TypeReference<Response<Route>>() {
        });

        Assertions.assertEquals(new Response<>(0, "No Content", null), response);

        tripRepository.delete(trip);
    }

}


