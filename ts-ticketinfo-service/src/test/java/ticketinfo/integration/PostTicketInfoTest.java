package ticketinfo.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import ticketinfo.entity.*;

import java.util.Date;
import java.util.HashMap;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/ticketinfoservice/ticketinfo endpoint.
 * This endpoint sends a request to ts-basic-service to retrieve a TravelResult object with attributes status,
 * trainType object and a hashmap with the prices.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostTicketInfoTest
{
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();

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


    @Container
    private static GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @BeforeAll
    static void setup() {
        trainServiceMongoDBContainer.start();
        trainServiceContainer.start();
        routeServiceMongoDBContainer.start();
        routeServiceContainer.start();
        stationServiceMongoDBContainer.start();
        stationServiceContainer.start();
        priceServiceMongoDBContainer.start();
        priceServiceContainer.start();
        basicServiceContainer.start();
    }


    @DynamicPropertySource
    private static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port",() -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port",() -> priceServiceContainer.getMappedPort(16579));
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid travel object with existing tripId, trainTypeId, routeId and station names to find the existing objects in the repositories
     * of ts-route-service, ts-train-service, ts-price-service.
     * The test verifies that the correct response is being returned, containing status true, the trainType, percent 1.0 and hashmap of prices.
     */
    @Test
    @Order(1)
    void validTestCorrectObjects() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "0b23bd3e-876a-4af3-b920-c50a90c90b04");
        trip.setStartingStationId("shanghai");
        trip.setStationsId("nanjing");
        trip.setTerminalStationId("taiyuan");
        trip.setStartingTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        trip.setEndTime(new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "454.99999999999994");
        prices.put("confortClass", "1300.0");

        String jsonString = mapper.writeValueAsString(travel);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Success"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.trainType").value(trainType))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

    /*
     * Test case when an empty travel Object is send with in the body of the request.
     * Because the other requests in the request chain will fail, the response Response<>(null, null, null) should be returned.
     */
    @Test
    @Order(2)
    void validTestZeroObjects() throws Exception {
        Travel travel = new Travel();

        String jsonString = mapper.writeValueAsString(travel);
        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case when the startingPlace of the travel Object does not exist, so no station with that name is contained in the repository of ts-station-service.
     * The test verifies that the correct response is being returned.
     */
    @Test
    @Order(3)
    void validTestStartingPlaceNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa",  "notExistingId", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));

        Travel travel = new Travel();
        travel.setStartingPlace("notExisting");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");

        String jsonString = mapper.writeValueAsString(travel);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Start place or end place not exist!"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.trainType").value(trainType))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

    /*
     * Test case when the trainTypeId does not exists, so no trainType with that id is contained in the repository of ts-train-service.
     *
     * TEST FAILS
     * When trainTypeId and so trainType does note exists (=null), trainTypeString is set to "".
     * The problem now is that when in ts-basic-service the request to ts-config-service is sent, the url looks like this:
     * "http://" + tsPriceServiceUrl + ":" + tsPriceServicePort + "/api/v1/priceservice/prices/" + routeId + "/" + trainTypeString,
     * so that (for example) the request is sent like this: http://localhost:56570/api/v1/priceservice/prices/0b23bd3e-876a-4af3-b920-c50a90c90b04/.
     * Because the trainTypeString is "", nothing is put after the last "/", and so the response status is 404, Not Found and therefore the test fails.
     */
    @Test
    @Order(4)
    void validTestTrainTypeIdNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "NotExisting",  "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(false);
        travelResult.setPercent(1.0);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");

        String jsonString = mapper.writeValueAsString(travel);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Train type doesn't exist"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.trainType").isEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.percent").value(1.0))
                .andExpect(jsonPath("$.data.prices").value(prices));
    }

    /*
     * Test case for a routeId that does not exist, so no route with the given routeId exists in the repository of ts-route-service.
     * The test is valid, because in ts-basic-service, an exception will be thrown when trying to calculate the prices (for what the route is needed)
     * and the prices are set to 95.0 and 120.0
     * The test verifies that the correct response is being returned.
     */
    @Test
    @Order(5)
    void routeIdValidTestNotExisting() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing",  "taiyuan", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b03"); // Not existing in route Repository

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        travel.setTrip(trip);

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE);
        trainType.setAverageSpeed(120);

        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
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
    @Order(6)
    void testPriceContainerStopped() throws Exception {
        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "0b23bd3e-876a-4af3-b920-c50a90c90b04");
        trip.setStartingStationId("shanghai");
        trip.setStationsId("nanjing");
        trip.setTerminalStationId("taiyuan");
        trip.setStartingTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        trip.setEndTime(new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

        Travel travel = new Travel();
        travel.setStartingPlace("Shang Hai");
        travel.setEndPlace("Tai Yuan");
        travel.setTrip(trip);

       priceServiceContainer.stop();

        String jsonString = mapper.writeValueAsString(travel);
        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

}