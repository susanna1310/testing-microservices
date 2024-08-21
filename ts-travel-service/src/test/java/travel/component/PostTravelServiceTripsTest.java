package travel.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import travel.entity.TravelInfo;
import travel.entity.Trip;
import travel.entity.TripId;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to create a new trip via POST request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostTravelServiceTripsTest extends BaseComponentTest {

    private final String url = "/api/v1/travelservice/trips";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/


    /*
     * The test is designed to verify that the endpoint for creating a new trip works correctly, for a valid trip.
     * It ensures that the endpoint returns a successful response with the appropriate message and the no content.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        TravelInfo info = createSampleTravelInfo();
        TripId id = new TripId(info.getTripId());
        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<ArrayList<ArrayList<Trip>>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class,
                typeFactory.constructParametricType(ArrayList.class, typeFactory.constructParametricType(ArrayList.class, Trip.class))));
        Assertions.assertEquals(new Response<>(1, "Create trip:" + id + ".", null), response);
    }

    /*
     * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple trip information objects provided in the request payload.
     * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        TravelInfo[] info = {createSampleTravelInfo(), createSampleTravelInfo()};
        String jsonRequest = objectMapper.writeValueAsString(info);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isBadRequest());
    }

    /*
     * The test is designed to verify that the endpoint for creating a new trip correctly handles the case
     * when there already exists a trip with the same ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        TravelInfo info = createSampleTravelInfo();
        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<ArrayList<ArrayList<Trip>>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class,
                typeFactory.constructParametricType(ArrayList.class, typeFactory.constructParametricType(ArrayList.class, Trip.class))));
        Assertions.assertEquals(new Response<>(1, "Trip " + info.getTripId().toString() + " already exists", null), response);
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
}