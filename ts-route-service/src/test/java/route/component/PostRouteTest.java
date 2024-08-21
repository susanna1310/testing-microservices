package route.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import route.entity.Route;
import route.repository.RouteRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint either creates a route or modifies a route and saves it in the repository. It gets a RouteInfo object as
 * the body and uses its parameters to create/modify the route. As such we test defect tests for the REST endpoint, equivalence
 * class tests for the attributes of the object and specific defect tests for this endpoint. It interacts only with the
 * database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostRouteTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RouteRepository routeRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    void beforeEach() {
        routeRepository.deleteAll();
    }


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This is to test the standard case of POST with a correct object and checking afterwards if it was correctly saved
     * in the repository and for the Success response.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        String requestJson = "{\"id\":\"1\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(1, routeRepository.findAll().size());
        assertNotEquals("1", routeRepository.findAll().get(0).getId());
        assertEquals("1", routeRepository.findAll().get(0).getStartStationId());
        assertEquals("2", routeRepository.findAll().get(0).getTerminalStationId());
        Route route = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);
        assertEquals("1", route.getStations().get(0));
        assertEquals("2", route.getStations().get(1));
        assertEquals(0, route.getDistances().get(0));
        assertEquals(300, route.getDistances().get(1));
        assertEquals(new Response<>(1, "Save Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));


    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":\"1\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"},{\"id\":\"1\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}]";

        mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * This defect test case is for if the route with the JSON id already exists in the repository, which means it will
     * be modified and not newly created. We do this by performing 2 POST requests. Therefore the response will be different.
     */
    @Test
    void validTestDuplicateObject() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";

        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk());

        assertTrue(routeRepository.findById("1234567890").isPresent());

        requestJson = "{\"id\":\"1234567890\", \"startStation\":\"3\", \"endStation\":\"4\", \"stationList\":\"3,4\", \"distanceList\":\"0,300\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(routeRepository.findById("1234567890").isPresent());
        assertEquals("3", routeRepository.findById("1234567890").get().getStartStationId());
        assertEquals("4", routeRepository.findById("1234567890").get().getTerminalStationId());

        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":false, \"startStation\":json, \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";

        mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * In this test case the JSON body is empty, which means that there is no object to POST
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Here we test the equivalence class for the id attribute, when it has correct length (>=10), so the id is
     * being used for the newly created Route object and the response is different.
     */
    @Test
    void bodyVarIdValidTestCorrectLengthAndCharacters() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        assertNotEquals("1", routeRepository.findById("1234567890").get().getId());
        assertEquals("1", routeRepository.findById("1234567890").get().getStartStationId());
        assertEquals("2", routeRepository.findById("1234567890").get().getTerminalStationId());
        Route route = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);
        assertEquals("1", route.getStations().get(0));
        assertEquals("2", route.getStations().get(1));
        assertEquals(0, route.getDistances().get(0));
        assertEquals(300, route.getDistances().get(1));
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this equivalence class the id string is too short (<10) (already seen in test above) or null, which is why the
     * created route object has a new id and the response is for save not modify.
     */
    @Test
    void bodyVarIdInvalidTestStringTooShortOrNull() throws Exception {
        String requestJson = "{\"id\":null, \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";


        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(1, routeRepository.findAll().size());
        assertNotNull(routeRepository.findAll().get(0).getId());
        assertEquals(new Response<>(1, "Save Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we assign Strings with unusual characters to the attributes, because for attributes like id you would
     * expect a numbers and letters, which is why it would be a new equivalence class.
     */
    @Test
    void bodyVarIdInvalidTestStringContainsWrongCharacters() throws Exception {
        String requestJson = "{\"id\":\"§$%&=,.-#*\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";


        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("§$%&=,.-#*").isPresent());
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Now we have the equivalence classes for the startStation and endStation attributes. As they are Strings with no
     * restrictions, the members are all in the same equivalence class. Here we test the empty String and null for
     * the stations.
     */
    @Test
    void bodyVarStationValidTestCorrectLengthAndCharacters() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"\", \"endStation\":null, \"stationList\":\"1,2\", \"distanceList\":\"0,300\"}";


        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }


    /*
     * The station list attributes are supposed to be Strings with stations/distances divided by ',', so they can be converted into
     * lists. As such there are a few equivalence classes beginning with correct and valid Strings
     */
    @Test
    void bodyVarStationlistValidTestCorrectLengthAndCharacters() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,test,3,,()/&\", \"distanceList\":\"1,2,3,4,5\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        String[] stations = "1,test,3,,()/&".split(",");
        String[] distances = "1,2,3,4,5".split(",");
        Route route = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);
        for(int i = 0; i<stations.length; i++) {
            assertEquals(stations[i], route.getStations().get(i));
            assertEquals(Integer.parseInt(distances[i]), route.getDistances().get(i));
        }
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test "too short"/empty Strings for the station lists, which do not have a ',' where they can be split.
     */
    @Test
    void bodyVarStationlistInvalidTestStringTooShort() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"\", \"distanceList\":\"1\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        String[] stations = "".split(",");
        String[] distances = "1".split(",");
        Route route = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);
        for(int i = 0; i<stations.length; i++) {
            assertEquals(stations[i], route.getStations().get(i));
            assertEquals(Integer.parseInt(distances[i]), route.getDistances().get(i));
        }
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this equivalence class the station lists are null, which means the split() method cannot be used on them. The
     * expected response is an exception. It could also be seen as a defect case, if we do not have the info about the
     * split() for creating the equivalence classes.
     */
    @Test
    void bodyVarStationlistInvalidTestStringIsNull() {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":null, \"distanceList\":null}";

        assertThrows(NestedServletException.class, () -> {mockMvc.perform(post("/api/v1/routeservice/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        );});

    }

    /*
     * For this equivalence class we assign a String so that the resulting lists are empty after the split, which means
     * they have the length 0. Because the result of the lists is an empty list, we categorize this as a new equivalence
     * class compared to the others.
     */
    @Test
    void bodyVarStationlistlistValidTestEmptyList() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\",\", \"distanceList\":\",\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        assertEquals(0, routeRepository.findById("1234567890").get().getDistances().size());
        assertEquals(0, routeRepository.findById("1234567890").get().getStations().size());
        assertEquals(new Response<>(1, "Modify success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }


    /*
     * In this specific defect case test, we test the situation when the station list and distance list have different
     * lengths after the split, which results in a different response from the endpoint.
     */
    @Test
    void listsDifferentLengthsTest() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"1,2\", \"distanceList\":\"2\"}";

        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertFalse(routeRepository.findById("1234567890").isPresent());
        assertEquals(new Response<>(0, "Station Number Not Equal To Distance Number", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this equivalence class/defect case test, we test the situation when the distance list has invalid characters, which results in
     * an exception, because the substrings between the ',' after the split are parsed into Integers.
     */
    @Test
    void distanceListInvalidCharactersTest() {
        String requestJson = "{\"id\":\"1234567890\", \"startStation\":\"1\", \"endStation\":\"2\", \"stationList\":\"2\", \"distanceList\":\"invalid characters()/&\"}";

        assertThrows(NestedServletException.class, () -> {mockMvc.perform(post("/api/v1/routeservice/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        );});
    }
}
