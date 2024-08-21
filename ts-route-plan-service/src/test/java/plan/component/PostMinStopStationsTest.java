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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a RoutePlanInfo object to retrieve the trips with the fewest stops on that route. To do that, it communicates
 * with several external services to get all routes and then to get all train trips on the route. As such we need to test the equivalence classes for the attributes of the
 * RoutePlanInfo object. Because the service communicates with other services via RestTemplate, we use MockRestServiceServer
 * to mock the responses of the external services.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostMinStopStationsTest {

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
     * For the first equivalence class we test valid values for the attributes. We mock the responses of the external
     * services for every request the service makes. As there are many requests, we tried to stay consistent to the
     * input throughout and in the end we get the trips with the fewest stops between the station name input. The
     * stationNames are of type String, which means every valid String is a possible valid Name. The num is not used
     * in the service layer, which means every valid int is also accepted. The date is used in a request to an external
     * service.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        Response<String> mockResponseId = new Response<>(1, "Success", "1");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));

        mockResponseId = new Response<>(1, "Success", "2");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        ArrayList<Route> routes = new ArrayList<>();
        for(int i = 0; i<6; i++) {
            Route route = new Route();
            route.setStations(Arrays.asList("1", "4", "5", "2"));
            routes.add(route);
        }
        Response<ArrayList<Route>> mockResponseRoute = new Response<>(1, "Success", routes);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1/2").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));


        ArrayList<ArrayList<Trip>> trains = new ArrayList<>();
        Trip trip = new Trip();
        trip.setTripId(new TripId("T"));
        trip.setRouteId("1");
        trains.add(new ArrayList<>());
        trains.get(0).add(trip);
        trains.get(0).add(trip);
        Response<ArrayList<ArrayList<Trip>>> mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));

        mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));


        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        Response<TripAllDetail> mockResponseDetail = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseDetail), MediaType.APPLICATION_JSON));

        Route route = new Route();
        route.setStations(Arrays.asList("1", "4", "5", "2"));
        Response<Route> mockResponse2 = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        JSONArray jsonArray = (JSONArray)(JSONObject.parseObject(result, Response.class).getData());
        for(int i = 0; i<jsonArray.size(); i++) {
            RoutePlanResultUnit element = jsonArray.getObject(i, RoutePlanResultUnit.class);
            assertArrayEquals(new String[]{"1", "4", "5", "2"}, element.getStopStations().toArray());
        }
        assertEquals(new Response<>(1, "Success.", jsonArray), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For this equivalence class we test the combination of (only for this service) valid input object, but no trips
     * for this given route. As a response we will get a failure response, which we simulate. This can be achieved by having no trips at all,
     * no trips for this route on the given date or a non existing/null value for the station names etc. as members of the
     * equivalence class.
     * The station name attributes of the object are Strings with no restriction, which means there is only one equivalence classes
     * with valid values, because they all lead to the same output. We tested that class already above for both
     * attributes. This test will test also the null value for these attributes, which leads to the same result without considering
     * what it could cause with the request to the other services. We will test that in integration testing.
     * The same goes for the date and num attribute of the body object. The num attribute is unused and the date attribute
     * is used in a request to another service.
     */
    @Test
    void validTestNoTrips() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        Response<String> mockResponseId = new Response<>(0, "Not exists", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));

        mockResponseId = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        Response<ArrayList<Route>> mockResponseRoute = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/null/null").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));


        //Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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

        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
     * The equivalence class of invalid dates also should cause a failure response in the request to an external service.
     * Like the test above with invalid stationNames, we simulate the response, which in turn also causes an exception and null response
     */
    @Test
    void bodyVarTravelDateInvalid() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint
        Response<String> mockResponseId = new Response<>(1, "Success", "1");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));

        mockResponseId = new Response<>(1, "Success", "2");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        ArrayList<Route> routes = new ArrayList<>();
        for(int i = 0; i<6; i++) {
            Route route = new Route();
            route.setStations(Arrays.asList("1", "4", "5", "2"));
            routes.add(route);
        }
        Response<ArrayList<Route>> mockResponseRoute = new Response<>(1, "Success", routes);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1/2").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));


        ArrayList<ArrayList<Trip>> trains = new ArrayList<>();
        Trip trip = new Trip();
        trip.setTripId(new TripId("T"));
        trip.setRouteId("1");
        trains.add(new ArrayList<>());
        trains.get(0).add(trip);
        trains.get(0).add(trip);
        Response<ArrayList<ArrayList<Trip>>> mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));

        mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));


        Response<TripAllDetail> mockResponseDetail = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponseDetail), MediaType.APPLICATION_JSON));




        //Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", null, 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }



}
