package travel.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
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
import travel.entity.*;
import travel.repository.TripRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration test for the GET Admin Trip Endpoint in TravelService.
 *
 * Following service are connected to this Endpoint:
 * - RouteService
 * - TrainService
 *
 * Test containers are used to create real service instances for testing.
 * MongoDB is used to create a real database instance for testing.
 *
 * Endpoint: "/api/v1/travelservice/admin_trip"
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetAdminTripTest {
    private final static Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
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
    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");
    @Container
    private static final GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);
    private final String url = "/api/v1/travelservice/admin_trip";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));

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
     * This test ensures that the endpoint correctly returns a response with status 1 and message "Success" for a valid
     * request.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        String validToken = generateJwtTokenUser();
        String authorizationHeader = "Bearer " + validToken;

        mockMvc.perform(get(url)
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * This test ensures that the endpoint correctly returns multiple object with the correct information.
     * And the response status is 1 and message is "Success".
     */
    @Test
    void validTestGetAllObjectsDetail() throws Exception {
        Route route1 = createRoute("92708982-77af-4318-be25-57ccb0ff69ad", "nanjing", "shanghai", Arrays.asList("nanjing", "zhenjiang", "wuxi", "suzhou", "shanghai"), Arrays.asList(0, 100, 150, 200, 250));
        Route route2 = createRoute("a3f256c1-0e43-4f7d-9c21-121bf258101f", "nanjing", "shanghai", Arrays.asList("nanjing", "suzhou", "shanghai"), Arrays.asList(0, 200, 250));
        Route route3 = createRoute("084837bb-53c8-4438-87c8-0321a4d09917", "suzhou", "shanghai", Arrays.asList("suzhou", "shanghai"), Arrays.asList(0, 50));
        Route route4 = createRoute("f3d4d4ef-693b-4456-8eed-59c0d717dd08", "shanghai", "suzhou", Arrays.asList("shanghai", "suzhou"), Arrays.asList(0, 50));

        TrainType trainType1 = createTrainType("GaoTieOne", 250);
        TrainType trainType2 = createTrainType("GaoTieTwo", 200);
        TrainType trainType3 = createTrainType("DongCheOne", 180);

        Trip trip1 = new Trip(new TripId("G1234"), "GaoTieOne", "92708982-77af-4318-be25-57ccb0ff69ad");
        Trip trip2 = new Trip(new TripId("G1236"), "GaoTieOne", "a3f256c1-0e43-4f7d-9c21-121bf258101f");
        Trip trip3 = new Trip(new TripId("G1237"), "GaoTieTwo", "084837bb-53c8-4438-87c8-0321a4d09917");
        Trip trip4 = new Trip(new TripId("D1345"), "DongCheOne", "f3d4d4ef-693b-4456-8eed-59c0d717dd08");

        String result = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn().getResponse().getContentAsString();


        Response<ArrayList<AdminTrip>> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<ArrayList<AdminTrip>>>() {
        });
        assertEquals(4, actualResponse.getData().size());

        assertTripDetails(actualResponse.getData().get(0), trip1, route1, trainType1);
        assertTripDetails(actualResponse.getData().get(1), trip2, route2, trainType1);
        assertTripDetails(actualResponse.getData().get(2), trip3, route3, trainType2);
        assertTripDetails(actualResponse.getData().get(3), trip4, route4, trainType3);
    }

    private Route createRoute(String id, String startStation, String terminalStation, List<String> stations, List<Integer> distances) {
        Route route = new Route();
        route.setId(id);
        route.setStartStationId(startStation);
        route.setTerminalStationId(terminalStation);
        route.setStations(new ArrayList<>(stations));
        route.setDistances(new ArrayList<>(distances));
        return route;
    }

    private TrainType createTrainType(String name, int maxSpeed) {
        return new TrainType(name, Integer.MAX_VALUE, Integer.MAX_VALUE, maxSpeed);
    }

    private void assertTripDetails(AdminTrip actualTrip, Trip expectedTrip, Route expectedRoute, TrainType expectedTrainType) {
        assertEquals(expectedTrip.getTripId(), actualTrip.getTrip().getTripId());
        assertEquals(expectedTrip.getTrainTypeId(), actualTrip.getTrip().getTrainTypeId());
        assertEquals(expectedTrip.getRouteId(), actualTrip.getTrip().getRouteId());
        assertEquals(expectedRoute, actualTrip.getRoute());
        assertEquals(expectedTrainType, actualTrip.getTrainType());
    }


    /*
     * Check if the endpoint returns the correct response when there are no objects in the database.
     *
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        List<Trip> trips = tripRepository.findAll();

        String validToken = generateJwtTokenUser();
        String authorizationHeader = "Bearer " + validToken;

        try {
            tripRepository.deleteAll();

            String result = mockMvc.perform(get(url)
                            .header("Authorization", authorizationHeader))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Response<Route> response = objectMapper.readValue(result, Response.class);
            Assertions.assertEquals(new Response<>(0, "No Content", null), response);
        } finally {
            tripRepository.saveAll(trips);
        }
    }
}

