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
 * This endpoint is for deleting the specific station with the given id and name from the repository. It takes a Station
 * object as body. As such we test delete specific REST defect tests, url parameter specific equivalence class
 * based tests as well as defect tests. It interacts only with the database, which is why we need to setup a MongoDBContainer for the
 * repository. The implementation logic for deleting the new object in the repository has a HUGE error (comparing null with
 * Optional), which causes the new object to always be deleted even if it doesn't exist. As such the corresponding tests in this class fail right now,
 * because the actual implementation is wrong.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteStationTest {

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * In this test we test the general case, where the id of the object body is a valid id of a Route object in the repository,
     * which means the deletion is executed normally.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Station station = new Station("1", "name");
        stationRepository.save(station);
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"}";


        String result = mockMvc.perform(delete("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(stationRepository.findByName("name"));
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"},{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"}]";

        mockMvc.perform(delete("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no object with the id exists in the repository. Therefore, the response should be
     * different, but as an Optional value is compared with null in the implementation, the other response can never be reached.
     */
    @Test
    void validTestMissingObject() throws Exception {
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"}";

        String result = mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "Station not exist", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":not, \"name\":valid, \"stayTime\":null}";

        mockMvc.perform(delete("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * In this test case the JSON body is empty, which means that there is no object to DELETE
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(delete("/api/v1/stationservice/stations")
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
        String requestJson = "{\"id\":null, \"name\":null, \"stayTime\":\"0\"}";


        assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete("/api/v1/stationservice/stations")
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
        Station station = new Station("1", "name", Integer.MIN_VALUE);
        stationRepository.save(station);
        String requestJson = "{\"id\":\"1\", \"name\":\"name\", \"stayTime\":" + Integer.MIN_VALUE + "}";

        String result = mockMvc.perform(delete("/api/v1/stationservice/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNull(stationRepository.findByName("name"));
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

}
