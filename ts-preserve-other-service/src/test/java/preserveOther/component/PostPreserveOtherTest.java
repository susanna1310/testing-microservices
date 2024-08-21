package preserveOther.component;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import preserveOther.entity.*;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS an OrderTicketsInfo object to preserve the ticket order information. To do that, it communicates
 * with several services to get and post information for the order. As such we need to test the equivalence classes for the attributes of the
 * OrderTicketsInfo object. Because the service communicates with other services via RestTemplate, we use MockRestServiceServer
 * to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostPreserveOtherTest {

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
     * This test is for the equivalence class of valid attributes for the body object. The ids are all seen as Strings,
     * but converted to UUID later, which means only Strings in the right UUID format are valid. The seatType has
     * to be either 2 or 3. The assurance and foodType have to be not 0 so that a request is made to their external services.
     * The rest of the attributes are used in requests to other services, but because we mock their responses, we see them
     * as valid anyway.
     */
    @Test
    void validTestCorrectObject() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        preserveOther.entity.Order order = new preserveOther.entity.Order();
        order.setId(id);
        order.setAccountId(id);
        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response<Assurance> mockAssurance = new Response<>(1, "Success", new Assurance());
        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/1/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAssurance), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}," +
                "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}]";

        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
        String requestJson = "{\"accountId\":invalid, \"contactsId\":invalid, \"tripId\":invalid, \"seatType\":null, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to post.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * The equivalence class of an invalid accountId with wrong characters/format would cause problems in the external service, which would return
     * a different response. Here we simulate that combination of invalid accountId in the other service by mocking
     * the different response, which causes a different response for this service as well. An invalid id would be
     * in the same equivalence class as non-existing, null etc.
     */
    @Test
    void bodyVarAccountidInvalidFormatTest() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(0, "Error", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/wrongformat").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"wrongformat\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Error", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class also exists for the contactsId, which causes a response with a different
     * message.
     */
    @Test
    void bodyVarContactsidInvalidFormatTest() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response<Contacts> mockResponse2 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/wrongFormat").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"wrongFormat\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Error", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Similar to the test above, this equivalence class also exists for the tripId, which causes a response with a different
     * message. But if a null object is returned here, an exception will occur, the request will be interrupted and
     * a null response returned, because the data is accessed before the status code is checked in the implementation. But to
     * get the response with status code 0, we mocked the response of the external service, so that it returns an initialized
     * object.
     */
    @Test
    void bodyVarTripidWrongFormatTest() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        Response<TripAllDetail> mockResponse3 = new Response<>(0, "Error", new TripAllDetail());
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"wrongFormat\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Error", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The seat type has to be either 2 for comfort class or 3 for economy class. With this test, we test the equivalence
     * class of values outside this range. But because the if statement only checks for 2, every value will default to
     * economy class.
     */
    @Test
    void bodyVarSeattypeInvalidTestValue() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        preserveOther.entity.Order order = new preserveOther.entity.Order();
        order.setId(id);
        order.setAccountId(id);
        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response<Assurance> mockAssurance = new Response<>(1, "Success", new Assurance());
        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/1/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAssurance), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":" + Integer.MIN_VALUE + ", \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Success.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test a valid value for the seatType in combination with too few seats, which should normally return a
     * response with status code 0, but because of a wrong implementation for the economy class (comparison of number of
     * seats with 3 instead of seatType as well as only checking number of seats in comfort class) does not. That is why
     * this test fails.
     */
    @Test
    void bodyVarSeattypeValidValueTooFewSeats() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setEconomyClass(0);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":3, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Seat Not Enough", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The date is for when the travel was ordered. It is only used in a request to another service to create this order.
     * As such the equivalence class of invalid values or dates long in the past will probably cause an error in creating the order.
     */
    @Test
    void bodyVarDateInValidTestDate() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2000-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(0, "Error", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the equivalence class of invalid station names for from and to. We do this by simulating a reasonable
     * response from the station service with status code 0. But as the implementation of this endpoint does not check the
     * status code of this response, this will cause the following requests to fail as well until it causes an exception,
     * which interrupts the request and returns a null response
     */
    @Test
    void bodyVarFromtoInvalidName() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Response<TravelResult> mockResponse5 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response<Ticket> mockResponse6 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2024-01-01\", \"from\":\"1\", \"to\":\"1\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The assurance can have different types/values. 0 stands for no assurance, while the other values could be an
     * assurance type. We already tested the equivalence class for a valid assurance type in the first test. Now we
     * input a non-existing/invalid insurance value, which means the request to the assurance service will fail. We
     * will simulate this by mocking the response and as a result get a new response from this endpoint.
     */
    @Test
    void bodyVarAssuranceInvalidTestValue() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        preserveOther.entity.Order order = new preserveOther.entity.Order();
        order.setId(id);
        order.setAccountId(id);
        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response<Assurance> mockAssurance = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/"+ Integer.MIN_VALUE + "/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAssurance), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":"+ Integer.MIN_VALUE + ", \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Success.But Buy Assurance Fail.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The food type can also have different values. 0 means that no food will be ordered and the request to the foodservice
     * will be skipped, while other values are different food types, if they exists. 2 also includes the stationName and
     * a storeName in the request. Now we will test the equivalence class of an invalid foodType, foodName or foodPrice, which means the
     * request to the assurance service will fail. We will simulate this by mocking the response and as a result get a
     * new response from this endpoint. This equivalence class also includes the combination of the valid type 2, but invalid/
     * non-existing stationName or storeName.
     */
    @Test
    void bodyVarFoodtypeInvalidTestValue() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        preserveOther.entity.Order order = new preserveOther.entity.Order();
        order.setId(id);
        order.setAccountId(id);
        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response mockFood = new Response(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockFood), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":0, \"foodType\":" + Integer.MIN_VALUE + ", \"stationName\":\"1\", \"storeName\":\"1\", \"foodName\":\"1\", \"foodPrice\":-3.99, \"handleDate\":\"date\", \"consigneeName\":\"\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Success.But Buy Food Fail.", "Success"), JSONObject.parseObject(result, Response.class));

    }

    /*
     * The consignee attributes can also be invalid with an invalid consignee name, weight, weight, handleDate or phone number. These
     * equivalence classes lead to the request to the consignservice failing, which we simulate by mocking the response similiar
     * to the two tests above.
     */
    @Test
    void bodyVarInvalidConsigneeInfo() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        preserveOther.entity.Order order = new preserveOther.entity.Order();
        order.setId(id);
        order.setAccountId(id);
        Response<preserveOther.entity.Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response mockConsign = new Response(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockConsign), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2025-01-01\", \"from\":\"stationA\", \"to\":\"stationA\", \"assurance\":0, \"foodType\":0, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"noDate\", \"consigneeName\":\"()42397)\", \"consigneePhone\":\"noNumber\", \"consigneeWeight\":-147.14, \"isWithin\":true}";

        String result = mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(1, "Consign Fail.", "Success"), JSONObject.parseObject(result, Response.class));

    }
}

