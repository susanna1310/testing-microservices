package travel2.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import travel2.entity.TravelInfo;
import travel2.entity.Trip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to update a trip via PUT request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the PUT request.
 */
public class PutTravelServiceTripsTest extends BaseComponentTest {

    private final String url = "/api/v1/travel2service/trips";
	/*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint for updating a trip works correctly, for a valid ID with a trip that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the trip.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        TravelInfo info = createSampleTravelInfo();
        info.setStationsId("new");
        trip.setStationsId("new");
        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Trip> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Trip.class));
        Assertions.assertEquals(new Response<>(1, "Update trip info:" + trip.getTripId().toString(), trip), response);
    }

    /*
     * The test verifies the behavior of the endpoint when attempting to perform a PUT request with multiple trip objects provided in the request payload.
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
     * The test verifies the behavior of the endpoint when a PUT request is made with a malformed or null object in the request payload.
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
     * The test is designed to verify that the endpoint for updating a trip works correctly, for a trip that does not exist in the database.
     * It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        TravelInfo info = createSampleTravelInfo();
        String jsonRequest = objectMapper.writeValueAsString(info);

        String result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Trip> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Trip.class));
        Assertions.assertEquals(new Response<>(1, "Trip" + info.getTripId().toString() + "doesn 't exists", null), response);
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