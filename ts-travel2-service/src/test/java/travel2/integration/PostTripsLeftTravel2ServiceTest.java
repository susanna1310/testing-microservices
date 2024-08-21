package travel2.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import io.swagger.models.auth.In;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel2.entity.*;
import travel2.repository.TripRepository;

import java.util.*;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/travel2service/trips/left endpoint.
 * This endpoint sends a request to ts-ticketinfo-service and ts-order-other-service to retrieve a TripResponse object
 * with attributes confortClass, EconomyClass, StartingStation, TerminalSTation, tripId, trainTypeId, priceForConfortClass and priceForEconomyClass.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostTripsLeftTravel2ServiceTest
{
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer travel2MongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");
    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2MongoDBContainer);
    @Container
    private static final MongoDBContainer configMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");
    @Container
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configMongoDBContainer);

    @Container
    public static MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");
    @Container
    private static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");
    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);
    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");
    @Container
    private static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");
    @Container
    private static GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");

    @Container
    private static GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");
    private final String url = "/api/v1/travel2service/trips/left";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.ticketinfo.service.url", ticketinfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketinfoServiceContainer.getMappedPort(15681));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));

        System.setProperty("spring.data.mongodb.host", travel2MongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", travel2MongoDBContainer.getMappedPort(27017).toString());
        travel2MongoDBContainer.start();
    }

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The test is designed to verify that the endpoint works correctly, for a trip with the given tripId that exists in the database, while
     * communicating with all the needed services.
     * It ensures that the endpoint returns a successful response with the appropriate message and that the data is not empty.
     */
    @Test
    @Order(1)
    void validTestObjectSuccessful() throws Exception {
        TripInfo info = new TripInfo();
        info.setStartingPlace("Shang Hai");
        info.setEndPlace("Tai Yuan");
        info.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));

        String jsonRequest = objectMapper.writeValueAsString(info);

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success Query"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint works correctly, for a trip with the given tripId that exists in the database, while
     * communicating with all the needed services.
     * It ensures that the endpoint returns a successful response with the correct list of trip responses.
     */
    @Test
    @Order(2)
    void validTestObjectCorrectly() throws Exception {
        TripInfo info = new TripInfo();
        info.setStartingPlace("Shang Hai");
        info.setEndPlace("Tai Yuan");
        info.setDepartureTime(new Date("Mon May 04 09:51:52 GMT+0800 2013"));

        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing", "beijing", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));

        Route route = new Route();
        route.setId("0b23bd3e-876a-4af3-b920-c50a90c90b04");
        List<String> stationIds = Arrays.asList("shanghai", "nanjing", "shijiazhuang", "taiyuan");
        List<Integer> distances = Arrays.asList(0, 350, 1000, 1300);
        route.setStations(stationIds);
        route.setDistances(distances);

        TravelResult travelResult = new TravelResult();
        travelResult.setPercent(1.0);
        travelResult.setStatus(true);
        HashMap<String, String> prices = calculatePrices(route, trip, 0.35, 1.0);
        travelResult.setPrices(prices);

        TripResponse tripResponse = new TripResponse();
        tripResponse.setConfortClass(0);
        tripResponse.setEconomyClass(0);
        tripResponse.setStartingStation(info.getStartingPlace());
        tripResponse.setTerminalStation(info.getEndPlace());
        tripResponse.setTripId(new TripId("Z1234"));
        tripResponse.setTrainTypeId("ZhiDa");
        tripResponse.setPriceForConfortClass(travelResult.getPrices().get("confortClass"));
        tripResponse.setPriceForEconomyClass(travelResult.getPrices().get("economyClass"));

        String jsonRequest = objectMapper.writeValueAsString(info);
        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<List<TripResponse>> response = JSONObject.parseObject(result, new TypeReference<Response<List<TripResponse>>>(){});
        Assertions.assertTrue(response.getData().contains(tripResponse));
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case when there are no trip in the database.
     * It ensures that the endpoint returns a response with the appropriate message and the empty list of trip responses.
     */
    @Test
    @Order(3)
    void validTestMissingTrips() throws Exception {
        List<TripResponse> list = new ArrayList<>();

        TripInfo info = new TripInfo();
        info.setStartingPlace("Shang Hai");
        info.setEndPlace("Tai Yuan");
        List<Trip> tripList = tripRepository.findAll();


        tripRepository.deleteAll();
        // No Trip saved in tripRepository

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<List<TripResponse>> response = JSONObject.parseObject(result, new TypeReference<Response<List<TripResponse>>>(){});
        Assertions.assertEquals(new Response<>(1, "Success Query", list), response);

        tripRepository.saveAll(tripList);
    }

    /*
     * The test is designed to verify that the endpoint works correctly when the afterToday(departureTime) returns false
     * It ensures that the endpoint returns a successful response with the appropriate message and the list of trip responses.
     */
    @Test
    @Order(4)
    void validTestTripResponseNull() throws Exception {
        TripInfo info = new TripInfo(); // Body variable of request
        info.setStartingPlace("Shang Hai");
        info.setEndPlace("Tai Yuan");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 1);
        info.setDepartureTime(calendar.getTime());
        // afterToday(departureTime) is false, so null is returned

        String jsonRequest = objectMapper.writeValueAsString(info);
        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "No Content", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case, for when one container in the chain stops, so the communication chain is being interrupted.
     * The test expects a response null.
     */
    @Test
    @Order(5)
    void testOneContainerStops() throws Exception {
        TripInfo info = new TripInfo(); // Body variable of request
        info.setStartingPlace("Shang Hai");
        info.setEndPlace("Tai Yuan");
        info.setDepartureTime(new Date(1367638312000L));

        basicServiceContainer.stop();

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    private HashMap<String, String> calculatePrices(Route route, Trip trip, double basicPriceRate, double firstClassPriceRate) {
        HashMap<String, String> prices = new HashMap<>();
        int indexStart = route.getStations().indexOf(trip.getStartingStationId());
        int indexEnd = route.getStations().indexOf(trip.getTerminalStationId());

        try {
            int distance = route.getDistances().get(indexEnd) - route.getDistances().get(indexStart);
            double priceForEconomyClass = distance * basicPriceRate;
            double priceForConfortClass = distance * firstClassPriceRate;
            prices.put("economyClass", "" + priceForEconomyClass);
            prices.put("confortClass", "" + priceForConfortClass);
        } catch (Exception e) {
            prices.put("economyClass", "95.0");
            prices.put("confortClass", "120.0");
        }

        return prices;
    }
}
