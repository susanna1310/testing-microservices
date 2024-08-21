package travel2.integration;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

import java.util.*;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/travel2service/trip_detail endpoint.
 * This endpoint sends requests to ts-ticketinfo-service and ts-order-other-service to retrieve a TripAllDetail object with attributes TripResponse object and Trip object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostTripDetailTravel2ServiceTest {
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
    private final String url = "/api/v1/travel2service/trip_detail";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

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
     * The test is designed to verify that the endpoint  works correctly, for a trip that exists in the database, while
     * communicating with all the needed services.
     * It ensures that the endpoint returns a successful response with the appropriate message and that the data is not empty.
     */
    @Test
    @Order(1)
    void validTestTripAllDetailInfoSuccess() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo(); // Body variable sent in request
        info.setTripId("Z1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Bei Jing");

        String jsonRequest = objectMapper.writeValueAsString(info);
        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint  works correctly, for a trip that exists in the database, while
     * communicating with all the needed services.
     * It ensures that the endpoint returns a successful response with the appropriate and all trip detail object.
     */
    @Test
    @Order(2)
    void validTestTripAllDetailInfoCorrectly() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("Z1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Bei Jing");

        Trip trip = new Trip(new TripId("Z1234"), "ZhiDa", "shanghai", "nanjing", "beijing", new Date("Mon May 04 09:51:52 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("0b23bd3e-876a-4af3-b920-c50a90c90b04");

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
        tripResponse.setStartingStation(info.getFrom());
        tripResponse.setTerminalStation(info.getTo());
        tripResponse.setTripId(new TripId("G1234"));
        tripResponse.setTrainTypeId("ZhiDa");
        tripResponse.setPriceForConfortClass(travelResult.getPrices().get("confortClass"));
        tripResponse.setPriceForEconomyClass(travelResult.getPrices().get("economyClass"));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(tripResponse);
        tripAllDetail.setTrip(trip);

        String jsonString = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<TripAllDetail> response = objectMapper.readValue(result, new TypeReference<Response<TripAllDetail>>() {
        });

        Assertions.assertEquals(tripAllDetail.getTrip().getRouteId(), response.getData().getTrip().getRouteId());
        Assertions.assertEquals(tripAllDetail.getTrip().getStartingStationId(), response.getData().getTrip().getStartingStationId());
        Assertions.assertEquals(tripAllDetail.getTrip().getStationsId(), response.getData().getTrip().getStationsId());
        Assertions.assertEquals(tripAllDetail.getTrip().getTerminalStationId(), response.getData().getTrip().getTerminalStationId());

        Assertions.assertEquals(tripAllDetail.getTripResponse().getStartingStation(), response.getData().getTripResponse().getStartingStation());
        Assertions.assertEquals(tripAllDetail.getTripResponse().getTerminalStation(), response.getData().getTripResponse().getTerminalStation());
        Assertions.assertEquals(tripAllDetail.getTripResponse().getTripId(), response.getData().getTripResponse().getTripId());
        Assertions.assertEquals(tripAllDetail.getTripResponse().getTrainTypeId(), response.getData().getTripResponse().getTrainTypeId());
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case when the trip with the given tripId is not in the database.
     * It ensures that the endpoint returns a response with the appropriate message and the trip all detail.
     */
    @Test
    @Order(3)
    void validTestMissingTrip() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo(); // Body variable sent in request
        info.setTripId("K1234");
        // No Trip exists with that tripId in repository

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(null);
        tripAllDetail.setTrip(null);

        String jsonRequest = objectMapper.writeValueAsString(info);
        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<TripAllDetail> response = objectMapper.readValue(result, new TypeReference<Response<TripAllDetail>>() {
        });
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case when afterToday(departureTime) returns false.
     * It ensures that the endpoint returns a response with the appropriate message and the trip all detail.
     */
    @Test
    @Order(4)
    void validTestTripResponseNull() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo(); // Body variable sent in request
        info.setTripId("Z1234");
        info.setFrom("Shang Hai");
        info.setTo("Bei Jing");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 1);
        info.setTravelDate(calendar.getTime());

        // afterToday(departureTime) is false, so null is returned
        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTrip(null);
        tripAllDetail.setTripResponse(null);

        String jsonRequest = objectMapper.writeValueAsString(info);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TripAllDetail> response = objectMapper.readValue(result, new TypeReference<Response<TripAllDetail>>() {
        });
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
    }

    /*
     * Test case, for when one container in the chain stops, so the communication chain is being interrupted.
     * The test expects a response null.
     */
    @Test
    @Order(5)
    void testOneContainerStopped() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo(); // Body variable sent in request
        info.setTripId("Z1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Bei Jing");

        seatServiceContainer.stop();

        String jsonRequest = objectMapper.writeValueAsString(info);
        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * Test case, for when one container in the chain stops, so the communication chain is being interrupted.
     * The test expects a response null.
     */
    @Test
    @Order(6)
    void testOneContainerStopped2() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo(); // Body variable sent in request
        info.setTripId("Z1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Tai Yuan");

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
            prices.put("economyClass", String.valueOf(priceForEconomyClass));
            prices.put("confortClass", String.valueOf(priceForConfortClass));
        } catch (Exception e) {
            prices.put("economyClass", "95.0");
            prices.put("confortClass", "120.0");
        }

        return prices;
    }
}
