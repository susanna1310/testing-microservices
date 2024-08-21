package admintravel.component;

import admintravel.entity.TravelInfo;
import admintravel.entity.Trip;
import admintravel.entity.TripId;
import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.Objects;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/admintravelservice/admintravel/{tripId} endpoint.
 * This endpoint send a DELETE request to ts-travel-service or ts-travel2-service, dependent on with what letter tripId starts,
 * to delete a specific travelInfo object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminTravelTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private TravelInfo travelInfo;
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }
    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        travelInfo = new TravelInfo();
        travelInfo.setTripId("G1234");
        travelInfo.setTrainTypeId("13");
        travelInfo.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        travelInfo.setStartingStationId("muenchen");
        travelInfo.setStationsId("95");
        travelInfo.setTerminalStationId("berlin");
        travelInfo.setStartingTime(new Date(2020, 12, 4, 15, 0));
        travelInfo.setEndTime(new Date(2020, 12, 4, 20, 30));
    }
    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case for valid deletion of a travelInfo object and the tripId starts with 'G' or 'D'.
     * The response of ts-travel-service is mocked to indicate that the object got deleted successfully.
     * The test expects the response to be equal to the mocked response and status OK.
     */
    @Test
    void validTestCorrectObjectTravelService() throws Exception {
        // tripId starts with 'G', add travel to ts-travel-service
        Response<String> expectedResponse = new Response<>(1, "Delete trip:" + travelInfo.getTripId() + ".", travelInfo.getTripId());
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips/" + travelInfo.getTripId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", travelInfo.getTripId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case for valid deletion of a travelInfo object and the tripId does not start with 'G' or 'D'.
     * The response of ts-travel2-service is mocked to indicate that the object got deleted successfully.
     * The test expects the response to be equal to the mocked response and status OK.
     */
    @Test
    void validTestCorrectObjectTravel2Service() throws Exception {
        // Change tripId, so it does not start with 'G' or 'D'
        // No we expect response from ts-travel2-service
        travelInfo.setTripId("B1234");

        Response<String> expectedResponse2 = new Response<>(1, "Delete trip:" + travelInfo.getTripId() + ".", travelInfo.getTripId());
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips/" + travelInfo.getTripId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse2)));

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", travelInfo.getTripId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse2, JSONObject.parseObject(actualResponse2, Response.class));
    }

    /*
     * Test case for valid request to delete multiple objects by writing multiple tripIds as path variable.
     * The test expects status OK, because the first tripId is used and the second one is ignored.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", "G1234", "B1234"))
                .andExpect(status().isOk());
    }



    /*
     * Test case to verify the handling of malformed `tripId` in the path variable, which should result in a client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", "G1234/D1234"))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Tests the scenario where no `tripId` is provided in the path variable, which should throw an `IllegalArgumentException`.
     */
    @Test
    void invalidTestMissingObject() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}")));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Tests the scenario where a non-existing `tripId` is provided. Mocks responses from both `ts-travel-service` and
     * `ts-travel2-service` for the respective scenarios and expects a response indicating the trip doesn't exist.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        // TripId starts with 'G', so ts-travel-service
        travelInfo.setTripId("DnonExistingId");
        Response<Object> expectedResponse = new Response<>(0, "Trip " + travelInfo.getTripId() + " doesn't exist.", null);
        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips/" + travelInfo.getTripId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", travelInfo.getTripId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));

        // Change tripId, so it does not start with 'G' or 'D'
        // No we expect response from ts-travel2-service

        travelInfo.setTripId("BnonExistingId");
        mockServer = MockRestServiceServer.createServer(restTemplate);

        Response<String> expectedResponse2 = new Response<>(0, "Trip " + travelInfo.getTripId() + " doesn't exist.", null);
        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips/" + travelInfo.getTripId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse2)));

        String actualResponse2 = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", travelInfo.getTripId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse2, JSONObject.parseObject(actualResponse2, Response.class));
}

    /*
     * Validates the handling of non-correct format `tripId` or special characters in the path variable, which should
     * result in an OK status.
     */
    @Test
    void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        int nonCorrectFormat = 1;
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admintravelservice/admintravel/{tripId}", nonCorrectFormat)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk());
    }
}
