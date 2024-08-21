package seat.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import seat.entity.*;


import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a Seat object to find out the remaining free seats for that interval of the Seat object. To do that,
 * it communicates with several services to get all information via the given info to find the route, trainType and leftTickets.
 * As such we need to test the equivalence classes for the attributes of the Seat object. Because the service communicates
 * with other services via RestTemplate, we use MockRestServiceServer to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostSeatsLeftTicketsTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test is for the equivalence class of valid attributes for the body object. The date should logically be in the future
     * but is actually not used in the logic implementation, which is why it only has one equivalence class (every Date is valid),
     * the first letter of the trainNumber decides the trainType and the stations are Strings so technically every name is
     * valid. The seat type has to be either 2 for comfort or 3 for economy class.
     */
    @Test
    void validTestCorrectObject() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint

        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i<6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i<5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("2");
            ticket.setSeatNo(new Random().nextInt(10));
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(3);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.0));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"3\", \"destStation\":\"5\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 3), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2},{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2}]";

        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we test the equivalence class case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc,which should not be able to be converted into the right object. We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"travelDate\":\"notADate\", \"trainNumber\":invalid, \"startStation\":invalid, \"destStation\":invalid, \"seatType\":null}";

        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to post.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Only the first character of the trainNumber String is checked and if it is neither "G" or "D" the other Services
     * for normal trains are requested. So as the type is String, almost every trainNumber is valid if it exists. Here we test the case,
     * where it does not start with "G" or "D" opposite to the first test case.
     */
    @Test
    void bodyVarTrainNumberValidTestCorrectLengthAndCharacters() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i<6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/routes/Z").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i<5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("5");
            ticket.setSeatNo(i);
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(50);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/train_types/Z").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.8));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"Z\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 4), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Now we test the case, where the trainNumber does not exist in the other services by mocking their responses. Because
     * the status code is not checked, this will cause an exception, which interrupts the request and returns a null response.
     * This is in the same equivalence class as non-existing/invalid startStations and destStations or null for all these
     * attributes.
     */
    @Test
    void bodyVarTrainNumberStartStationDestStationNonExisting() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        Response<Route> mockResponse1 = new Response<>(0, "Error", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/Gwrong").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response<LeftTicketInfo> mockResponse2 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        Response<TrainType> mockResponse3 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/Gwrong").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"Gwrong\", \"startStation\":\"wrong\", \"destStation\":\"wrong\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The seat type can either be 2 or 3. So here we test the equivalence class, where the value is out of range. But
     * as it is only compared with 2 and every other value is automatically assumed to be 3, the request will be executed
     * as normal.
     */
    @Test
    void bodyVarSeattypeValidTestValueOutOfRange() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i<6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i<5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("5");
            ticket.setSeatNo(i);
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setEconomyClass(10);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.3));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Get Left Ticket of Internal Success", 2), JSONObject.parseObject(result, Response.class));

    }

}
