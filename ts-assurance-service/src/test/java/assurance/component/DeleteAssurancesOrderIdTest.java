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

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/*
 * Test class for DELETE /api/v1/assuranceservice/assurances/orderid/{orderId} endpoint.
 * The endpoint deletes an existing assurance from the repository with the given orderId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAssurancesOrderIdTest
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
     * Test case for a valid request where an existing order Id of an existing order in the repository is provided.
     * The test expects a successful deletion response:
     * Response<>(1, "Delete Success with Order Id", null)
     */
    @Test
    void validTestCorrectObject() throws Exception {
        assuranceRepository.save(assurance);
        Assertions.assertEquals(1, assuranceRepository.findAll().size());

        String response = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", assurance.getOrderId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertNull(assuranceRepository.findByOrderId(assurance.getOrderId()));
        Assertions.assertEquals(0, assuranceRepository.findAll().size());
        Assertions.assertEquals(new Response<>(1, "Delete Success with Order Id", null), JSONObject.parseObject(response, Response.class));
    }

    /*
     * Test case for a valid request where multiple order Ids are provided.
     * The test expects a status OK, because only the first orderId gets used and the second one gets ignored.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", 1, 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for an invalid request with a malformed orderId.
     * The est expects a client error response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid", 1/2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request where the orderId path variable is missing.
     * The test expects an IllegalArgumentException to be thrown.
     */
    @Test
    void invalidTestMissingObject()  {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid/{orderId}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Same as non existing test case in DeleteAssuranceAssuranceIdTest class.
     * Test case for an invalid request where a non-existing order Id is provided.
     * As the assurance is tried t be deleted from the repository without a check if it even exists or not,
     * the assurance is null afterwards if it existed before or not.
     * So the case that a response with status 0 is returned never occurs, but response with status 1 is always returned.
     * But the response should not have status 1 in this case, so the Test fails.
     */
    // TEST FAILS
    @Test
    void invalidTestNonexistingOrderId() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", assurance.getOrderId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertNotEquals(new Response<>(1, "Delete Success with Order Id", null), JSONObject.parseObject(response, Response.class));
    }

    /*
     * Test case for an invalid request where the order ID contains special characters.
     * The test expects a client error response, because the orderId attribute is of type UUID and so has the requirement that it only consists of letters and numbers.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", "+)*(&*=)-+&=?-ç&*?-+%&/-)(/&%ç+=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
