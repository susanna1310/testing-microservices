package preserveOther.integration;

import com.alibaba.fastjson.JSONObject;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;



/*
 * This endpoint POSTS an OrderTicketsInfo object to preserve the ticket order information. To do that, it communicates
 * with several services to get and post information for the order. As such we need to test the equivalence classes for the attributes of the
 * OrderTicketsInfo object and defects of the endpoint. Because the service communicates with other services via RestTemplate,
 * we do not mock these responses this time for Integration Testing. It communicates with many other services, which hinders
 * the performance and the tests fail on systems, which do not have enough resources. We had many problems testing this
 * endpoint for that reason, because it did not work on most of our systems and if it did, we still had a lot of problems
 * with Docker etc. We only mock notificationService,
 * because this service sends mails to the user, which we can't do in these tests. We do this via WireMock, which only
 * mocks the data of the request, but the request itself is still processed. We agreed upon doing it like this in the meeting
 * with the other groups for setting up the integration tests.
 * We had to modify create in trainService as well, else we would have had no objects in the trainRepository and the tests
 * would have failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostPreserveOtherTest {

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer assuranceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-assurance-mongo");


    @Container
    public static MongoDBContainer consignServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-consign-mongo");

    @Container
    public static MongoDBContainer foodServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-food-mongo");

    @Container
    public static MongoDBContainer contactsServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-contacts-mongo");

    @Container
    public static MongoDBContainer securityServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-security-mongo");


    @Container
    public static MongoDBContainer userServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");


    @Container
    private static GenericContainer<?> consignServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-consign-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16111)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-service")
            .dependsOn(consignServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> foodServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-food-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18856)
            .withNetwork(network)
            .withNetworkAliases("ts-food-service")
            .dependsOn(foodServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> contactsServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-contacts-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12347)
            .withNetwork(network)
            .withNetworkAliases("ts-contacts-service")
            .dependsOn(contactsServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> securityServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-security-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11188)
            .withNetwork(network)
            .withNetworkAliases("ts-security-service")
            .dependsOn(securityServiceMongoDBContainer);

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
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    private static GenericContainer<?> assuranceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-assurance-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18888)
            .withNetwork(network)
            .withNetworkAliases("ts-assurance-service")
            .dependsOn(assuranceServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> userServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-user-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12342)
            .withNetwork(network)
            .withNetworkAliases("ts-user-service")
            .dependsOn(userServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
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
    public static MongoDBContainer consignPriceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-mongo");

    @Container
    private static GenericContainer<?> consignPriceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-consign-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16110)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-service")
            .dependsOn(consignPriceServiceMongoDBContainer);

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
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
            .dependsOn(configServiceMongoDBContainer);

    @RegisterExtension
    static WireMockExtension notificationServiceWireMock = WireMockExtension.newInstance().options(wireMockConfig().port(17853)).build();

    @BeforeAll
    public static void setUp() {
        configureFor("localhost", 17853);
        securityServiceContainer.start();
        orderOtherServiceContainer.start();
        orderServiceContainer.start();
        contactsServiceContainer.start();
        travel2ServiceContainer.start();
        ticketinfoServiceContainer.start();
        basicServiceContainer.start();
        stationServiceContainer.start();
        priceServiceContainer.start();
        routeServiceContainer.start();
        consignServiceContainer.start();
        consignPriceServiceContainer.start();
        foodServiceContainer.start();
        seatServiceContainer.start();
        userServiceContainer.start();
        configServiceContainer.start();
        assuranceServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.assurance.service.url", assuranceServiceContainer::getHost);
        registry.add("ts.assurance.service.port", () -> assuranceServiceContainer.getMappedPort(18888));
        registry.add("ts.consign.service.url", consignServiceContainer::getHost);
        registry.add("ts.consign.service.port", () -> consignServiceContainer.getMappedPort(16111));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.food.service.url", foodServiceContainer::getHost);
        registry.add("ts.food.service.port", () -> foodServiceContainer.getMappedPort(18856));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.contacts.service.url", contactsServiceContainer::getHost);
        registry.add("ts.contacts.service.port", () -> contactsServiceContainer.getMappedPort(12347));
        registry.add("ts.security.service.url", securityServiceContainer::getHost);
        registry.add("ts.security.service.port", () -> securityServiceContainer.getMappedPort(11188));
        registry.add("ts.user.service.url", userServiceContainer::getHost);
        registry.add("ts.user.service.port", () -> userServiceContainer.getMappedPort(12342));
        registry.add("ts.notification.service.url", () -> "localhost");
        registry.add("ts.notification.service.port", () -> "17853");
        registry.add("ts.ticketinfo.service.url", ticketinfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketinfoServiceContainer.getMappedPort(15681));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
        registry.add("ts.consign.price.service.url", consignPriceServiceContainer::getHost);
        registry.add("ts.consing.price.service.port", () -> consignPriceServiceContainer.getMappedPort(16110));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
    }



	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test is for the equivalence class of valid attributes for the body object. The ids are all seen as Strings,
     * but converted to UUID later, which means only Strings in the right UUID format are valid. The seatType has
     * to be either 2 or 3. The assurance and foodType have to be not 0 so that a request is made to their external services.
     * But as the assuranceService returns a 403 status code error for the request, we set it to 0, so that the test
     * runs through.
     * The rest of the attributes are used in requests to other services, which can cause different responses.
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    void validTestCorrectObject() throws Exception{
        mockNotificationService();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyMockNotificationService();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));

    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * The defect/equivalence class of an invalid accountId with wrong characters/format will cause problems in the external service,
     * which causes an exception in the external service, where it is converted to UUID. And as this case is not handled
     * it also causes an exception in this endpoint, which returns a null response
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    void bodyVarAccountIdInvalidFormatTest() throws Exception{
        String requestJson = "{\"accountId\":\"wrong\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The defect/equivalence class of a non-existing accountId does not cause a different response, because it is never checked
     * if the accountId does exist. As such the request to this endpoint is successful.
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    void bodyVarAccountIdNonExisting() throws Exception{
        mockNotificationService();
        String requestJson = "{\"accountId\":\"4d2a46c7-0000-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyMockNotificationService();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Similar to the test with wrong format above, this equivalence class also exists for the contactsId, which causes
     * an exception with null response as well
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    void bodyVarContactsIdInvalidFormatTest() throws Exception{
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"wrong\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The defect/equivalence class of a non-existing contactsId causes a response with status code 0 in the request to
     * the contactsService. This status code of the response is checked, which means that this endpoint also returns
     * a relevant response.
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    void bodyVarContactsIdNonExisting() throws Exception{
        String requestJson = "{\"accountId\":\"4d2a46c7-0000-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-0000-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No contacts according to contacts id", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Similar to the test above, this equivalence class also exists for the tripId, which causes a response with a different
     * message. As such the travel2Service will return a success response and object with null attributes. As they are
     * not checked for null and used later, this leads to an exception and results in a null response from the endpoint.
     * The travel2Service never returns a response with status code 0, so an unsuccessful request is never correctly
     * handled. This is in the same equivalence class as non-existing tripIds, null etc. as it is always seen as a String.
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    void bodyVarTripIdWrongFormatTest() throws Exception{
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"wrong\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The seat type has to be either 2 for comfort class or 3 for economy class. With this test, we test the equivalence
     * class of values outside this range. But because the if statement only checks for 2, every value will default to
     * economy class.
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    void bodyVarSeatTypeInvalidTestValue() throws Exception {
        mockNotificationService();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d00000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":" + Integer.MIN_VALUE + ", \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyMockNotificationService();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test a valid value for the seatType in combination with too few seats, which should normally return a
     * response with status code 0, but because of a wrong implementation for the economy class (comparison of number of
     * seats with 3 instead of seatType as well as only checking number of seats in comfort class) does not. That is why
     * this test fails.
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    void bodyVarSeatTypeValidValueTooFewSeats() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90001\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K8134\", \"seatType\":3, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Seat Not Enough", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The date is for when the travel was ordered. It is only used in a request to the travel2Service. If this date
     * is not after today, the request "fails" and the returned object of the travel2Service will have null attributes. But
     * as the status code is not 0, the values of the attributes are not checked and later cause an exception and null
     * response
     */
    @Test
    @org.junit.jupiter.api.Order(9)
    void bodyVarDateInValidTestDate() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2000-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the equivalence class of invalid station names for from and to. This causes a response with status code
     * 0 from stationService. But as the implementation of this endpoint does not check the
     * status code of this response, this will cause the following requests to fail as well until it causes an exception,
     * which interrupts the request and returns a null response
     */
    @Test
    @org.junit.jupiter.api.Order(10)
    void bodyVarFromToInvalidName() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"no\", \"to\":\"no\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The assurance can have different types/values. 0 stands for no assurance, while the other values could be an
     * assurance type. We already tested the equivalence class for a valid assurance type in the first test. Now we
     * input a non-existing/invalid insurance value, which means the request to the assurance service will fail. But as
     * the assuranceService always returns a 403 status code for this request, this test fails.
     */
    @Test
    @org.junit.jupiter.api.Order(11)
    void bodyVarAssuranceInvalidTestValue() throws Exception {
        mockNotificationService();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406000000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":100, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyMockNotificationService();
        assertEquals(new Response<>(1, "Success.But Buy Assurance Fail.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where we have a start and end station, that do not have a route between them. As such
     * the routeService will have an unsuccessful response in the travel2Service request chain, which in the end leads
     * to an exception and null response. And as this case is not checked, this also leads to a null response for this
     * endpoint
     */
    @Test
    @org.junit.jupiter.api.Order(12)
    void defectNoRoute() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Jia Xing Nan\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The email to the user is sent as a last request in the endpoint. Here we simulate the response, that sending the
     * email failed, so it returns false. But this case is actually not checked and we get a success response from this
     * endpoint anyway
     */
    @Test
    @org.junit.jupiter.api.Order(13)
    void defectEmailError() throws Exception {
        stubFor(WireMock.post(urlEqualTo("/api/v1/notifyservice/notification/preserve_success"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("false")));

        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68400000000\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyMockNotificationService();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we test the defect case, where creating the order with all the given information fails. That can be achieved
     * when an order with the accountId already exists in the orderRepository. As such we get a new response, because
     * this case is checked in the implementation
     */
    @Test
    @org.junit.jupiter.api.Order(14)
    void defectCreateOrderFail() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Order already exist", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The response of the request to the notificationService is never checked even if it fails. Here we test the defect
     * case, where notificationService is unavailable, which leads to the request to this service not being processed
     * and results in an exception and null response. Any service we do not test this defect for below
     * is used in another request chain before, which means that if the service is unavailable, we do not even reach that point,
     * where this endpoint sends a request to it directly.
     */
    @Test
    @org.junit.jupiter.api.Order(15)
    void defectTestUnavailableNotificationService() throws Exception {
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da64\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * ConsignService never returns a response with status code 0, so we can't reach the case, where the consign fails. Here
     * we test the defect case, where consignService or consignPriceService are unavailable, which leads to the request
     * to these services not being processed and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(16)
    void defectTestUnavailableConsignService() throws Exception {
        consignServiceContainer.stop();
        consignPriceServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da63\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where foodService is unavailable, which leads to the request to this service not being processed
     * and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(17)
    void defectTestUnavailableFoodService() throws Exception {
        foodServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da62\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where assuranceService is unavailable, which leads to the request to this service not being processed
     * and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(18)
    void defectTestUnavailableAssuranceService() throws Exception {
        assuranceServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da61\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":1, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where travel2Service is unavailable, which leads to the request to this service not being processed
     * and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(19)
    void defectTestUnavailableTravel2Service() throws Exception {
        travel2ServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da61\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where contactsService is unavailable, which leads to the request to this service not being processed
     * and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(20)
    void defectTestUnavailableContactsService() throws Exception {
        contactsServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da61\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the defect case, where securityService or order(Other)Service is unavailable, which leads to the
     * request to these services not being processed and results in an exception and null response.
     */
    @Test
    @org.junit.jupiter.api.Order(21)
    void defectTestUnavailableSecurityService() throws Exception {
        contactsServiceContainer.stop();
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        String requestJson = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da61\", \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\", \"tripId\":\"K1432\", \"seatType\":2, \"date\":\"2026-01-01\", \"from\":\"Nan Jing\", \"to\":\"Shang Hai\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }


    private void mockNotificationService() {
        stubFor(WireMock.post(urlEqualTo("/api/v1/notifyservice/notification/preserve_success"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));
    }

    private void verifyMockNotificationService() {
        verify(postRequestedFor(urlEqualTo("/api/v1/notifyservice/notification/preserve_success")));
    }


}


