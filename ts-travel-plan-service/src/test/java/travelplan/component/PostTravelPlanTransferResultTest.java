package travelplan.component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import travelplan.entity.TransferTravelInfo;
import travelplan.entity.TransferTravelResult;
import travelplan.entity.TripInfo;
import travelplan.entity.TripResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/travelplanservice/travelPlan/transferResult endpoint.
 * This endpoint send POST request to both ts-travel-station and ts-travel2-station to retrieve high-speed and normal train trips
 * for the first and second section. The two sections are then combined and stored into TransferTravelResult object.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostTravelPlanTransferResultTest {
    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private TransferTravelInfo travelInfo;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        travelInfo = new TransferTravelInfo("fromStationName", "viaStationName", "toStationName", new Date(), "G1234");
    }


    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case to verify  that the endpoint correctly processes a valid TransferTravelInfo object and returns the expected TransferTravelResult.
     * The test mocks the responses for the travel service and travel2 service endpoints to simulate the behavior of external services.
     * The test expects the response status to be OK and that the response data contains the expected trip responses for both the first and second sections of the travel.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        TripInfo tripInfo = new TripInfo(travelInfo.getFromStationName(), travelInfo.getViaStationName(), travelInfo.getTravelDate());

        List<TripResponse> firstSectionFromHighSpeed = new ArrayList<>();
        List<TripResponse> firstSectionFromNormal = new ArrayList<>();

        TripResponse tripResponsefirstSectionFromHighSpeed1 = new TripResponse();
        tripResponsefirstSectionFromHighSpeed1.setStartingStation(tripInfo.getStartingPlace());
        tripResponsefirstSectionFromHighSpeed1.setTerminalStation("EndStationSpeed");

        TripResponse tripResponsefirstSectionFromNormal1 = new TripResponse();
        tripResponsefirstSectionFromNormal1.setStartingStation("EndStationSpeed");
        tripResponsefirstSectionFromNormal1.setTerminalStation(tripInfo.getEndPlace());

        firstSectionFromHighSpeed.add(tripResponsefirstSectionFromHighSpeed1);
        firstSectionFromNormal.add(tripResponsefirstSectionFromNormal1);

        Response<List<TripResponse>> responseFirstSectionTravelService = new Response<>(1, "Success", firstSectionFromHighSpeed);
        Response<List<TripResponse>> responseFirstSectionTravel2Service = new Response<>(1, "Success.", firstSectionFromNormal);

        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips/left"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFirstSectionTravelService)));

        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips/left"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseFirstSectionTravel2Service)));

        TripInfo tripInfo2 = new TripInfo(travelInfo.getViaStationName(), travelInfo.getToStationName(), travelInfo.getTravelDate());

        List<TripResponse> secondSectionFromHighSpeed = new ArrayList<>();
        List<TripResponse> secondSectionFromNormal = new ArrayList<>();

        TripResponse tripResponseSecondSectionFromHighSpeed1 = new TripResponse();
        tripResponseSecondSectionFromHighSpeed1.setStartingStation(tripInfo2.getStartingPlace());
        tripResponseSecondSectionFromHighSpeed1.setTerminalStation("terminalStationSpeed");

        TripResponse tripResponseSecondSectionFromNormal1 = new TripResponse();
        tripResponseSecondSectionFromNormal1.setStartingStation("terminalStationSpeed");
        tripResponseSecondSectionFromNormal1.setTerminalStation(tripInfo2.getEndPlace());

        secondSectionFromHighSpeed.add(tripResponseSecondSectionFromHighSpeed1);
        secondSectionFromNormal.add(tripResponseSecondSectionFromNormal1);

        Response<List<TripResponse>> responseSecondSectionTravelService = new Response<>(1, "Success", secondSectionFromHighSpeed);
        Response<List<TripResponse>> responseSecondSectionTravel2Service = new Response<>(1, "Success.", secondSectionFromNormal);

        mockServer.expect(requestTo("http://ts-travel-service:12346/api/v1/travelservice/trips/left"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseSecondSectionTravelService)));

        mockServer.expect(requestTo("http://ts-travel2-service:16346/api/v1/travel2service/trips/left"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(responseSecondSectionTravel2Service)));

        List<TripResponse> firstSection = new ArrayList<>();
        firstSection.addAll(firstSectionFromHighSpeed);
        firstSection.addAll(firstSectionFromNormal);

        List<TripResponse> secondSection = new ArrayList<>();
        secondSection.addAll(secondSectionFromHighSpeed);
        secondSection.addAll(secondSectionFromNormal);

        TransferTravelResult result = new TransferTravelResult();
        result.setFirstSectionResult(firstSection);
        result.setSecondSectionResult(secondSection);

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travelInfo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<TransferTravelResult> actualResponse = JSONObject.parseObject(response, new TypeReference<Response<TransferTravelResult>>() {
        });
        Assertions.assertEquals(new Response<>(1, "Success.", result), actualResponse);

        Assertions.assertTrue(actualResponse.getData().getFirstSectionResult().contains(tripResponsefirstSectionFromHighSpeed1));
        Assertions.assertTrue(actualResponse.getData().getFirstSectionResult().contains(tripResponsefirstSectionFromNormal1));

        Assertions.assertTrue(actualResponse.getData().getSecondSectionResult().contains(tripResponseSecondSectionFromHighSpeed1));
        Assertions.assertTrue(actualResponse.getData().getSecondSectionResult().contains(tripResponseSecondSectionFromNormal1));
    }

    /*
     * Test case to verify that the endpoint correctly handles a request with multiple TransferTravelInfo objects in the request body, which should result in a client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(travelInfo);
        jsonArray.add(travelInfo);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case to verify that the endpoint correctly handles a malformed JSON object, resulting in a Bad Request.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{fromStationName: startingPlace, toStationName: endStation}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(malformedJson)))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify that the endpoint correctly handles a request with a missing request body, resulting in a Bad Request.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case to verify that the endpoint can handle a TransferTravelInfo object with a null value for the fromStationName field, and still processes the request successfully.
     */
    @Test
    void bodyVar_startingPlace_validTestNull() throws Exception {
        travelInfo.setFromStationName(null);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(travelInfo)))
                .andExpect(status().isOk());
    }
}
