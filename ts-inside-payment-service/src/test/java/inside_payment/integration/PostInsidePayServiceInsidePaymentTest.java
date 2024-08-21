package inside_payment.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import inside_payment.entity.*;
import inside_payment.repository.AddMoneyRepository;
import inside_payment.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Order;
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

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to create and process a user's payment for an order by checking the order's status, verifying if the user has enough balance, and if not, attempting an external payment.
 * It communicates with the ts-payment-service , the ts-order-service and the ts-order-other-service.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostInsidePayServiceInsidePaymentTest {

    private final String url = "/api/v1/inside_pay_service/inside_payment";

    private final static Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddMoneyRepository addMoneyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service");


    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static GenericContainer<?> orderContainer = new GenericContainer<>(DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    public static GenericContainer<?> orderOtherContainer = new GenericContainer<>(DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer paymentServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-payment-mongo");

    @Container
    public static GenericContainer<?> paymentContainer = new GenericContainer<>(DockerImageName.parse("local/ts-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(19001)
            .withNetwork(network)
            .withNetworkAliases("ts-payment-service")
            .dependsOn(paymentServiceMongoDBContainer);


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderContainer::getHost);
        registry.add("ts.order.service.port", () -> orderContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherContainer.getMappedPort(12032));
        registry.add("ts.payment.service.url", paymentContainer::getHost);
        registry.add("ts.payment.service.port", () -> paymentContainer.getMappedPort(19001));

        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017).toString());
    }

    @BeforeEach
    public void setUp() {
        addMoneyRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has not enough balance.
     * It uses the ts-order-service because the trip id starts with "G".
     * It ensures that the endpoint returns a successful response with the appropriate message and no content.
     */
    @Test
    @Order(1)
    void validTestOrderService() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(PaymentType.O, paymentRepository.findByOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703").get(0).getType());
        Assertions.assertEquals(new Response<>(1, "Payment Success Pay Success", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has not enough balance.
     * It uses the ts-order-other-service because the trip id starts with "K".
     * It ensures that the endpoint returns a successful response with the appropriate message and no content.
     */
    @Test
    @Order(2)
    void validTestOrderOtherService() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("4d2a46c7-70cb-4cf1-c5bb-b68406d9da6e");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("K1235");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(PaymentType.O, paymentRepository.findByOrderId(paymentInfo.getOrderId()).get(0).getType());
        Assertions.assertEquals(new Response<>(1, "Payment Success Pay Success", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the order does not exist.
     * It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    @Order(3)
    void validTestOrderNotFound() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a64b-49c0-a8c0-32776b067700");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(0, "Payment Failed, Order Not Exists", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the price is negative
     * It ensures that the endpoint returns a response with the appropriate message and no content.
     * The test fails because the implementation allows negative values as price.
     */
    @Test
    @Order(4)
    void invalidTestNegativePrice() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("4d2a46c7-70cb-4ce1-c5bb-b68406d9da6e");
        paymentInfo.setPrice("-50.");
        paymentInfo.setTripId("K1235");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(0, "Payment Failed", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * This  defect-based test ensures that the application handles scenarios where the
     * ts-payment-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(5)
    void testServiceUnavailable() throws Exception {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");

        // Stop the payment service container to simulate service unavailability
        paymentContainer.stop();

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();
        Response<String> response = JSONObject.parseObject(result, new TypeReference<Response<String>>() {});

        //Just example response, how case could be handled in the implementation.
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Payment service unavailable. Please try again later.", response.getMsg());
    }
}
