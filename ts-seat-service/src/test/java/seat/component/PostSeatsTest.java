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
import seat.entity.LeftTicketInfo;
import seat.entity.Route;
import seat.entity.Ticket;
import seat.entity.TrainType;


import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a Seat object to assign the requested seat for an order. To do that, it communicates with several services
 * to get all information via the given info to check if the seat is available and in case change the seat. As such we need to test the equivalence
 * classes for the attributes of the Seat object. Because the service communicates with other services via RestTemplate,
 * we use MockRestServiceServer to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostSeatsTest {

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
            ticket.setDestStation("5");
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



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Ticket ticket = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Ticket.class);
        assertEquals("1", ticket.getStartStation());
        assertEquals("4", ticket.getDestStation());
        assertEquals(new Response<>(1, "Use a new seat number!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2},{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2}]";

        mockMvc.perform(post("/api/v1/seatservice/seats")
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

        mockMvc.perform(post("/api/v1/seatservice/seats")
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

        mockMvc.perform(post("/api/v1/seatservice/seats")
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
    void bodyVarTrainnumberValidTestCorrectLengthAndCharacters() throws Exception {
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
            ticket.setSeatNo(new Random().nextInt(10));
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(3);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/train_types/Z").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"Z\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Ticket ticket = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Ticket.class);
        assertEquals("1", ticket.getStartStation());
        assertEquals("4", ticket.getDestStation());
        assertEquals(new Response<>(1, "Use a new seat number!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

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

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
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
     * as normal. Another team member mentioned, that this test fails for them, because of a NullPointerException, but it
     * works for me.
     */
    @Test
    void bodyVarSeatTypeValidTestValueOutOfRange() throws Exception {
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
            ticket.setSeatNo(new Random().nextInt(10));
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setEconomyClass(2);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"1\", \"destStation\":\"4\", \"seatType\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Ticket ticket = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Ticket.class);
        assertEquals("1", ticket.getStartStation());
        assertEquals("4", ticket.getDestStation());
        assertEquals(new Response<>(1, "Use a new seat number!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the combination of valid values to get a different response. We do this by mocking the responses,
     * so that the ticket can get an assigned seat number from another sold ticket, because its endStation is before our
     * startStation
     */
    @Test
    void bodyVarSeatReused() throws Exception {
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
        for(int i = 0; i<1; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("2");
            ticket.setSeatNo(1);
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



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"3\", \"destStation\":\"4\", \"seatType\":2}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Ticket ticket = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Ticket.class);
        assertEquals("3", ticket.getStartStation());
        assertEquals("4", ticket.getDestStation());
        assertEquals(1, ticket.getSeatNo());
        assertEquals(new Response<>(1, "Use the previous distributed seat number!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the last test we mock the external service responses, so that there is actually no free seat for the seat
     * request. That means the request will be interrupted and return a null response.
     */
    @Test
    void noSeatTest() throws Exception {
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
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(0);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"travelDate\":\"2024-12-01\", \"trainNumber\":\"G\", \"startStation\":\"3\", \"destStation\":\"4\", \"seatType\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(post("/api/v1/seatservice/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

}
