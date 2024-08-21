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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs the corresponding station id to the given name over the URL parameter. Which is why we need to test
 * the equivalence classes for the parameter. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetStationNameForIdTest {

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
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * For the first equivalence class we want to get the object, which is why we insert the object before the request.
     * As such the combination of a valid input name and the insertion before, we reach the corresponding response.
     */
    @Test
    void validTestGetObject() throws Exception {
        Station station = new Station("1", "name");
        stationRepository.save(station);

        String result = mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}", "name")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Success", "1"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For the second equivalence class, we do not insert anything, so we don't get any name for the stationName, because the
     * repository is empty. This is the outcome as when we query with a name, that no object in the repository has.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {

        String result = mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}", "name")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "Not exists", "name"), JSONObject.parseObject(result, Response.class));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * For this test we do not input a URL parameter, which should cause an exception
     */
    @Test
    void invalidTestMissingBody() {
        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}")
        );});
    }

    /*
     * For the last equivalence class we give an invalid formatted name as the URL parameter, which should cause a 4xx client error.
     */
    @Test
    void invalidTestNonCorrectFormatName() throws Exception {
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}","name/invalid")
                )
                .andExpect(status().is4xxClientError());
    }
}
