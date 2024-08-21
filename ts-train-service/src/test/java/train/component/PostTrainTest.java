package train.component;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This is a test class for the POST endpoint of the Train Service. It is used to test the creation of a new train type.
 * (/api/v1/trainservice/trains)
 * It interacts only with the database, which is why we need to setup a MongoDBContainer for the repository.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostTrainTest
{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainTypeRepository trainTypeRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    @Autowired
    protected ObjectMapper objectMapper;


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
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test if a valid object can be created. And the object is stored in the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        String trainTypeJson = JSONObject.toJSONString(trainType);

        String result = mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trainTypeJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(1, "create success", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
        assertTrue(trainTypeRepository.findById("123").isPresent());
    }


    /*
     * Test if an object with a duplicate id (already existing) cannot be created. Ensure that the object is not stored in the repository.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String trainTypeJson = JSONObject.toJSONString(trainType);

        String result = mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trainTypeJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response<TrainType> expectedResponse = new Response<>(0, "train type already exist", trainType);
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TrainType> actualResponse = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TrainType.class));

        assertEquals(expectedResponse, actualResponse);
        assertEquals(1, trainTypeRepository.count()); // Ensure no duplicate was created
    }

    /*
     * Test if an object with a malformed object cannot be created. Ensure that the object is not stored in the repository.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{ \"id\": \"123\", \"economyClass\": \"not-an-integer\" }";

        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        assertEquals(0, trainTypeRepository.count());
    }


    /*
     * Test if an object with a missing body cannot be created. Ensure that the object is not stored in the repository.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertEquals(0, trainTypeRepository.count());
    }

    // No special test cases for body validation, as the body is validated by the framework.
}
