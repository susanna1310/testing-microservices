package security.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * This endpoint is checking the security for the accountId in other words if the order information of the last hour or
 * the total valid orders of the account exceed the critical configuration information.
 * It takes an id as a URL parameter. As such we test equivalence classes for the input and defects for the endpoint.
 * It interacts with the database, which is why we need to setup a MongoDBContainer for the repository, and with other
 * services via RestTemplate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetSecurityConfigsByAccountIdTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityRepository securityRepository;

    private final static Network network = Network.newNetwork();

    @Container
    private static final MongoDBContainer securityServiceMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-security-mongo");

    @Container
    private static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    private static final MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

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

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", securityServiceMongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", securityServiceMongoDBContainer.getMappedPort(27017).toString());
        securityServiceMongoDBContainer.start();
        orderServiceMongoDBContainer.start();
        orderOtherServiceMongoDBContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
    }

	/*
	#######################################
	# Method (GET) specific test cases #
	#######################################
	*/

    /*
     * The first equivalence class test is for a potentially valid id which exists in the repository, which results in
     * getting the OrderSecurity objects from the external services for the account. Depending on if the sum of values of the OrderSecurity
     * objects is higher than the one from the securityConfig objects, the response is successful or not.
     */
    @Test
    @Order(1)
    void validTestCorrectId() throws Exception {
        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success.r", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For this input class we test the case when we give the endpoint request more than one id as the URL parameter. This
     * does not cause an error, because it only takes the first value. This equivalence class test is still important,
     * because it highlights the difference to endpoints which take body object json, which causes an error if it is too
     * many objects.
     */
    @Test
    @Order(2)
    void validTestMultipleIds() throws Exception {
        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success.r", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), JSONObject.parseObject(result, Response.class));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * The id is normally of type UUID but is a String as a URL parameter. But as the conversion happens in another service,
     * only Strings with the UUID format are valid. Now we test the equivalence class of id/defect case, where it does not exist, which means
     * the other services' response will have an OrderSecurity object with default values (0)
     */
    @Test
    @Order(3)
    void invalidTestNonExistingId() throws Exception {
        UUID id = UUID.randomUUID();

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id.toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success.r", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we will test the defect case combination of a valid Id and too high order values for the OrderSecurity objects, which
     * exceed the critical configuration information of the securityConfig objects in the repository. As a result the
     * response will be different.
     */
    @Test
    @Order(4)
    void invalidTestTooHighValues() throws Exception {
        UUID id = UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        securityRepository.deleteAll();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("-1");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("-1");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Too much order in last one hour or too much valid order", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Now we test the defect case/equivalence class of id, where it does not follow the UUID format, which means
     * the other services' response will be null (because of the exception in converting the String to UUID), which
     * also causes an exception in the securityService and a null response.
     */
    @Test
    @Order(5)
    void invalidTestInvalidId() throws Exception {
        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "invalid")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The endpoint compares the amount of orders from the accountId with the critical configuration information. Here we
     * test the defect case, where the securityConfigs named "max_order_1_hour" and "max_order_not_use" with the
     * configuration information do not exist in the repository. Although the interaction with the external services
     * is successful, the request to the endpoint will still fail with a null response because of an exception.
     */
    @Test
    @Order(6)
    void invalidTestNonExistingSecurityConfigs() throws Exception {
        securityRepository.deleteAll();

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we test the defect case, where one or more external services are not available, so the request to these
     * services does not get processed, which results in an exception. Normally this should be covered/caught, but here
     * it also results in an exception in this service, which results in a null response.
     */
    @Test
    @Order(7)
    void defectTestUnavailableService() throws Exception {
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

}

