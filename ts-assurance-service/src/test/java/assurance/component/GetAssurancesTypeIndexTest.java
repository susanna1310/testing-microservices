package assurance.component;

import assurance.entity.Assurance;
import assurance.entity.AssuranceType;
import assurance.repository.AssuranceRepository;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/assuranceservice/assurances/{typeIndex}/{orderId} endpoint.
 * If the assurance does not already exist, this endpoint creates a new Assurance with the provided orderId and typeIndex and saves it to the repository.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAssurancesTypeIndexTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssuranceRepository assuranceRepository;
    private ObjectMapper mapper = new ObjectMapper();
    private Assurance assurance;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);


    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }
    @BeforeEach
    public void setup() {
        assuranceRepository.deleteAll();
        assurance = new Assurance(UUID.randomUUID(), UUID.randomUUID(), AssuranceType.getTypeByIndex(1));
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for a valid request to create a new (non-existing) assurance with the given typeIndex and orderId.
     * The test expects a successful response indicating that the assurance was created and stored in the repository.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Assertions.assertEquals(assuranceRepository.findAll().size(), 0);

        String orderId = UUID.randomUUID().toString();
        int typeIndex = 1;

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", typeIndex, orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assurance createdAssurance = new Assurance(UUID.randomUUID(), UUID.fromString(orderId), AssuranceType.getTypeByIndex(typeIndex));

        Response<Assurance> re = JSONObject.parseObject(response, new TypeReference<Response<Assurance>>(){});
        Assertions.assertEquals(re.getStatus(), 1);
        Assertions.assertEquals(re.getMsg(), "Success");
        Assertions.assertEquals(re.getData().getType().getIndex(), typeIndex);
        Assertions.assertEquals(assuranceRepository.findAll().size(), 1);
    }

    /*
     * Test case for a valid request to create an already existing assurance for the given typeIndex and orderId.
     * The test expects a response indicating that the assurance already exists.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        assuranceRepository.save(assurance);

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", assurance.getType().getIndex(), assurance.getOrderId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Fail.Assurance already exists", null), JSONObject.parseObject(response, Response.class));
        Assertions.assertEquals(assuranceRepository.findAll().size(), 1);
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for an invalid request where the orderId path variable is missing.
     * The test expects an IllegalArgumentException to be thrown.
     */
    @Test
    void invalidTestNonexistingOrderId() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", assurance.getType().getIndex())));
    }

    /*
     * Test case for a valid request where the provided typeIndex does not correspond to any assurance type.
     * The test expects a response indicating that the assurance type does not exist.
     */
    @Test
    void validTestNonexistingTypeIndex() throws Exception {
        int typeIndex = 3000;

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", typeIndex, assurance.getOrderId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Fail.Assurance type doesn't exist", null), JSONObject.parseObject(response, Response.class));
        Assertions.assertEquals(assuranceRepository.findAll().size(), 0);
    }
}
