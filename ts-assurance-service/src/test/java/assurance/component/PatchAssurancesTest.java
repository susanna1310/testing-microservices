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

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the PATCH /api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex} endpoint.
 * This endpoint modifies an assurance with the assuranceId and the updated values of orderId and typeIndex.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PatchAssurancesTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssuranceRepository assuranceRepository;
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
	#######################################
	# Method (PATCH) specific test cases #
	#######################################
	*/

    /*
     * Test case for a valid PATCH request to modify an existing assurance object.
     * The test expects a successful response indicating the assurance was modified correctly.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        assuranceRepository.save(assurance);
        Assertions.assertEquals(1, assuranceRepository.findAll().size());

        int newTypeIndex = 1;

        String response = mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex}", assurance.getId(), assurance.getOrderId(), newTypeIndex))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Assurance> re = JSONObject.parseObject(response, new TypeReference<Response<Assurance>>(){});
        Assertions.assertEquals(re.getStatus(), 1);
        Assertions.assertEquals(re.getMsg(), "Modify Success");
        Assertions.assertEquals(re.getData().getId(), assurance.getId());
        Assertions.assertEquals(re.getData().getOrderId(), assurance.getOrderId());
        Assertions.assertEquals(re.getData().getType(), AssuranceType.getTypeByIndex(newTypeIndex));

        Assurance updatesAssurance = assuranceRepository.findById(assurance.getId());
        Assertions.assertNotNull(updatesAssurance);
        Assertions.assertEquals(AssuranceType.getTypeByIndex(newTypeIndex), updatesAssurance.getType());
    }

    /*
     * Test case for an invalid PATCH request with a malformed object.
     * The test expects a client error status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/", 1/2/3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request with missing path variables.
     * The test expects an IllegalArgumentException to be thrown.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request with a non-existing assurance Id, so the assurance with the given Id does not exist in the repository.
     * The test expects a response indicating that the assurance was not found.
     */
    @Test
    void validTestNonexistingId() throws Exception {
        assuranceRepository.save(assurance);
        Assurance a = new Assurance(UUID.randomUUID(), UUID.randomUUID(), AssuranceType.getTypeByIndex(1));

        String response = mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex}", a.getId().toString(), a.getOrderId().toString(), a.getType().getIndex())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Fail.Assurance not found.", null), JSONObject.parseObject(response, Response.class));
        Assertions.assertEquals(1, assuranceRepository.findAll().size());
    }

    /*
     * Test case for a valid request with a non-existing assurance type index.
     * The test expects a response indicating that the given assurance type does not exist.
     */
    @Test
    void validTestNonexistingAssuranceType() throws Exception {
        assuranceRepository.save(assurance);
        String response = mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex}", assurance.getId().toString(), assurance.getOrderId().toString(), 3000)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Assurance Type not exist", null), JSONObject.parseObject(response, Response.class));
    }


    /*
     * Test case for an invalid request with special characters in the assurance Id.
     * The test expects a client error status, because the assurance Id is of the type UUID, and so can only consist of letters and numbers.
     */
    @Test
    void invalidTestWrongCharactersId() throws Exception {
        String invalidAssuranceId = "%/&)=(%+-/()=-)=/%-?=)(/&%ç";
        int validTypeIndex = 1;

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/assuranceservice/assurances/{assuranceId}/{orderId}/{typeIndex}", invalidAssuranceId, UUID.randomUUID().toString(), validTypeIndex)
                        .header("Content-Type", "application/json"))
                .andExpect(status().is4xxClientError());
    }
}
