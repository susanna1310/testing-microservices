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
import travel.entity.*;

import java.util.*;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration test for the POST Trip Detail Endpoint in TravelService.
 *
 * Following service are connected to this Endpoint:
 * - TrainService
 * - SeatService
 * - RouteService
 * - TicketInfoService
 * - OrderService
 * - StationService
 * - BasicService
 * - Travel2Service
 * - ConfigService
 *
 * Test containers are used to create real service instances for testing.
 * MongoDB is used to create a real database instance for testing.
 *
 * Endpoint: "/api/v1/travelservice/trip_detail"
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostTripDetailTest {
    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer travel2MongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    private static final GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
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
    private static final GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configMongoDBContainer);
    @Container
    private static final GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");
    @Container
    private static final GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");
    @Container
    private static final GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");
    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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
    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");
    @Container
    private static final GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderOtherServiceMongoDBContainer);
    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");
    @Container
    private static final GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);
    private final String url = "/api/v1/travelservice/trip_detail";
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
        registry.add("ts.order.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderOtherServiceContainer.getMappedPort(12031));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.travel.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travel2ServiceContainer.getMappedPort(12346));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));

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
     * This test is designed to verify that the endpoint correctly handles the case when all the information is valid.
     */
    @Test
    void validTestTripAllInfo() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("G1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Tai Yuan");

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
     * This test is designed to verify that the endpoint correctly handles the case when post request is valid
     * It ensures that the endpoint returns a response with the appropriate message and data.
     */
    @Test
    void validTestTripAllDetailInfoCorrectly() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("G1234");
        info.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        info.setFrom("Shang Hai");
        info.setTo("Tai Yuan");

        Trip trip = new Trip(
                new TripId("G1234"),
                "GaoTieOne",
                "shanghai",
                "nanjing",
                "beijing",
                new Date("Mon May 04 09:51:52 GMT+0800 2013"),
                new Date("Mon May 04 15:51:52 GMT+0800 2013")
        );
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


    /*
     * This test is designed to verify that the endpoint correctly handles the case when the tripId does not exist in the repository.
     */
    @Test
    void validTestMissingTrip() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("non-existing-trip-id");

        TripAllDetail expectedTripAllDetail = new TripAllDetail();
        expectedTripAllDetail.setTripResponse(null);
        expectedTripAllDetail.setTrip(null);

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TripAllDetail> response = objectMapper.readValue(result, new TypeReference<Response<TripAllDetail>>() {
        });

        Assertions.assertEquals(new Response<>(1, "Success", expectedTripAllDetail), response);
    }

    /*
     * This test is designed to verify that the endpoint correctly handles the case when the reponse is empty.
     */
    @Test
    void validTestTripEmptyNull() throws Exception {
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("G1234");
        info.setFrom("Shang Hai");
        info.setTo("Tai Yuan");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 1);
        info.setTravelDate(calendar.getTime());

        TripAllDetail expectedTripAllDetail = new TripAllDetail();
        expectedTripAllDetail.setTrip(null);
        expectedTripAllDetail.setTripResponse(null);

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TripAllDetail> response = objectMapper.readValue(result, new TypeReference<Response<TripAllDetail>>() {
        });

        Assertions.assertEquals(new Response<>(1, "Success", expectedTripAllDetail), response);
    }
}
