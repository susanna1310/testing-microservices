package rebook.component;

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
import rebook.entity.*;
import rebook.entity.Order;


import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a RebookInfo object to rebook/change an order. To do that, it communicates with several services
 * to get all information via the given ids as well as change the information. As such we need to test the equivalence
 * classes for the attributes of the RebookInfo object. Because the service communicates with other services via RestTemplate,
 * we use MockRestServiceServer to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostRebookTest {

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
     * This test is for the equivalence class of valid attributes for the body object. As the ids are all seen as Strings
     * in this service and only converted to UUID in the external services, almost every String is valid. The seatType has
     * to be either 2 or 3. The date is only used to set an attribute, which is why there is only one equivalence class for it.
     * We mock the responses of the other services as if it would communicate with them like normal (excluding the String ids).
     */
    @Test
    void validTestCorrectObject() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse5 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response mockResponse6 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals(0, order.getDocumentType());
        assertEquals("0", order.getSeatNumber());
        assertEquals("0.0", order.getDifferenceMoney());
        assertEquals(2, order.getSeatClass());
        assertEquals("180", order.getPrice());
        assertEquals("1", order.getFrom());
        assertEquals(id, order.getId());
        assertEquals("G", order.getTrainNumber());
        assertEquals("1", order.getTo());
        assertEquals(3, order.getStatus());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null},{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}]";

        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
        String requestJson = "{\"loginId\":not, \"orderId\":a, \"oldTripId\":String, \"tripId\":wrong, \"seatType\":2.324, \"date\":date}";

        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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

        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
     * The equivalence class of a non-existing loginId would cause problems in the external service, which would return
     * a different response. Here we simulate that combination of non-existing loginId in the other service by mocking
     * the different response, which causes a different response for this service as well. A non-existing id would be
     * in the same class as null, wrong characters/format for UUID etc.
     */
    @Test
    void bodyVarLoginIdNonExisting() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(0, "Not existing", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Can't draw back the difference money, please try again!", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class also exists for the orderId, which causes a response with a different
     * message.
     */
    @Test
    void bodyVarOrderIdNonExisting() throws Exception {
        Response<Order> mockResponse1 = new Response<>(0, "Not exists", new Order());
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "order not found", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class also exists for the orderId, which causes a response with a different
     * message.
     */
    @Test
    void bodyVarTripIdNonExisting() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(0, "Not exists", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Not exists", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test is for the equivalence class, when both tripIds are from the same type, which means they both begin with
     * "G" or "D" or both do not. In that case the order is not deleted and newly created like in the first test, but
     * merely updated. The response would be the same. But if this request to the orderService has a return code 0, then
     * we get a new response
     */
    @Test
    void bodyVarOldTripIdTripIdSameType() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse5 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response mockResponse6 = new Response<>(0, "Error", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"Z\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Can't update Order!", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test a valid value for the seatType in combination with too few seats, which should normally return a
     * response with status code, but because of a wrong implementation for the economy class (comparison of number of
     * seats with 3 instead of seatType as well as only checking number of seats in comfort class) does not. That is
     * why the test fails.
     */
    @Test
    void bodyVarSeatTypeValidTestValueTooFewSeats() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setEconomyClass(0);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));


        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":3, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Seat Not Enough", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the equivalence class where the seat type is neither 2 or 3. What exactly happens depends on the
     * response of the seat service, but if it just returns an initialized ticket object, the response will be a success.
     * But here we test the case, when it returns null as a response to the non-existing seat type. This stops the request to
     * our endpoint and only returns null values, probably as response to a NullPointerException
     */
    @Test
    void bodyVarSeatTypeInvalidTestValue() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Response<Ticket> mockResponse5 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The last equivalence class we want to test is the case if the new ticket price is higher than the old one. Normally
     * we can achieve this hypothetical, if the new trainType is the more expensive high speed train with the tripId beginning with
     * "G" or "D". As sake for a correct test sequence, we did not do this in the first test, but now we get a new response
     * for this case.
     */
    @Test
    void validTestCantPayDifference() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(1);
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("160");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"loginId\":\"1\", \"orderId\":\"1\", \"oldTripId\":\"1\", \"tripId\":\"G\", \"seatType\":2, \"date\":null}";

        String result = mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        order = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Order.class);
        assertEquals("20", order.getDifferenceMoney());
        assertEquals(new Response<>(2, "Please pay the different money!", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }
}
