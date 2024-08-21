package travel2.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
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
import travel2.entity.Route;
import travel2.entity.Trip;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve the route that matches to the trip with the given trip ID via GET.
 * To get the route it communicates with the ts-route-service.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetTravelServiceRoutesTripIdTest extends BaseComponentTest {

    private final String url = "/api/v1/travel2service/routes/{tripId}";
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
     * The test is designed to verify that the endpoint for retrieving the route works correctly, for a valid ID with a trip that exists in the database and a matching route.
     * It ensures that the endpoint returns a successful response with the appropriate message and the route.
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

        String trainNumber = "D12355";
        String result = mockMvc.perform(get(url, trainNumber)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Route> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Route.class));
        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "\"[Get Route By Trip ID] Trip Not Found:\" + tripId", route), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case
     * when there is no trip associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String trainNumber = "D12355";
        String result = mockMvc.perform(get(url, trainNumber)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Route> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Route.class));
        Assertions.assertEquals(new Response<>(0, "\"[Get Route By Trip ID] Trip Not Found:\" + tripId", null), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the route correctly handles the case
     * when there is no route that matches the trip. It ensures that the endpoint returns a response with the appropriate message and no content.
     *
     * The test fails because the getRouteByTripId() function in TravelServiceImpl checks for route != null but the return value from the ts-route-service sets route attributes to null
     * Actual return: Route{id='null', stations=null, distances=null, startStationId='null', terminalStationId='null'}
     */
    @Test
    void validTestGetNoRoutes() throws Exception {
        Trip trip = createSampleTrip();

        tripRepository.save(trip);
        Response<Route> responseRoute = new Response<>(0, "No content with the routeId", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        String trainNumber = "D12355";
        String result = mockMvc.perform(get(url, trainNumber)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Route> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Route.class));
        mockServer.verify();
        Assertions.assertEquals(new Response<>(0, "\"[Get Route By Trip ID] Trip Not Found:\" + tripId", null), response);
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * The test is designed to verify that the endpoint correctly handles the case when no trip ID parameter is provided in the request.
     * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
     */
    @Test
    void invalidTestNonExistingId() {
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get(url));
        });
    }

    /*
     * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided. With the character
     * "/" the url changes and is therefore not found.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        mockMvc.perform(get(url, "3/4/5")
                )
                .andExpect(status().isNotFound());
    }

}