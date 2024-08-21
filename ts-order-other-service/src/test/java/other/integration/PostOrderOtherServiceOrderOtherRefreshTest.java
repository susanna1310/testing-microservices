package other.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
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
import other.entity.Order;
import other.entity.QueryInfo;
import other.repository.OrderOtherRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to check if the orders, where the account ID matches the login ID from the order information, fit the requirements
 * and update the "from" and "to" station name of each order to the station id.
 * To update get the station id, it communicates with the ts-station-service.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostOrderOtherServiceOrderOtherRefreshTest {

    private final String url = "/api/v1/orderOtherService/orderOther/refresh";

    private final static Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderOtherRepository orderOtherRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service");

    @Container
    public static final MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static GenericContainer<?> stationContainer = new GenericContainer<>(DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.station.service.url", stationContainer::getHost);
        registry.add("ts.station.service.port", () -> stationContainer.getMappedPort(12345));

        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017).toString());
    }

    @BeforeEach
    public void setUp() {
        orderOtherRepository.deleteAll();
    }

    private Order createSampleOrder() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067702"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013"));
        order.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order.setContactsName("contactName");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("1");
        order.setFrom("nanjing");
        order.setTo("shanghaihongqiao");
        order.setStatus(0);
        order.setPrice("100.0");
        return order;
    }


    /*
     * The equivalence based test is designed to verify that the endpoint for checking if the orders fit the requirements works correctly and updates the station IDs, for a valid order information
     * where the login ID matches an account ID of orders in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and correct orders.
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    void validTestCorrectObject() throws Exception {
        QueryInfo info = new QueryInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderOtherRepository.save(order);
        ArrayList<Order> orderList = new ArrayList<>();
        orderList.add(order);
        order.setFrom("Nan Jing");
        order.setTo("Shang Hai Hong Qiao");

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Success", orderList), JSONObject.parseObject(result,  new TypeReference<Response<ArrayList<Order>>>(){}));
    }

    /**
     * This equivalence based test verifies the behavior of the implementation when no orders match the query criteria. That way an empty list is in the body
     * for the POST request to station. An empty list is expected as return data.
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    void validTestNoOrdersFound() throws Exception {
        QueryInfo info = new QueryInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success", new ArrayList()), JSONObject.parseObject(result,  new TypeReference<Response<ArrayList<Order>>>(){}));
    }

    /**
     * This equivalence based test verifies the behavior of the implementation when the order includes an invalid value for the station ids.
     * With the station ids being invalid the ts-station service returns Response(status=0, msg=No stationNamelist according to stationIdList, data=[]).
     * But since the implementation does not check the return value, there is an IndexOutOfBoundsException.
     * This test fails because of the exception.
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    void validTestNoStationNameFound() throws Exception {
        QueryInfo info = new QueryInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        //not existing station ids
        order.setFrom("Invalid");
        order.setTo("Invalid");
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderOtherRepository.save(order);

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        //Just example response, how case could be handled in the implementation
        Assertions.assertEquals(new Response<>(0, "No station names found for the provided station IDs", new ArrayList<>()), JSONObject.parseObject(result,  new TypeReference<Response<ArrayList<Order>>>(){}));
    }


    /*
     * This defect-based test ensures that the application handles scenarios where the
     * ts-station-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    void testServiceUnavailable() throws Exception {
        QueryInfo info = new QueryInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderOtherRepository.save(order);

        // Stop the station service container to simulate service unavailability
        stationContainer.stop();

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();
        Response<?> response = JSONObject.parseObject(result, new TypeReference<Response<?>>() {});
        //Just example response, how case could be handled in the implementation
        Assertions.assertEquals(0, response.getStatus());
        Assertions.assertEquals("Station service unavailable. Please try again later.", response.getMsg());
    }



}


