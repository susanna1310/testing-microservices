package travel.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import travel.entity.Trip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to delete the trip based on a given trip ID via DELETE request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the DELETE request.
 * - URL parameter-specific test cases.
 */
public class DeleteTravelServiceTripsTripsIdTest extends BaseComponentTest {
    private final String url = "/api/v1/travelservice//trips/{tripId}";
	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * The test is designed to verify that the endpoint for deleting the trip by trip ID works correctly, for a valid ID with a trip that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the trip ID.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);

        String tripId = "D12355";
        String result = mockMvc.perform(delete(url, tripId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
        Assertions.assertEquals(new Response<>(1, "Delete trip:" + tripId + ".", tripId), response);
    }

    /*
     * The test is designed to verify that the endpoint for deleting a trip by trip ID correctly handles the case
     * when there is no trip associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String tripId = "D12355";
        String result = mockMvc.perform(delete(url, tripId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
        Assertions.assertEquals(new Response<>(0, "Trip " + tripId + " doesn't exist.", null), response);
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
}