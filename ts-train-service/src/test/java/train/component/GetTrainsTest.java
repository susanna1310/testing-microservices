package train.component;


import com.alibaba.fastjson.JSONObject;
import train.entity.TrainType;
import train.repository.TrainTypeRepository;
import edu.fudan.common.util.Response;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
* This is a test class for the GET endpoint of the Train Service. It is used to test the retrieval of all objects in the
* service repository. It interacts only with the database, which is why we need to setup a MongoDBContainer for the
* repository.
*/
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetTrainsTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainTypeRepository trainTypeRepository;

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
        trainTypeRepository.deleteAll();
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * This is to test if the GET endpoint can retrieve a large amount of objects, which is why the repository is filled
     * with non-specific objects. For the return response we have to check if the data list has the same amount of objects
     * and for the success message.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        for (int i = 0; i < 10000; i++) {
            trainTypeRepository.save(new TrainType());
        }

        String result = mockMvc.perform(get("/api/v1/trainservice/trains")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TrainType> trainTypes = (List<TrainType>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(10000, trainTypes.size());
        assertEquals(new Response<>(1, "Success", trainTypes), JSONObject.parseObject(result, Response.class));
    }

    /*
     * A GET request with no data, so it should return nothing.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/trainservice/trains")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "no Content", null), JSONObject.parseObject(result, Response.class));
    }
}
