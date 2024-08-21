package travel2.integration;

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
import travel2.entity.*;
import travel2.repository.TripRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/travel2service/admin_trip endpoint.
 * This endpoint sends requests to ts-train-service and ts-route-service to retrieve all Trips existing and saved as AdminTrips in a list.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetAdminTripTravel2ServiceTest {
    private final static Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
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
    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");
    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);
    private final String url = "/api/v1/travel2service/admin_trip";
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
     * The test is designed to verify that the endpoint works correctly for trips that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and data is not empty.
     */
    @Test
    void validTestGetAllObjectsSuccess() throws Exception {
        mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Travel Service Admin Query All Travel Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint works correctly for trips that exists in the database.
     * It ensures that the endpoint returns a successful response with the correct admin trips.
     */
    @Test
    void validTestGetAllObjectsCorrectly() throws Exception {
        String routeId1 = "0b23bd3e-876a-4af3-b920-c50a90c90b04";
        String routeId2 = "9fc9c261-3263-4bfa-82f8-bb44e06b2f52";
        String routeId3 = "d693a2c5-ef87-4a3c-bef8-600b43f62c68";
        String routeId4 = "20eb7122-3a11-423f-b10a-be0dc5bce7db";
        String routeId5 = "1367db1f-461e-4ab7-87ad-2bcc05fd9cb7";

        String trainTypeId1to3 = "ZhiDa";
        String trainTypeId4 = "TeKuai";
        String trainTypeId5 = "KuaiSu";

        Route route1 = new Route();
        route1.setId(routeId1);
        route1.setStartStationId("shanghai");
        route1.setTerminalStationId("taiyuan");
        route1.setStations(new ArrayList<>(Arrays.asList("shanghai", "nanjing", "shijiazhuang", "taiyuan")));
        route1.setDistances(Arrays.asList(0, 350, 1000, 1300));

        Route route2 = new Route();
        route2.setId(routeId2);
        route2.setStartStationId("nanjing");
        route2.setTerminalStationId("beijing");
        route2.setStations(Arrays.asList("nanjing", "xuzhou", "jinan", "beijing"));
        route2.setDistances(Arrays.asList(0, 500, 700, 1200));

        Route route3 = new Route();
        route3.setId(routeId3);
        route3.setStartStationId("taiyuan");
        route3.setTerminalStationId("shanghai");
        route3.setStations(Arrays.asList("taiyuan", "shijiazhuang", "nanjing", "shanghai"));
        route3.setDistances(Arrays.asList(0, 300, 950, 1300));

        Route route4 = new Route();
        route4.setId(routeId4);
        route4.setStartStationId("shanghai");
        route4.setTerminalStationId("taiyuan");
        route4.setStations(Arrays.asList("shanghai", "taiyuan"));
        route4.setDistances(Arrays.asList(0, 1300));

        Route route5 = new Route();
        route5.setId(routeId5);
        route5.setStartStationId("shanghaihongqiao");
        route5.setTerminalStationId("hangzhou");
        route5.setStations(Arrays.asList("shanghaihongqiao", "jiaxingnan", "hangzhou"));
        route5.setDistances(Arrays.asList(0, 150, 300));

        TrainType trainType1to3 = new TrainType(trainTypeId1to3, Integer.MAX_VALUE, Integer.MAX_VALUE, 120);
        TrainType trainType4 = new TrainType(trainTypeId4, Integer.MAX_VALUE, Integer.MAX_VALUE, 120);
        TrainType trainType5 = new TrainType(trainTypeId5, Integer.MAX_VALUE, Integer.MAX_VALUE, 90);

        Trip trip1 = new Trip(new TripId("Z1234"), trainTypeId1to3, routeId1);
        Trip trip2 = new Trip(new TripId("Z1235"), trainTypeId1to3, routeId2);
        Trip trip3 = new Trip(new TripId("Z1236"), trainTypeId1to3, routeId3);
        Trip trip4 = new Trip(new TripId("T1235"), trainTypeId4, routeId4);
        Trip trip5 = new Trip(new TripId("K1345"), trainTypeId5, routeId5);

        String result = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Travel Service Admin Query All Travel Success"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Response<ArrayList<AdminTrip>> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<ArrayList<AdminTrip>>>() {
        });
        Assertions.assertEquals(5, actualResponse.getData().size());

        Assertions.assertEquals(actualResponse.getData().get(0).getTrip().getTripId(), trip1.getTripId());
        Assertions.assertEquals(actualResponse.getData().get(0).getTrip().getTrainTypeId(), trip1.getTrainTypeId());
        Assertions.assertEquals(actualResponse.getData().get(0).getTrip().getRouteId(), trip1.getRouteId());
        Assertions.assertEquals(actualResponse.getData().get(0).getRoute(), route1);
        Assertions.assertEquals(actualResponse.getData().get(0).getTrainType(), trainType1to3);

        Assertions.assertEquals(actualResponse.getData().get(1).getTrip().getTripId(), trip2.getTripId());
        Assertions.assertEquals(actualResponse.getData().get(1).getTrip().getTrainTypeId(), trip2.getTrainTypeId());
        Assertions.assertEquals(actualResponse.getData().get(1).getTrip().getRouteId(), trip2.getRouteId());
        Assertions.assertEquals(actualResponse.getData().get(1).getRoute(), route2);
        Assertions.assertEquals(actualResponse.getData().get(1).getTrainType(), trainType1to3);

        Assertions.assertEquals(actualResponse.getData().get(2).getTrip().getTripId(), trip3.getTripId());
        Assertions.assertEquals(actualResponse.getData().get(2).getTrip().getTrainTypeId(), trip3.getTrainTypeId());
        Assertions.assertEquals(actualResponse.getData().get(2).getTrip().getRouteId(), trip3.getRouteId());
        Assertions.assertEquals(actualResponse.getData().get(2).getRoute(), route3);
        Assertions.assertEquals(actualResponse.getData().get(2).getTrainType(), trainType1to3);

        Assertions.assertEquals(actualResponse.getData().get(3).getTrip().getTripId(), trip4.getTripId());
        Assertions.assertEquals(actualResponse.getData().get(3).getTrip().getTrainTypeId(), trip4.getTrainTypeId());
        Assertions.assertEquals(actualResponse.getData().get(3).getTrip().getRouteId(), trip4.getRouteId());
        Assertions.assertEquals(actualResponse.getData().get(3).getRoute(), route4);
        Assertions.assertEquals(actualResponse.getData().get(3).getTrainType(), trainType4);

        Assertions.assertEquals(actualResponse.getData().get(4).getTrip().getTripId(), trip5.getTripId());
        Assertions.assertEquals(actualResponse.getData().get(4).getTrip().getTrainTypeId(), trip5.getTrainTypeId());
        Assertions.assertEquals(actualResponse.getData().get(4).getTrip().getRouteId(), trip5.getRouteId());
        Assertions.assertEquals(actualResponse.getData().get(4).getRoute(), route5);
        Assertions.assertEquals(actualResponse.getData().get(4).getTrainType(), trainType5);
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case
     * when there are no trips in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        List<Trip> trips = tripRepository.findAll();
        tripRepository.deleteAll();

        // Delete all Trip objects from repository so that no trips are contained
        String result = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<Route> response = objectMapper.readValue(result, Response.class);
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);

        tripRepository.saveAll(trips);
    }
}

