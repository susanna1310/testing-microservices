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
 * This endpoint is designed to create and process a payment, checking if the user has enough balance, and if not, handles the payment through the ts-payment-service.
 * It calculates the total expense and compares it to the total money the use has. If the money is not enough, it makes the payment over the ts-payment-service.
 * Since the implementation of the service function treats the BigDecimal incorrect, the total expense is always 0, so that the ts-payment is never called.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostInsidePayServiceInsidePaymentDifferenceTest {
    private final String url = "/api/v1/inside_pay_service/inside_payment/difference";

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
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has not enough money.
     * It ensures that the endpoint returns a successful response with the appropriate message and no content.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Pay Difference Success", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has not enough money,
     * but the payment with that order id already exists in the ts-payment-service
     * It ensures that the endpoint returns a response with the appropriate message and no content.
     * The test fails because of incorrect service implementation, so that ts-payment-service is never called
     */
    @Test
    @Order(2)
    void validTestDuplicatePayment() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a68b-49c0-a8c0-32776b067701");
        paymentInfo.setPrice("50.");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(0, "Pay Difference Failed", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }

    /*
     * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the price is negative
     * It ensures that the endpoint returns a successful response with the appropriate message and no content.
     * The test fails because of incorrect service implementation, so that ts-payment-service is never called. With the correct implementation
     * the test would still fail because the implementation allows negative values as price.
     */
    @Test
    @Order(3)
    void invalidTestNegativePrice() throws Exception{
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a68b-49c0-a8c0-32776b067701");
        paymentInfo.setPrice("-50.");

        String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(PaymentType.E, paymentRepository.findByOrderId(paymentInfo.getOrderId()).get(0).getType());
        Assertions.assertEquals(new Response<>(0, "Pay Difference Failed", null), JSONObject.parseObject(result, new TypeReference<Response<String>>() {}));
    }


    /*
     * This  defect-based test ensures that the application handles scenarios where the
     * ts-payment-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because of incorrect service implementation, so that ts-payment-service is never called. With the correct implementation
     * the test would still fail because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(4)
    void testServiceUnavailable() throws Exception {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");

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
