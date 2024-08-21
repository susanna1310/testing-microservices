package adminorder.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import foodsearch.FoodApplication;
import foodsearch.entity.*;
import org.junit.jupiter.api.*;
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

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all trip food for a given train trip, including available train food and food stores at stations along the route,
 * based on the provided date, start station, end station, and trip ID. To do that it communicates with the ts-food-map-service to get the train foods with the same trip ID,
 * the ts-travel-service to get the route of the trip id and its stations, the ts-station-service to get id form the start and end station. This is used to filter out the station that
 * matches the ids. Lastly it gets all the food stores from the ts-food-map-service and matches them with the station ids, in order to get all trip train foods and food stores.
 * Services in the chain: ts-food-map-service, ts-travel-service, ts-route-service and ts-station-service
 */
@SpringBootTest(classes = { FoodApplication.class })
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetFoodServiceFoodsDateStartStationEndStationTripIdTest {


    private final String url = "/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}";

    private final static Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Container
    public static final MongoDBContainer foodMapServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-food-map-mongo");

    @Container
    public static GenericContainer<?> foodMapContainer = new GenericContainer<>(DockerImageName.parse("local/ts-food-map-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18855)
            .withNetwork(network)
            .withNetworkAliases("ts-food-map-service")
            .dependsOn(foodMapServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static GenericContainer<?> stationContainer = new GenericContainer<>(DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static GenericContainer<?> travelContainer = new GenericContainer<>(DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static GenericContainer<?> routeContainer = new GenericContainer<>(DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);



    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.food.map.service.url", foodMapContainer::getHost);
        registry.add("ts.food.map.service.port", () -> foodMapContainer.getMappedPort(18855));
        registry.add("ts.station.service.url", stationContainer::getHost);
        registry.add("ts.station.service.port", () -> stationContainer.getMappedPort(12345));
        registry.add("ts.travel.service.url", travelContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelContainer.getMappedPort(12346));
        registry.add("ts.route.service.url", routeContainer::getHost);
        registry.add("ts.route.service.port", () -> routeContainer.getMappedPort(11178));
    }

    /*
     * The test is designed to verify that the endpoint for retrieving all trip food works correctly, for all valid path variables that have matching objects in the database
     * It ensures that the endpoint returns a successful response with the appropriate message and all trip food.
     */
    @Test
    @Order(1)
    void validTestGetAllObjects() throws Exception {
        String startStation = "Nan Jing";
        String endStation = "Shang Hai";
        String tripId = "G1234";

        String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(1, response.getStatus());
        Assertions.assertEquals("Get All Food Success", response.getMsg());
        Assertions.assertEquals(1, response.getData().getTrainFoodList().size());
        Assertions.assertEquals("G1234", response.getData().getTrainFoodList().get(0).getTripId());
        Assertions.assertEquals(2, response.getData().getFoodStoreListMap().get("shanghai").size());
        Assertions.assertEquals(3, response.getData().getFoodStoreListMap().get("nanjing").size());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving all trip food works correctly, if there are no station ids found.
     * It ensures that the endpoint returns a successful response with the appropriate message and all trip food.
     */
    @Test
    @Order(2)
    void validTestStationsNotFound() throws Exception {
        String startStation = "Station A";
        String endStation = "Station B";
        String tripId = "G1234";

        String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Get All Food Failed", response.getMsg());
        Assertions.assertEquals(new AllTripFood(), response.getData());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving all trip food works correctly, if there is no route found.
     * It ensures that the endpoint returns a successful response with the appropriate message and all trip food.
     */
    @Test
    @Order(3)
    void validTestNoRouteFound() throws Exception {
        String startStation = "Station A";
        String endStation = "Station B";
        String tripId = "K1245";

        String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Get All Food Failed", response.getMsg());
        Assertions.assertEquals(new AllTripFood(), response.getData());
    }

    /*
     * This defect-based test ensures that the application handles scenarios where the
     * ts-food-map-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(4)
    void testServiceUnavailable() throws Exception {
        String startStation = "Nan Jing";
        String endStation = "Shang Hai";
        String tripId = "G1234";

        // Stop the food map service container to simulate service unavailability
        foodMapContainer.stop();

        String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});

        //Just example response, how case could be handled in the implementation.
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Food map service unavailable. Please try again later.", response.getMsg());
    }
}
