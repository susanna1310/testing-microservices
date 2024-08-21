package security.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import security.entity.OrderSecurity;
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * This endpoint is checking the security for the accountId in other words if the order information of the last hour or
 * the total valid orders of the account exceed the critical configuration information.
 * It takes an id as a URL parameter. As such we test equivalence classes for the input. It interacts with the database,
 * which is why we need to setup a MongoDBContainer for the repository, and with other services via RestTemplate, so we
 * use MockRestServiceServer to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetSecurityConfigByIdTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private MockRestServiceServer mockServer;

    @Autowired
    private SecurityRepository securityRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    @BeforeAll
    static void beforeAll() throws Exception {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    void beforeEach() {
        securityRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

	/*
	#######################################
	# Method (GET) specific test cases #
	#######################################
	*/

    /*
     * The first equivalence class test is for a potentially valid id which exists in the repository, which results in
     * getting the OrderSecurity objects from the external services for the account. We also have to save two securityConfig
     * objects in the repository for the critical configuration information. Depending on if the sum of values of the OrderSecurity
     * objects is higher than the one from the securityConfig objects, the response is successful or not.
     */
    @Test
    void validTestCorrectId() throws Exception {
        //Mock the responses for external services
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumInLastOneHour(10);
        responseData.setOrderNumOfValidOrder(15);
        Response<OrderSecurity> mockResponse = new Response<>(1, "Success", responseData);

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-service:12031/api/v1/orderservice/order/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        mockServer.verify();
        assertEquals(new Response<>(1, "Success.r", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For this input class we test the case when we give the endpoint request more than one id as the URL parameter. This
     * does not cause an error, because it only takes the first value. This equivalence class test is still important,
     * because it highlights the difference to endpoints which take body object json, which causes an error if it is too
     * many objects.
     */
    @Test
    void invalidTestMultipleIds() throws Exception {
        //Mock the responses for external services
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumInLastOneHour(10);
        responseData.setOrderNumOfValidOrder(15);
        Response<OrderSecurity> mockResponse = new Response<>(1, "Success", responseData);

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-service:12031/api/v1/orderservice/order/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id.toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        mockServer.verify();
        assertEquals(new Response<>(1, "Success.r", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we test the case, when the URL parameter is malformed in any way, in other words it has wrong characters, is a
     * wrong attribute type etc. as an equivalence class, which should not be able to be converted into the right type.
     * We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", UUID.randomUUID() + "/" + UUID.randomUUID())
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to GET. No input causes an exception.
     */
    @Test
    void invalidTestMissingId() {
        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}")
        );});
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * The id is normally of type UUID but is a String as a URL parameter. But as the conversion happens in another service,
     * a component test, where we mock the responses of other services, does not cause an error. That means that every valid
     * String works as an id. Now we test the equivalence class of id, where it does not exist, which means
     * the other services' response will have an OrderSecurity object with default values (0)
     */
    @Test
    void invalidTestNonExistingId() throws Exception {
        //Mock the responses for external services
        OrderSecurity orderSecurity = new OrderSecurity();
        Response<OrderSecurity> mockResponse = new Response<>(1, "Success", orderSecurity);

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-service:12031/api/v1/orderservice/order/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        mockServer.verify();
        assertEquals(new Response<>(1, "Success.r", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we will test the combination of a valid Id and too high order values for the OrderSecurity objects, which
     * exceed the critical configuration information of the securityConfig objects in the repository. As a result the
     * response will be different.
     */
    @Test
    void invalidTestTooHighValues() throws Exception {
        //Mock the responses for external services
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumOfValidOrder(20);
        responseData.setOrderNumInLastOneHour(20);
        Response<OrderSecurity> mockResponse = new Response<>(1, "Success", responseData);

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-service:12031/api/v1/orderservice/order/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        mockServer.verify();
        assertEquals(new Response<>(0, "Too much order in last one hour or too much valid order", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Now we test the equivalence class of id, where it does not follow the UUID format, which means
     * the other services' response will be null, which causes an exception and a null response.
     */
    @Test
    void invalidTestInvalidId() throws Exception {
        //Mock the responses for external services
        Response<OrderSecurity> mockResponse = new Response<>(0, "Error", null);

        mockServer.expect(ExpectedCount.once(), requestTo(startsWith("http://ts-order-service:12031/api/v1/orderservice/order/security")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "invalid")
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

}
