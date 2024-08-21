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
 * Test class for POST /api/v1/adminbasicservice/adminbasic/stations endpoint
 * This endpoint send a POST request to ts-station-service to create a new station object.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminBasicStationTest
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
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Valid test case for creating a new station object.
     * Verifies that the POST operation succeeds with an OK status and the response matches expected result.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Station> response = new Response<>(1, "Create success", station);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, new TypeReference<Response<Station>>(){}));
    }

    /*
     * Invalid test case for sending multiple station objects in a single request.
     * Verifies that the POST operation fails with a 4xx client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray json = new JSONArray();
        json.add(station);
        json.add(station);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for attempting to create a station object that already exists.
     * Verifies that the POST operation returns an OK status with a response indicating the object already exists.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Response<Station> response = new Response<>(0, "Already exists", station);
        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", station.getId());
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(actualResponse, new TypeReference<Response<Station>>(){}));
    }

    /*
     * Invalid test case for sending a malformed JSON object.
     * Verifies that the POST operation fails with a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', name: 'Name'}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for sending a request with a missing station object.
     * Verifies that the POST operation fails with a bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for ID with a string that is too short.
     * Expects an OK status.
     */
    @Test
    void bodyVar_id_validTestStringTooShort() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32");
        json.put("name", station.getName());
        json.put("stayTime", station.getStayTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/stations")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
