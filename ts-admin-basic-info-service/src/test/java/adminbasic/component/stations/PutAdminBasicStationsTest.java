package adminbasic.component.stations;

import adminbasic.entity.Station;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for PUT /api/v1/adminbasicservice/adminbasic/stations endpoint
 * This endpoint sends a PUT request to ts-station-service to update a station object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminBasicStationsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private Station station;

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        station = new Station(UUID.randomUUID().toString(), "Name", 10);
    }

    /*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Valid test case for updating an existing station object.
     * Verifies that the PUT operation succeeds with an OK status and the response matches expected result.
     */
    @Test
    void validTestCorrectObject() throws  Exception {
        Station updatedStation = new Station(station.getId(), "UpdatedName", 20);

        Response<Station> response = new Response<>(1, "Update success", updatedStation);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", updatedStation.getId());
        json.put("name", updatedStation.getName());
        json.put("stayTime", updatedStation.getStayTime());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<Station>>(){}));
    }

    /*
     * Valid test case for updating an existing station object and verifying updated values.
     * Verifies that the PUT operation succeeds with an OK status and the updated station object's values are correct.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        Station updatedStation = new Station(station.getId(), "UpdatedName", 20);

        Response<Station> response = new Response<>(1, "Update success", updatedStation);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", updatedStation.getId());
        json.put("name", updatedStation.getName());
        json.put("stayTime", updatedStation.getStayTime());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();

        Response<Station> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<Station>>(){});
        Assertions.assertEquals(actualResponse.getData().getName(), "UpdatedName");
        Assertions.assertEquals(actualResponse.getData().getStayTime(), 20);
    }

    /*
     *  Invalid test case for sending multiple station objects in a single request.
     * Verifies that the PUT operation fails with a 4xx client error status.
     * [doc~]
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(station);
        jsonArray.add(station);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for sending a malformed JSON object.
     * Verifies that the PUT operation fails with a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', name: 'Name'}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for sending a request with a missing station object.
     * Verifies that the PUT operation fails with a bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Valid test case for updating a non-existing station object.
     * Verifies that the PUT operation returns a response indicating the station does not exist.
     */
    @Test
    void bodyVar_id_validTestNotExisting() throws Exception {
        Station updatedStation = new Station(station.getId(), "UpdatedName", 20);

        Response<Object> response = new Response<>(0, "Station not exist", null);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", updatedStation.getId());
        json.put("name", updatedStation.getName());
        json.put("stayTime", updatedStation.getStayTime());
        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Valid test case for ID with a string that is too long.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringTooLong() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32-68b4db4ac3e8-68b4db4ac3e8");
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     *  Valid test case for ID with a string that is too short.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringTooShort() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32");
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for ID with a string containing wrong characters.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringContainsWrongCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for ID with a null value.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for stay time with a value within range and negative.
     * Expects an OK status.
     */
    @Test
    void bodyVar_staytime_validTestValueIsNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", station.getName());
        json.put("stayTime", -1);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for stay time with a wrong variable type (string instead of integer).
     * Expects a bad request status.
     */
    @Test
    void bodyVar_staytime_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", station.getName());
        json.put("stayTime", "shouldNotBeString");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Valid test case for stay time with a null value.
     * Expects an OK status.
     */
    @Test
    void bodyVar_staytime_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", station.getName());
        json.put("stayTime", null);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for name with correct length and any characters.
     * Expects an OK status.
     */
    @Test
    void bodyVar_name_validTestCorrectTooShortAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", "&%(&%/&%/");
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for name with a string that is too long.
     * Expects an OK status.
     */
    @Test
    void bodyVar_name_validTestStringTooLong() throws Exception {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String tooLongName = new String(chars);

        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", tooLongName);
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for name with a null value.
     * Expects a bad request status.
     */
    @Test
    void bodyVar_name_invalidTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", null);
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }


}
