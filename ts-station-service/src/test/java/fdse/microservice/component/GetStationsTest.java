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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs all station objects from the station repository. As this endpoint has no body or argument, there is
 * only a two equivalence classes to test. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetStationsTest {

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
     * With the first equivalence class we test the outcome when we get a list of stations back. For that we simply need
     * to insert them into the repository before the request
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        for (int i = 0; i < 50; i++) {
            stationRepository.save(new Station());
        }

        String result = mockMvc.perform(get("/api/v1/stationservice/stations")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<Station> stations = (List<Station>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(50, stations.size());
        assertEquals(new Response<>(1, "Find all content", stations), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the second equivalence class test we do not get any content back. This can be achieved by leaving the repository
     * empty.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/stationservice/stations")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No content", null), JSONObject.parseObject(result, Response.class));
    }
}
