package fdse.basic.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import fdse.basic.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.HashMap;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/basicservice/basic/travel endpoint.
 * The endpoint sends GET requests to ts-station-service to retrieve the starting and end place id.
 * Sends GET request to ts-train-service to retrieve the trainType.
 * Sends GET request to ts-route-service to retrieve the route.
 * Sends GET request to ts-price-service to retrieve the price config.
 * The endpoint saves a TravelResult which is also send as the data of the response.
 *
 * I changed the directory /src/main/java/fdse.microservice and /test/java/fdse.microservice to /src/main/java/fdse.basic and /test/java/fdse.basic
 * to avoid configuration problems with ts-station-name, because they have the same directory name.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostBasicTest
{
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Network network = Network.newNetwork();
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");

    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);


    @DynamicPropertySource
    private static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port",() -> priceServiceContainer.getMappedPort(16579));
    }


    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request where all attributes of the trip object are correct and match to an object initialized in the repositories of the other services.
     * This test expects a successful response with the appropriate travel result:
     * Result<>(1, "Success", travelResult)
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "454.99999999999994");
        prices.put("confortClass", "1300.0");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(true))
                .andExpect(jsonPath("$.data.trainType").value(trainType))
                .andExpect(jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/


    /*
     * Test case for a request where the starting place does not exist in the repository of ts-station-service.
     * The test expects response status 0 and message indicating that starting place does not exist.
     * The test is still valid, because even when the starting place (and so startingPlaceId) do not exist, an exception is thrown and the prices of economy and comfort are set to 95.0 and 120.0
     */
    @Test
    @Order(2)
    void startingPlaceValidTestNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "notExistingId", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("NotExisting");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0)) // Expecting status 0 (failure)
                .andExpect(jsonPath("$.msg").value("Start place or end place not exist!"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").value(trainType))
                .andExpect(jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

    /*
     * Test case for a request where the trainTypeid does not exist, so no trainType with the given trainTypeId is contained in the repository of ts-train-service.
     * TEST FAILS
     * When trainTypeId and so trainType does not exist (=null), trainTypeString is set to "".
     * The problem now is that when the request to ts-config-service is sent, the url looks like this:
     * "http://" + tsPriceServiceUrl + ":" + tsPriceServicePort + "/api/v1/priceservice/prices/" + routeId + "/" + trainTypeString,
     * e.g. the request is sent like this: "http://localhost:56570/api/v1/priceservice/prices/0b23bd3e-876a-4af3-b920-c50a90c90b04/".
     * Because the trainTypeString is "", nothing is put after the last "/", and so the response status is 404, Not Found and therefore the test fails.
     */
    @Test
    @Order(3)
    void trainTypeIdValidTestNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "NotExisting",  "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0)) // Expecting status 0 (failure)
                .andExpect(jsonPath("$.msg").value("Train type doesn't exist"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").isEmpty())
                .andExpect(jsonPath("$.data.percent").value(1.0));
    }

    /*
     * Test case for a valid request with a non-existing routeId, so no route with that given routeId exists in the repository of ts-route-service.
     * The test is valid, because (in the service implementation class) when the route does not exist, an exception is thrown and the prices of economy and comfort are set to 95.0 and 120.0
     */
    @Test
    @Order(4)
    void routeIdValidTestNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b03"); // Not existing in route Repository

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Bei Jing");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1)) // Expecting status 0 (failure)
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(true))
                .andExpect(jsonPath("$.data.trainType").value(trainType))
                .andExpect(jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(5)
    void testOneContainerStopped() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        routeServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travel))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }
}
