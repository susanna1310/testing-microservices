package fdse.microservice.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import fdse.microservice.entity.Station;
import fdse.microservice.repository.StationRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is for creating a station object with the given attributes in the repository. It takes a Station
 * object as body. As such we test equivalence classes for the input. It interacts only with the database, which is why
 * we need to setup a MongoDBContainer for the repository. In the implementation of the logic for this endpoint there is
 * null compared with Optional, which is always false. As such we can never get the condition that the object with the
 * id does not exist and this endpoint can never create and save an object in the repository. Because the
 * actual response/behaviour is wrong, some tests still fail.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostStationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StationRepository stationRepository;

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
        stationRepository.deleteAll();
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in creating and saving the
     * input object in the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":1}";

        String result = mockMvc.perform(post("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertTrue(stationRepository.findById("1").isPresent());
        assertEquals("name",stationRepository.findById("1").get().getName());
        assertEquals(1,stationRepository.findById("1").get().getStayTime());
        assertEquals(new Response<>(1, "Save success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"1\"},{\"id\":\"2\", \"name\":\"name2\", \"stayTime\":\"1\"}]";


        mockMvc.perform(post("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * For this test, we already insert the object of the body into the repository before the request. With the combination
     * of a valid attribute class and the insertion beforehand, we get a different response.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Station station = new Station("1", "name", 1);
        stationRepository.save(station);
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"1\"}";


        String result = mockMvc.perform(post("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        station = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Station.class);
        assertEquals("1",station.getId());
        assertEquals("name",station.getName());
        assertEquals(1,station.getStayTime());
        assertEquals(new Response<>(0, "Already exists", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc,which should not be able to be converted into the right object. We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        mockMvc.perform(post("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to post.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/stationservice/stations")
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
     * The id and name attributes of the object are Strings with no restriction, which means there is only one equivalence classes
     * with valid values, because they all lead to the same output. We tested that class already above for both
     * attributes. But the null value for the id is actually a separate equivalence class as it causes an exception, when
     * we send such a json body.
     */
    @Test
    void bodyVarIdNameInvalidTestStringIsNull() throws Exception {
        String requestJson = "{\"id\":null, \"name\":null, \"stayTime\":\"0\"}";

        assertThrows(NestedServletException.class, () -> {mockMvc.perform(post("/api/v1/stationservice/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        );});
    }

    /*
     * The equivalence classes for the staytime attribute are similar. As it is an int, we either have values that are int
     * or not. For the valid values we test a negative very small integer to simultaneously cover that edge case.
     */
    @Test
    void bodyVarStaytimeValidTest() throws Exception {
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertTrue(stationRepository.findById("1").isPresent());
        assertEquals("name",stationRepository.findById("1").get().getName());
        assertEquals(Integer.MIN_VALUE,stationRepository.findById("1").get().getStayTime());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }
}
