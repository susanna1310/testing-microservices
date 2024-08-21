package travel2.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import travel2.entity.Trip;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all trips via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetTravelServiceTripsTest extends BaseComponentTest {
    private final String url = "/api/v1/travel2service/trips";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint for retrieving all trips works correctly, for trips that exists in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and the trips.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        List<Trip> trips = new ArrayList<>();
        trips.add(trip);
        String result = mockMvc.perform(get(url)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Trip.class);
        Response<List<Trip>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
        Assertions.assertEquals(new Response<>(1, "Success", trips), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving all trips correctly handles the case
     * when there are no trips in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get(url)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Trip.class);
        Response<List<Trip>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }
}