package rebook.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import rebook.entity.Order;


import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;

/*
 * This endpoint POSTS a RebookInfo object to pay for the difference in price for a rebooked order and update the order information.
 * To do that, it communicates with several services to get all information via the given ids as well as change the information.
 * As such we need to test the equivalence classes for the attributes of the RebookInfo object and defect tests for the
 * endpoint. Because the service communicates with other services via RestTemplate, we do not mock their responses this time for integration test.
 * This endpoint communicates with many other services, which can hinder the performance or many systems don't have enough
 * resources to run all these containers, which causes the tests/requests to fail.
 * We had to modify create in trainService as well, else we would have had no objects in the trainRepository and the tests
 * would have failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostRebookDifferenceTest
{

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");


    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");


    @Container
    public static MongoDBContainer insidePaymentServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-mongo");


    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer paymentServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-payment-mongo");

    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");


    @Container
    private static GenericContainer<?> insidePaymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-inside-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18673)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service")
            .dependsOn(insidePaymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

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
    private static GenericContainer<?> paymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(19001)
            .withNetwork(network)
            .withNetworkAliases("ts-payment-service")
            .dependsOn(paymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    private static GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

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
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> ticketInfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");

    @Container
    private static GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @Container
    private static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);


    @BeforeAll
    public static void setUp() {
        stationServiceContainer.start();
        orderOtherServiceContainer.start();
        orderServiceContainer.start();
        insidePaymentServiceContainer.start();
        stationServiceContainer.start();
        paymentServiceContainer.start();
        travel2ServiceContainer.start();
        travelServiceContainer.start();
        priceServiceContainer.start();
        trainServiceContainer.start();
        routeServiceContainer.start();
        seatServiceContainer.start();
        ticketInfoServiceContainer.start();
        basicServiceContainer.start();
        configServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.inside.payment.service.url", insidePaymentServiceContainer::getHost);
        registry.add("ts.inside.payment.service.port", () -> insidePaymentServiceContainer.getMappedPort(18673));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.payment.service.url", paymentServiceContainer::getHost);
        registry.add("ts.payment.service.port", () -> paymentServiceContainer.getMappedPort(19001));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.travel.service.url", travelServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelServiceContainer.getMappedPort(12346));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.ticketinfo.service.url", ticketInfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketInfoServiceContainer.getMappedPort(15681));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));

    }
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test is for the equivalence class of valid attributes for the body object. As the ids are all seen as Strings
     * in this service and only converted to UUID in the external services, the String has to be in a valid UUID format. The seatType has
     * to be either 2 or 3. The date is used in a request to an external service. We setup objects in the repositories of the
     * other services, so that we get valid responses with valid data. Because the deletion of the
     * order at the end is doing a request to the endpoint with a wrong POST instead of DELETE, this test fails
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    void validTestCorrectObject() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":2, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
        Order order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(1, order.getDocumentType());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(3, order.getSeatClass());
        assertEquals("250.0", order.getPrice());
        assertEquals("nanjing", order.getFrom());
        assertEquals("Contacts_One", order.getContactsName());
        assertEquals("DocumentNumber_One", order.getContactsDocumentNumber());
        assertEquals(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067703"), order.getId());
        assertEquals(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), order.getAccountId());
        assertEquals("G1234", order.getTrainNumber());
        assertEquals("shanghai", order.getTo());
        assertEquals("Thu Jan 01 01:00:00 CET 2026", order.getTravelDate().toString());
        assertEquals(3, order.getStatus());
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * The defect/equivalence class of a non-existing loginId. This does not cause an error in the external services as
     * just a payment object will be created with this new loginId in the paymentService. But the normal standard
     * would have been to have the payment fail as there is no account with such an id in the insidePayment.
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    void bodyVarLoginIdNonExisting() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-0000-0000-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"G1237\", \"tripId\":\"G1234\", \"seatType\":2, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
        Order order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(1, order.getDocumentType());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(2, order.getSeatClass());
        assertEquals("250.0", order.getPrice());
        assertEquals("nanjing", order.getFrom());
        assertEquals("Contacts_One", order.getContactsName());
        assertEquals("DocumentNumber_One", order.getContactsDocumentNumber());
        assertEquals(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067703"), order.getId());
        assertEquals(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), order.getAccountId());
        assertEquals("G1234", order.getTrainNumber());
        assertEquals("shanghai", order.getTo());
        assertEquals("Thu Jan 01 01:00:00 CET 2026", order.getTravelDate().toString());
        assertEquals(3, order.getStatus());
    }

    /*
     * Similar to the test above, this equivalence class also exists for the orderId, which causes a response with a different
     * message this time.
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    void bodyVarOrderIdNonExisting() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-0000-0000-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":2, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Order Not Found", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class also exists for the tripId, which does not cause a different response
     * of the travelService but only returns a success response with status code 1 and null attributes of the data object.
     * That in turn causes a later exception, which returns a null response
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    void bodyVarTripIdNonExisting() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G\", \"seatType\":2, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test is for the equivalence class, when both tripIds are from the same type, which means they both begin with
     * "G" or "D" or both do not. In that case the order is not deleted and newly created like in the first test, but
     * merely updated. The response would be the same.
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    void bodyVarOldTripIdTripIdSameType() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"G9999\", \"tripId\":\"G1234\", \"seatType\":2, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
        Order order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(1, order.getDocumentType());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(2, order.getSeatClass());
        assertEquals("250.0", order.getPrice());
        assertEquals("nanjing", order.getFrom());
        assertEquals("Contacts_One", order.getContactsName());
        assertEquals("DocumentNumber_One", order.getContactsDocumentNumber());
        assertEquals(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067703"), order.getId());
        assertEquals(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), order.getAccountId());
        assertEquals("G1234", order.getTrainNumber());
        assertEquals("shanghai", order.getTo());
        assertEquals("Thu Jan 01 01:00:00 CET 2026", order.getTravelDate().toString());
        assertEquals(3, order.getStatus());
    }

    /*
     * Here we test the equivalence class where the seat type is neither 2 or 3. It normally defaults to economy class, but
     * so for this endpoint it does not matter if the value is neither 2 or 3
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    void bodyVarSeatTypeInvalidTestValue() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067705\", \"oldTripId\":\"G9999\", \"tripId\":\"G1234\", \"seatType\":0, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
        Order order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(1, order.getDocumentType());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(3, order.getSeatClass());
        assertEquals("0", order.getPrice());
        assertEquals("nanjing", order.getFrom());
        assertEquals("Contacts_One", order.getContactsName());
        assertEquals("DocumentNumber_One", order.getContactsDocumentNumber());
        assertEquals(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067705"), order.getId());
        assertEquals(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), order.getAccountId());
        assertEquals("G1234", order.getTrainNumber());
        assertEquals("shanghai", order.getTo());
        assertEquals("Thu Jan 01 01:00:00 CET 2026", order.getTravelDate().toString());
        assertEquals(3, order.getStatus());

    }

    /*
     * The last equivalence class we want to test is the case if the old ticket price is higher than the new one. Normally
     * we can achieve this, if the old trainType is the more expensive high speed train with the tripId beginning with
     * "G" or "D". That means the returned response will be successful.
     */
    @Test
    @org.junit.jupiter.api.Order(9)
    void validTestNegativeDifference() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067705\", \"oldTripId\":\"G9999\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
        Order order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(1, order.getDocumentType());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(3, order.getSeatClass());
        assertEquals("95.0", order.getPrice());
        assertEquals("nanjing", order.getFrom());
        assertEquals("Contacts_One", order.getContactsName());
        assertEquals("DocumentNumber_One", order.getContactsDocumentNumber());
        assertEquals(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067705"), order.getId());
        assertEquals(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), order.getAccountId());
        assertEquals("G1234", order.getTrainNumber());
        assertEquals("shanghai", order.getTo());
        assertEquals("Thu Jan 01 01:00:00 CET 2026", order.getTravelDate().toString());
        assertEquals(3, order.getStatus());
    }

    /*
     * The defect/equivalence class of a  loginId with the wrong format will cause an exception in the external service, which will return
     * a null response. This is not handled, so it causes a null response for this endpoint, too.
     */
    @Test
    @org.junit.jupiter.api.Order(10)
    void bodyVarLoginIdWrongFormat() throws Exception {
        String requestJson = "{\"loginId\":null, \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class/defect also exists for the orderId, which causes a response with a different
     * message.
     */
    @Test
    @org.junit.jupiter.api.Order(11)
    void bodyVarOrderIdWrongFormat() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":null, \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this defect test, we test the case when the payment of the difference fails, which means we get a response
     * with status code 0 from the inside-payment service. This can be achieved, if there is already a payment object
     * with the orderId in the paymentRepository. But because of an implementation error, which causes the paymentService
     * to never be called. Since the implementation of the inside-payment service function treats the BigDecimal incorrect, the total
     * expense is always 0, so that the ts-payment is never called. That is why this test fails
     */
    @Test
    @org.junit.jupiter.api.Order(12)
    void invalidTestPaymentFailed() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ad7750b-a68b-49c0-a8c0-32776b067701\", \"oldTripId\":\"G9999\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Can't pay the difference,please try again", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test is for testing the defect, when an external service is unavailable. Because this case is not handled/covered,
     * it causes a null response. Here we test it for the insidePayment service. Any service we do not test this defect for below
     * is used in another request chain before, which means that if the service is unavailable, we do not reach that point.
     */
    @Test
    @org.junit.jupiter.api.Order(13)
    void defectTestUnavailableInsidePaymentService() throws Exception {
        insidePaymentServiceContainer.stop();
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test is for testing the defect, when an external service is unavailable. Because this case is not handled/covered,
     * it causes a null response. Here we test it for the travelService or travel2Service.
     */
    @Test
    @org.junit.jupiter.api.Order(14)
    void defectTestUnavailableTravelService() throws Exception {
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        travelServiceContainer.stop();
        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test is for testing the defect, when an external service is unavailable. Because this case is not handled/covered,
     * it causes a null response. Here we test it for the stationService.
     */
    @Test
    @org.junit.jupiter.api.Order(15)
    void defectTestUnavailableStationService() throws Exception {
        stationServiceContainer.stop();
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test is for testing the defect, when an external service is unavailable. Because this case is not handled/covered,
     * it causes a null response. Here we test it for the orderService or orderOtherService.
     */
    @Test
    @org.junit.jupiter.api.Order(16)
    void defectTestUnavailableOrderService() throws Exception {
        orderServiceContainer.stop();
        String requestJson = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\", \"oldTripId\":\"Z1237\", \"tripId\":\"G1234\", \"seatType\":3, \"date\":\"2026-01-01\"}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }
}
