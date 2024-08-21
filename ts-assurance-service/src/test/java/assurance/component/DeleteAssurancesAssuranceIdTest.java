package assurance.component;

import assurance.entity.Assurance;
import assurance.entity.AssuranceType;
import assurance.repository.AssuranceRepository;
import com.alibaba.fastjson.JSONObject;
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
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/assuranceservice/assurances/assuranceid/{assuranceId} endpoint
 * This endpoint deletes an assurance from the repository with the given assurance Id.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAssurancesAssuranceIdTest
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
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case for a valid request where a correct assurance Id is porvided and the assurance with that Id is contained in the repository.
     * The test expects a successful deletion response, equal to Response<>(1, "Delete Success with Assurance id", null)
     * And the test verifies that the assurance actually got deleted in the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        assuranceRepository.save(assurance);
        Assertions.assertEquals(1, assuranceRepository.findAll().size());

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId}", assurance.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertNull(assuranceRepository.findById(assurance.getId()));
        Assertions.assertEquals(0, assuranceRepository.findAll().size());
        Assertions.assertEquals(new Response<>(1, "Delete Success with Assurance id", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case for a valid request where multiple assurance Ids are provided.
     * The test expects a status OK because only the first Id gets used and the second one gets ignored.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId}", 1, 2)
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for an invalid request with a malformed assurance Id.
     * The test expects a client error response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId}", "1/2")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request where the assurance Id path variable is missing.
     * The test expects that an IllegalArgumentException is thrown.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for a request where a non-existing assurance Id is provided, so the assurance with the given id is not contained in the repository.
     * The Test fails, because before anything is checked, the assurance is deleted from the repository, even if it's a non-existing id.
     * So when the assurance existed before, it is now null, but when the assurance did not exist before it is also null.
     * The response always has status 1, but we expect response status 0, but this case will never be true.
     * That's why the test fails.
     */

    // TEST FAILS
    @Test
    void invalidTestNonexistingId() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId}", assurance.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertNotEquals(new Response<>(1, "Delete Success with Assurance id", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case for an invalid request where the assurance ID contains special characters.
     * The test expects a client error response, because the assuranceId attribute is of type UUID and so has the requirement that it only consists of letters and numbers.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/assuranceid/{assuranceId}", "+)*(&*=)-+&=?-ç&*?-+%&/-)(/&%ç+=")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
