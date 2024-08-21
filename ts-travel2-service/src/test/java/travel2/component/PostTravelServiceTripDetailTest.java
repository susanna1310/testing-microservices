package travel2.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import travel2.entity.*;

import java.net.URI;
import java.util.*;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve detailed trip information based on provided trip details via POST request.
 * For that it communicates with the ts-route-service, ts-ticketinfo-service, ts-order-service, ts-train-service and ts-seat-service.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostTravelServiceTripDetailTest extends BaseComponentTest {
    private final String url = "/api/v1/travel2service/trip_detail";
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        tripRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The test is designed to verify that the endpoint  works correctly, for a trip that exists in the database, while
     * communicating with all the needed services.
     * It ensures that the endpoint returns a successful response with the appropriate message and the all trip detail.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("D12355");
        info.setFrom("Tokyo");
        info.setTo("Osaka");
        info.setTravelDate(new Date("Mon May 04 09:00:00 GMT+0800 2025"));

        queryForStationId(info.getFrom(), "id123");
        queryForStationId(info.getTo(), "id321");

        Route route = new Route();
        route.setId(trip.getRouteId());
        List<String> stationIds = new ArrayList<>();
        stationIds.add("id123");
        stationIds.add("id321");
        List<Integer> distances = new ArrayList<>();
        distances.add(3);
        distances.add(7);
        distances.add(11);
        route.setStations(stationIds);
        route.setDistances(distances);
        Response<Route> responseRoute = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setId(trip.getTrainTypeId());
        trainType.setAverageSpeed(40);
        TravelResult travelResult = new TravelResult();
        travelResult.setPercent(1.0);
        travelResult.setStatus(true);
        HashMap<String, String> prices = new HashMap<>();
        prices.put("economyClass", "" + 10.0);
        prices.put("confortClass", "" + 15.0);
        travelResult.setPrices(prices);
        Response<TravelResult> responseTravelResult = new Response<>(1, "Success", travelResult);
        TravelResult resultForTravel = JsonUtils.conveterObject(responseTravelResult.getData(), TravelResult.class);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseTravelResult), MediaType.APPLICATION_JSON));

        SoldTicket sold = new SoldTicket();
        sold.setTrainNumber("D12355");
        Response<SoldTicket> responseOrder = new Response<>(1, "Success", sold);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + info.getTravelDate() + "/" + trip.getTripId()
                .toString()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        TripResponse tripResponse = new TripResponse();
        tripResponse.setConfortClass(50);
        tripResponse.setEconomyClass(50);
        queryForStationId(info.getFrom(), "id123");
        queryForStationId(info.getTo(), "id321");
        tripResponse.setConfortClass(getRestTicketNumber());
        queryForStationId(info.getFrom(), "id123");
        queryForStationId(info.getTo(), "id321");
        tripResponse.setEconomyClass(getRestTicketNumber());
        tripResponse.setStartingStation(info.getFrom());
        tripResponse.setTerminalStation(info.getTo());

        int indexStart = route.getStations().indexOf("id123");
        int indexEnd = route.getStations().indexOf("id321");
        int distanceStart = route.getDistances().get(indexStart) - route.getDistances().get(0);
        int distanceEnd = route.getDistances().get(indexEnd) - route.getDistances().get(0);

        Response<TrainType> responseTrainType = new Response<>(1, "success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-train-service:14567/api/v1/trainservice/trains/" + trip.getTrainTypeId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainType), MediaType.APPLICATION_JSON));


        int minutesStart = 60 * distanceStart / trainType.getAverageSpeed();
        int minutesEnd = 60 * distanceEnd / trainType.getAverageSpeed();

        Calendar calendarStart = Calendar.getInstance();
        calendarStart.setTime(trip.getStartingTime());
        calendarStart.add(Calendar.MINUTE, minutesStart);
        tripResponse.setStartingTime(calendarStart.getTime());
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTime(trip.getStartingTime());
        calendarEnd.add(Calendar.MINUTE, minutesEnd);
        tripResponse.setEndTime(calendarEnd.getTime());
        tripResponse.setTripId(new TripId("D12355"));
        tripResponse.setTrainTypeId(trip.getTrainTypeId());
        tripResponse.setPriceForConfortClass(resultForTravel.getPrices().get("confortClass"));
        tripResponse.setPriceForEconomyClass(resultForTravel.getPrices().get("economyClass"));
        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(tripResponse);
        tripAllDetail.setTrip(trip);
        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TripAllDetail> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TripAllDetail.class));
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case when there are no trip in the database.
     * It ensures that the endpoint returns a response with the appropriate message and the trip all detail.
     */
    @Test
    void validTestMissingObject() throws Exception {
        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(null);
        tripAllDetail.setTrip(null);
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("D12355");
        info.setFrom("Tokyo");
        info.setTo("Osaka");

        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TripAllDetail> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TripAllDetail.class));
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
        Assertions.assertEquals(new Response<>(1, "Success", tripAllDetail), response);
    }

    @Test
    void invalidTestMultipleObjects() throws Exception {
        TripAllDetailInfo[] info = {new TripAllDetailInfo(), new TripAllDetailInfo()};
        String jsonRequest = objectMapper.writeValueAsString(info);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isBadRequest());
    }


    /*
     * The test verifies the behavior of the endpoint when a POST request is made with a malformed or null object in the request payload.
     * It expects the endpoint to return a 400 Bad Request status code, indicating that the request body does not conform to the expected format or is missing essential data.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(null);
        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isBadRequest());
    }

    /*
     * The test verifies the behavior of the endpoint when a POST request is made without any object in the request payload.
     * It expects the endpoint to return a 400 Bad Request status code, indicating that the request body is missing, and thus cannot be processed as expected.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isBadRequest());
    }

    private Integer getRestTicketNumber() throws Exception {
        Response<Integer> responseSeats = new Response<>(1, "Get Left Ticket of Internal Success", 5);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseSeats), MediaType.APPLICATION_JSON));
        return responseSeats.getData();
    }


    private void queryForStationId(String stationName, String data) throws Exception {
        Response<String> response = new Response<>(1, "Success", data);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/" + stationName).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));
    }
}