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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This is a test class for the DELETE endpoint of the Train Service. It is used to test if a train type can be deleted.
 * It interacts only with the database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteTrainTest {

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Delete a train type with a valid id. Ensure that the train type is deleted from the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String result = mockMvc.perform(delete("/api/v1/trainservice/trains/123"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(1, "delete success", true);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
        assertFalse(trainTypeRepository.findById("123").isPresent());
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/


    /*
     * Try to delete a train type with a non-existing id. Ensure that the repository is not modified.
     * REMARK: The implementation of the Endpoint is not correct, this is why this test fails.
     */
    @Test
    void invalidTestNonExistingId() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String result = mockMvc.perform(delete("/api/v1/trainservice/trains/999"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "there is no train according to id", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
        assertTrue(trainTypeRepository.findById("123").isPresent());
    }

    /*
     * Try to delete a train type with an id that is not in the correct format. Ensure that the repository is not modified.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String result = mockMvc.perform(delete("/api/v1/trainservice/trains/abc123"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "there is no train according to id", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
        assertTrue(trainTypeRepository.findById("123").isPresent());
    }

    /*
     * Try to delete a train type with an id that contains characters that are not allowed. Ensure that the repository is not modified.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String result = mockMvc.perform(delete("/api/v1/trainservice/trains/!@#"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "there is no train according to id", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
        assertTrue(trainTypeRepository.findById("123").isPresent());
    }
}
