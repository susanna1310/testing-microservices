package seat.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import seat.entity.LeftTicketInfo;
import seat.entity.Seat;
import seat.entity.SeatClass;
import seat.entity.Ticket;

import java.util.*;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/seatservice/left_tickets endpoint.
 * This endpoint POSTS a Seat object to find out the remaining free seats for that interval of the Seat object. To do that, it communicates with several services:
 * Sends a GET request to ts-travel-service or ts-travel2-service (depending on the first letter of the trainNumber) to retrieve the route with the given trainNumber
 * Sends a POST request to ts-order-service or ts-order-other-service (depending on the first letter of the trainNumber) to retrieve the LetTicketInfo object
 * Send a GET request to ts-travel-service or ts-travel2-service (depending on the first letter of the trainNumber) to retrieve the trainType with the given trainNumber
 * The endpoint retrieves the number of tickets left.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostLeftTicketsTest
{
    @Autowired
    private MockMvc mockMvc;

    private static final Network network = Network.newNetwork();
    private final ObjectMapper mapper = new ObjectMapper();
    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.travel.service.url", travelServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelServiceContainer.getMappedPort(12346));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
    }

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid seat object with the trainNumber starting with G, so the requests are sent to ts-travel-service and ts-order-service.
     * This test expects a successful response with the appropriate travel result:
     * Response<>(1, "Get Left Ticket of Internal Success", 1073741823)
     */
    @Test
    @Order(1)
    void validTestCorrectObjectTrainNumberG() throws Exception {
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("nanjing");
        seat.setDestStation("shanghai");
        seat.setTrainNumber("G1236");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 1073741823), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case for a valid seat object with the trainNumber not starting with G or D, so the requests are sent to ts-travel2-service and ts-order-other-service.
     * This test expects a successful response with the appropriate travel result:
     * Response<>(1, "Get Left Ticket of Internal Success", 1073741823)
     */
    @Test
    @Order(2)
    void validTestCorrectObjectTrainNumberNotGOrD() throws Exception {
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("shanghai");
        seat.setDestStation("beijing");
        seat.setTrainNumber("K1345");

        //Actual request to the endpoint we want to test

        String jsonString = mapper.writeValueAsString(seat);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 1073741823), JSONObject.parseObject(result, Response.class));
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case with a valid seat object where the startStation and destStation do not exist, but because those attributes are not relevant in sending a request and the attributes are just saved into the ticket object as they exist in the seat object,
     * the test remains valid (with the trainNumber not starting with G or D, so the requests are sent to ts-travel2-service and ts-order-other-service) and in the if statement:
     *                 if (stationList.indexOf(soldTicketDestStation) < stationList.indexOf(startStation))
     * because both destStation and startStation do not exist, the result is true.
     * This test expects a successful response with the appropriate travel result:
     * Response<>(1, "Get Left Ticket of Internal Success", 1073741823)
     */
    @Test
    @Order(3)
    void testStartStationDestStationNonExisting() throws Exception {
        Seat seatRequest = new Seat();
        seatRequest.setTravelDate(new Date());
        seatRequest.setTrainNumber("K1345");
        seatRequest.setSeatType(SeatClass.BUSINESS.getCode());
        seatRequest.setStartStation("notExisting");
        seatRequest.setDestStation("notExisting");

        //Actual request to the endpoint we want to test

        String jsonString = mapper.writeValueAsString(seatRequest);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 1073741823), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Test case with an invalid seat object where the trainNumber does not exist. The test fails, because the trainNumber attribute is used in every request sent to the other services.
     * When one request in the request chain fails, the final response returned is:
     * Response<>(null, null, null)
     */
    @Test
    @Order(4)
    void testTrainNumberNonExisting() throws Exception {
        Seat seatRequest = new Seat();
        seatRequest.setTravelDate(new Date());
        seatRequest.setTrainNumber("K0000");
        seatRequest.setSeatType(SeatClass.BUSINESS.getCode());
        seatRequest.setStartStation("shanghai");
        seatRequest.setDestStation("beijing");

        //Actual request to the endpoint we want to test

        String jsonString = mapper.writeValueAsString(seatRequest);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case with a valid seat object where the seatType does not exist (out of range), but because it is only compared with 2 and every other value is automatically assumed to be 3, the request will be executed
     * as normal.
     * This test expects a successful response with the appropriate travel result:
     * Response<>(1, "Get Left Ticket of Internal Success", 1073741823)
     */
    @Test
    @Order(5)
    void seatTypeValidTestValueOutOfRange() throws Exception {
        Seat seat = new Seat();
        seat.setTrainNumber("K1345");
        seat.setSeatType(9); // Does not exist
        seat.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        seat.setStartStation("shanghai");
        seat.setDestStation("beijing");

        //Actual request to the endpoint we want to test

        String jsonString = mapper.writeValueAsString(seat);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 1073741823), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(6)
    void testTravelServiceStopped() throws Exception {
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("nanjing");
        seat.setDestStation("shanghai");
        seat.setTrainNumber("G1236");

        travelServiceContainer.stop();

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(7)
    void testTravel2ContainerStopped() throws Exception {
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("shanghai");
        seat.setDestStation("beijing");
        seat.setTrainNumber("K1345");

        travel2ServiceContainer.stop();

        String jsonString = mapper.writeValueAsString(seat);
        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

}