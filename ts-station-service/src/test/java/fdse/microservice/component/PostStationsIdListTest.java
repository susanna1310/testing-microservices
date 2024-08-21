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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * In this endpoint we do a POST request of a station name list and get the corresponding id list. It takes a List<String> as body.
 * As such we test equivalence classes for the input. It interacts only with the database, which is why we need to setup
 * a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostStationsIdListTest {

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
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * For the first equivalence class we want to get all objects, which is why we insert the object before the request.
     * As such the combination of a valid input name from the equivalence class and the insertion before, we reach the
     * corresponding response.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        List<String> nameList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Station station = new Station(String.valueOf(i), String.valueOf(i));
            nameList.add(String.valueOf(i));
            stationRepository.save(station);
        }
        String requestJson = JSONObject.toJSONString(nameList);

        String result = mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<String> stations = (List<String>) (JSONObject.parseObject(result, Response.class).getData());
        for (int i = 0; i < 10000; i++) {
            assertEquals(String.valueOf(i), stations.get(i));
        }
        assertEquals(new Response<>(1, "Success", stations), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        List<List<String>> lists = new ArrayList<>();
        lists.add(new ArrayList<>());
        lists.add(new ArrayList<>());
        String requestJson = JSONObject.toJSONString(lists);

        mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
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
        String requestJson = "{[not, a, valid, json]}";

        mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
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

        mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
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
     * There are a few equivalence classes for the input list which cause different outcomes we want to test for.
     * We already tested one outcome in the first test case above. Here we want to test when we have an input name value in
     * the list, which more than one object in the repository has. This case should normally not occur, which is why
     * an exception is thrown.
     */
    @Test
    void bodyVarStationNameListInvalidTestStringIsDuplicate() {
        List<String> nameList = new ArrayList<>();
        nameList.add(null);
        Station station = new Station("1", null);
        stationRepository.save(station);
        station.setId("2");
        stationRepository.save(station);
        String requestJson = JSONObject.toJSONString(nameList);

        assertThrows(NestedServletException.class, () -> {mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                );});
    }

    /*
     * For this equivalence class, we have a mixed list as input with only a few existing names in the repository.
     * As such the returned data should be a mix of ids and "Not exist".
     */
    @Test
    void bodyVarStationNameListValidTestMixedList() throws Exception {
        List<String> nameList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            nameList.add(String.valueOf(i));
            if(i % 2 == 0) {
                Station station = new Station(String.valueOf(i), String.valueOf(i));
                stationRepository.save(station);
            }
        }
        String requestJson = JSONObject.toJSONString(nameList);

        String result = mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<String> stations = (List<String>) (JSONObject.parseObject(result, Response.class).getData());
        for (int i = 0; i < stations.size(); i++) {
            if(i % 2 == 0) {
                assertEquals(String.valueOf(i), stations.get(i));
            } else {
                assertEquals("Not Exist", stations.get(i));
            }
        }
        assertEquals(new Response<>(1, "Success", stations), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the last equivalence class from inputs we want to get the outcome response that there is no content. Because of
     * the implementation adding "Not exist" to the result data list if the name does not exist in the repository and afterwards
     * checking if the result data list is empty for the response, we can never get the response except if our input list
     * is also empty.
     */
    @Test
    void bodyVarStationNameListValidTestEmptyList() throws Exception {
        List<String> nameList = new ArrayList<>();
        String requestJson = JSONObject.toJSONString(nameList);

        String result = mockMvc.perform(post("/api/v1/stationservice/stations/idlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No content according to name list", null), JSONObject.parseObject(result, Response.class));

    }
}
