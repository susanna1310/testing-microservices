package adminorder.component;

import adminorder.entity.Order;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.springframework.http.RequestEntity.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/adminorderservice/adminorder endpoint.
 * This endpoint send a GET request to both ts-order-service and ts-order-other-service to retrieve all existing orders.
 * The responses from both services are mocked in all test cases.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAdminOrderTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Tests the retrieval of all orders from both ts-order-service and ts-order-other-service.
     * Verifies that the server returns a list containing all retrieved orders.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(UUID.randomUUID());
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order.setContactsName("Name");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("FirstClass");
        order.setFrom("berlin");
        order.setTo("muenchen");
        order.setStatus(0);
        order.setPrice("100");

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setAccountId(UUID.randomUUID());
        order2.setBoughtDate(new Date());
        order2.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order2.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order2.setContactsName("Name");
        order2.setDocumentType(1);
        order2.setContactsDocumentNumber("contactDocumentNumber");
        order2.setTrainNumber("B1234");
        order2.setCoachNumber(5);
        order2.setSeatClass(2);
        order2.setSeatNumber("FirstClass");
        order2.setFrom("muenchen");
        order2.setTo("berlin");
        order2.setStatus(0);
        order2.setPrice("100");

        ArrayList<Order> ordersFromService = new ArrayList<>();
        ordersFromService.add(order);

        ArrayList<Order> orderFromOtherService = new ArrayList<>();
        orderFromOtherService.add(order2);

        Response<ArrayList<Order>> responseFromService = new Response<>(1, "Success.", ordersFromService);
        Response<ArrayList<Order>> responseFromOtherService = new Response<>(1, "Success", orderFromOtherService);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFromService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFromOtherService)));

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<ArrayList<Order>> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<Order>>>(){});

        Assertions.assertEquals(1, actualResponse.getStatus());
        Assertions.assertEquals("Get the orders successfully!", actualResponse.getMsg());
        Assertions.assertEquals(2, actualResponse.getData().size());
        Assertions.assertTrue(actualResponse.getData().contains(order));
        Assertions.assertTrue(actualResponse.getData().contains(order2));
    }

    /*
     * Tests the case where no orders are retrieved from either service.
     * Verifies that the server returns an empty list with a success status.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Response<Object> responseFromService = new Response<>(0, "No Content.", null);
        Response<Object> responseFromOtherService = new Response<>(0, "No Content.", null);

        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFromService)));

        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFromOtherService)));

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<ArrayList<Order>> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<Order>>>(){});

        Assertions.assertEquals(1, actualResponse.getStatus());
        Assertions.assertEquals("Get the orders successfully!", actualResponse.getMsg());
        Assertions.assertTrue(actualResponse.getData().isEmpty());
    }
}
