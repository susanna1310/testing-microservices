package travel.component;

import com.fasterxml.jackson.databind.type.CollectionType;
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
import travel.entity.AdminTrip;
import travel.entity.Route;
import travel.entity.TrainType;
import travel.entity.Trip;

import java.net.URI;
import java.util.ArrayList;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all trips and return them as admin trips. For that is communicates with
 * the ts-train-service, to get the train type and the ts-route-service to get the routes.
 * This endpoint is used by the admin.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetTravelServiceAdminTripTest extends BaseComponentTest {

    private final String url = "/api/v1/travelservice/admin_trip";
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        tripRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint works correctly for trips that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the admin trips.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);


        Route route = new Route();
        route.setId(trip.getRouteId());
        Response<Route> responseRoute = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setId(trip.getTrainTypeId());
        Response<TrainType> responseTrainType = new Response<>(1, "success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-train-service:14567/api/v1/trainservice/trains/" + trip.getTrainTypeId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainType), MediaType.APPLICATION_JSON));


        ArrayList<AdminTrip> adminTrips = new ArrayList<>();
        AdminTrip adminTrip = new AdminTrip();
        adminTrip.setTrip(trip);
        adminTrip.setRoute(route);
        adminTrip.setTrainType(trainType);
        adminTrips.add(adminTrip);
        String result = mockMvc.perform(get(url)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, AdminTrip.class);
        Response<ArrayList<AdminTrip>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success", adminTrips), response);
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case
     * when there are no trips in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get(url)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, AdminTrip.class);
        Response<ArrayList<AdminTrip>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }
}