package travel.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import travel.entity.Trip;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieves all trips by station IDs via POST request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 * - Body variable specific test cases.
 */
public class PostTravelServiceTripsRoutesTest extends BaseComponentTest {

    private final String url = "/api/v1/travelservice/trips/routes";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The test is designed to verify that the endpoint for retrieving all trips by station IDs works correctly, for a valid IDs that match trips in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the trips.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        ArrayList<ArrayList<Trip>> tripList = new ArrayList<>();
        ArrayList<Trip> trips = new ArrayList<>();
        trips.add(trip);
        tripList.add(trips);

        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add(trip.getRouteId());
        String jsonRequest = objectMapper.writeValueAsString(routeIds);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<ArrayList<ArrayList<Trip>>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class,
                typeFactory.constructParametricType(ArrayList.class, typeFactory.constructParametricType(ArrayList.class, Trip.class))));
        Assertions.assertEquals(new Response<>(1, "Success", tripList), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving all trips by station IDs correctly handles the case
     * when there are no trips in the database. It ensures that the endpoint returns a response with the appropriate message and the empty trip list.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add("routeId");
        String jsonRequest = objectMapper.writeValueAsString(routeIds);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<ArrayList<ArrayList<Trip>>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class,
                typeFactory.constructParametricType(ArrayList.class, typeFactory.constructParametricType(ArrayList.class, Trip.class))));
        ArrayList<ArrayList<Trip>> list = new ArrayList<>();
        list.add(new ArrayList<>());
        Assertions.assertEquals(new Response<>(1, "Success", list), response);
    }

    /*
     * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple trip objects provided in the request payload.
     * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        Trip[] trips = {createSampleTrip(), createSampleTrip()};
        String jsonRequest = objectMapper.writeValueAsString(trips);

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


	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/


    /*
     * The test is designed to verify that the endpoint for retrieving all trips by station IDs correctly handles the case
     * when the station IDs are empty. It ensures that the endpoint returns a response with the appropriate message and the no content.
     */
    @Test
    void bodyVarRoutesIdsValidTestEmptyList() throws Exception {
        ArrayList<String> routeIds = new ArrayList<>();
        String jsonRequest = objectMapper.writeValueAsString(routeIds);
        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<ArrayList<ArrayList<Trip>>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class,
                typeFactory.constructParametricType(ArrayList.class, typeFactory.constructParametricType(ArrayList.class, Trip.class))));
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }
}