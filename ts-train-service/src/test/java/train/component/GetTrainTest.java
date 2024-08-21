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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This is a test class for the GET endpoint of the Train Service to get one specific traintype.
 * (/api/v1/trainservice/trains/{id})
 * It is used to test if a train type can be deleted.
 * It interacts only with the database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetTrainTest {
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
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Retrieve a train type with a valid id. Ensure that the train type is retrieved from the repository.
     */
    @Test
    void validTestExistingId() throws Exception {
        TrainType trainType = new TrainType("123", 100, 50);
        trainTypeRepository.save(trainType);

        String result = mockMvc.perform(get("/api/v1/trainservice/trains/123"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response<TrainType> expectedResponse = new Response<>(1, "success", trainType);
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        Response<TrainType> actualResponse = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, TrainType.class));

        assertEquals(expectedResponse, actualResponse);
    }

    /*
     * Try to retrieve a train type with a non-existing id. Ensure that the correct message is returned.
     */
    @Test
    void invalidTestNonExistingId() throws Exception {
        String result = mockMvc.perform(get("/api/v1/trainservice/trains/999"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "here is no TrainType with the trainType id: 999", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Try to retrieve a train type with an id that is not in the correct format. Ensure that the correct status is returned.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        String result = mockMvc.perform(get("/api/v1/trainservice/trains/abc123"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "here is no TrainType with the trainType id: abc123", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Try to retrieve a train type with an id that contains characters that are not allowed. Ensure that the correct status is returned.
     * */
    @Test
    void validTestWrongCharacters() throws Exception {
        String result = mockMvc.perform(get("/api/v1/trainservice/trains/!@#"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Response expectedResponse = new Response(0, "here is no TrainType with the trainType id: !@#", null);
        assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
    }
}
