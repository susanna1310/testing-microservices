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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint updates a Station object by creating a new one and replacing it in the repository. It gets a Station object as
 * the body and uses its parameters to create the Station object. As such we test equivalence class tests for the
 * attributes of the object. It interacts only with the database, which is why we need to setup a MongoDBContainer for
 * the repository. Similar for the PostStationTest class, in the implementation of the logic for this endpoint there is
 * also null compared with Optional, which is always false. As such we can never get the response that the object with the
 * id does not exist and it basically functions as a create endpoint, because the object will be created anyway. Because the
 * actual response/behaviour is wrong, some tests still fail.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutStationTest {

    @Autowired
    private MockMvc mockMvc;

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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in updating the object
     * with the id in the repository.
     */
    @Test
    void validTestUpdatesCorrectObject() throws Exception {
        Station station = new Station("1", "name", 1);
        stationRepository.save(station);
        String requestJson = "{\"id\":\"1\", \"name\":\"nameNew\", \"stayTime\":\"2\"}";

        String result = mockMvc.perform(put("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertTrue(stationRepository.findById("1").isPresent());
        assertEquals("nameNew", stationRepository.findById("1").get().getName());
        assertEquals(2, stationRepository.findById("1").get().getStayTime());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the combination of a correct body object, which does not exist in the repository with the given id.
     * As such there is nothing to update and the response will be different.
     */
    @Test
    void invalidTestUpdateNonexistingObject() throws Exception {
        String requestJson = "{\"id\":\"1\", \"name\":\"nameNew\", \"stayTime\":\"2\"}";

        String result = mockMvc.perform(put("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(stationRepository.findById("1").isPresent());
        assertEquals(new Response<>(0, "Station not exist", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"1\"},{\"id\":\"2\", \"name\":\"name2\", \"stayTime\":\"1\"}]";


        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we test the case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc,which should not be able to be converted into the right object. We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to update.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(put("/api/v1/stationservice/stations")
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
    void bodyVarIdNameInvalidTestNull() {
        Station station = new Station(null, null);
        stationRepository.save(station);
        String requestJson = "{\"id\":null, \"name\":\"nameNew\", \"stayTime\":\"0\"}";


        assertThrows(NestedServletException.class, () -> {mockMvc.perform(put("/api/v1/stationservice/stations")
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
        Station station = new Station("1", "name", 1);
        stationRepository.save(station);
        String requestJson = "{\"id\":\"1\", \"name\":\"nameNew\", \"stayTime\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(stationRepository.findByName("nameNew"));
        assertEquals(Integer.MIN_VALUE, stationRepository.findByName("nameNew").getStayTime());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }
}
