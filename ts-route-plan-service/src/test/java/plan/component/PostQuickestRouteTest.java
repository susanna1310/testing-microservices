package plan.component;

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
import plan.entity.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.alibaba.fastjson.JSONArray;


import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a RoutePlanInfo object to retrieve the quickest trips on that route. To do that, it communicates
 * with the travelservice and travel2service to get all train trips from the given start station to end station on the date
 * and searches the (max) 5 quickest ones. As such we need to test the equivalence classes for the attributes of the
 * RoutePlanInfo object. Because the service communicates with other services via RestTemplate, we use MockRestServiceServer
 * to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostQuickestRouteTest {

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
     * For the first equivalence class we test valid values for the attributes. We mock the responses of the external
     * services to give us a few different trips for the given route, which means that as a response we should get the
     * 5 quickest ones.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        TripId id = new TripId("T");
        ArrayList<TripResponse> tripResponses = new ArrayList<>();
        for(int i = 0; i<5; i++) {
            TripResponse trip = new TripResponse();
            trip.setTripId(id);
            trip.setStartingTime(new Date());
            trip.setEndTime(new Date());
            tripResponses.add(trip);
        }

        Response<ArrayList<TripResponse>> mockResponse = new Response<>(1, "Success", tripResponses);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));



        ArrayList<TripResponse> tripResponsesOther = new ArrayList<>();
        for(int i = 0; i<5; i++) {
            TripResponse trip = new TripResponse();
            trip.setTripId(id);
            trip.setStartingTime(new Date());
            trip.setEndTime(new Date());
            tripResponsesOther.add(trip);
        }

        mockResponse = new Response<>(1, "Success", tripResponsesOther);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        Route route = new Route();
        route.setStations(new ArrayList<>());
        Response<Route> mockResponse2 = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/routes/" + id).build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        JSONArray jsonArray = (JSONArray)(JSONObject.parseObject(result, Response.class).getData());
        assertEquals(5, jsonArray.size());
        assertEquals(new Response<>(1, "Success", jsonArray), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For this equivalence class we test the combination of (only for this service) valid input object, but no trips
     * for this given route. As a response we will get an empty list. This can be achieved by having no trips at all,
     * no trips for this route on the given date or a non existing/null value for the station names etc. as members of the
     * equivalence class.
     * The station name attributes of the object are Strings with no restriction, which means there is only one equivalence classes
     * with valid values, because they all lead to the same output. We tested that class already above for both
     * attributes. This test will test also the null value for these attributes, which leads to the same result without considering
     * what it could cause with the request to the other services. We will test that in integration testing.
     * The same goes for the date and num attribute of the body object. The num attribute is unused and the date attribute
     * is used as info to get the response from the other services, which are mocked. So we test a past date here, because
     * you would expect to get no trips for this input.
     */
    @Test
    void validTestNoTrips() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        Response<ArrayList<TripResponse>> mockResponse = new Response<>(0, "No content", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = dateFormat.parse("20000301");
        RoutePlanInfo routePlanInfo = new RoutePlanInfo(null, null, date, Integer.MIN_VALUE);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"formStationName\":\"StationA\", \"toStationName\":\"StationB\", \"Date\":\""+ new Date() +"\", \"num\":\"0\"}, {formStationName\":\"StationA\", \"toStationName\":\"StationB\", \"Date\":\""+ new Date() +"\", \"num\":\"0\"}]";

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
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
        String requestJson = "{\"formStationName\":wrong, \"toStationName\":type, \"Date\":\"hehe\", \"num\":null}";

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
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

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }
}
