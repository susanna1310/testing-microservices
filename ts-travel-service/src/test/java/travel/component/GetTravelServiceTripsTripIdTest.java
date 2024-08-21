package travel.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import travel.entity.Trip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve the trip based on a given trip ID via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetTravelServiceTripsTripIdTest extends BaseComponentTest {
    private final String url = "/api/v1/travelservice/trips/{tripId}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint for retrieving the trip by trip ID works correctly, for a valid ID with a trip that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the trip.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);

        String trainNumber = "D12355";
        String result = mockMvc.perform(get(url, trainNumber)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Trip> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Trip.class));
        Assertions.assertEquals(new Response<>(1, "Search Trip Success by Trip Id " + trainNumber, trip), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the trip by trip ID correctly handles the case
     * when there is no trip order associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String trainNumber = "D12355";
        String result = mockMvc.perform(get(url, trainNumber)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<Trip> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Trip.class));
        Assertions.assertEquals(new Response<>(0, "No Content according to tripId" + trainNumber, null), response);
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