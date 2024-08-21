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
import travel2.entity.TrainType;
import travel2.entity.Trip;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve the train type that matches to the trip with the given trip ID via GET.
 * To get the train type it communicates with the ts-train-service.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetTravelServiceTrainTypesTripIdTest extends BaseComponentTest {

    private final String url = "/api/v1/travel2service/train_types/{tripId}";
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
     * The test is designed to verify that the endpoint for retrieving the train type works correctly, for a valid ID with a trip that exists in the database and a matching train type.
     * It ensures that the endpoint returns a successful response with the appropriate message and the train type.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Trip trip = createSampleTrip();
        tripRepository.save(trip);
        TrainType trainType = new TrainType();
        trainType.setId(trip.getTrainTypeId());

        Response<TrainType> responseTrainType = new Response<>(1, "success", trainType);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-train-service:14567/api/v1/trainservice/trains/" + trip.getTrainTypeId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainType), MediaType.APPLICATION_JSON));


        String result = mockMvc.perform(get(url, trip.getTripId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TrainType> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TrainType.class));
        mockServer.verify();
        Assertions.assertEquals(new Response<>(1, "Success query Train by trip id", trainType), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when there is no trip associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Trip trip = createSampleTrip();

        String result = mockMvc.perform(get(url, trip.getTripId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TrainType> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TrainType.class));
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when there is no train type that matches the trip. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetNoRoutes() throws Exception {
        Trip trip = createSampleTrip();

        tripRepository.save(trip);
        Response<TrainType> responseTrainType = new Response<>(0, "here is no TrainType with the trainType id: " + trip.getTripId(), null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-train-service:14567/api/v1/trainservice/trains/" + trip.getTrainTypeId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainType), MediaType.APPLICATION_JSON));

        String result = mockMvc.perform(get(url, trip.getTripId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TrainType> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TrainType.class));
        mockServer.verify();
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
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