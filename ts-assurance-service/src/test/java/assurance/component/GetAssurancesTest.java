package assurance.component;



import assurance.entity.Assurance;
import assurance.entity.AssuranceType;
import assurance.entity.PlainAssurance;
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

import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/assuranceservice/assurances endpoint.
 * This endpoint retrieves all existing assurances in the repository.
 * The assurances are transformed into PlainAssurances and are then returned in an Arraylist.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAssurancesTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssuranceRepository assuranceRepository;
    private Assurance assurance;
    private Assurance assurance2;

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
        assurance2 = new Assurance(UUID.randomUUID(), UUID.randomUUID(), AssuranceType.TRAFFIC_ACCIDENT);
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for a valid request to retireve all assurances.
     * Two assurances are first saved to the repository and then two PlainAssurances are created out of the two assurances.
     * Then the GET request is sent, and the test verifies that the two PlainAssurances are contained in the arraylist that is contained in the response.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        assuranceRepository.save(assurance);
        assuranceRepository.save(assurance2);

        Assertions.assertEquals(assuranceRepository.findAll().size(), 2);

        PlainAssurance pa = new PlainAssurance(assurance.getId(), assurance.getOrderId(), assurance.getType().getIndex(), assurance.getType().getName(), assurance.getType().getPrice());
        PlainAssurance pa2 = new PlainAssurance(assurance2.getId(), assurance2.getOrderId(), assurance2.getType().getIndex(), assurance2.getType().getName(), assurance2.getType().getPrice());
        ArrayList<PlainAssurance> assurances = new ArrayList<>();
        assurances.add(pa);
        assurances.add(pa2);

        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<ArrayList<PlainAssurance>> re = JSONObject.parseObject(response, new TypeReference<Response<ArrayList<PlainAssurance>>>(){});

        Assertions.assertEquals(new Response<>(1, "Success", assurances), re);
        Assertions.assertEquals(re.getData().size(), 2);
        Assertions.assertTrue(re.getData().contains(pa));
        Assertions.assertTrue(re.getData().contains(pa2));
    }

    /*
     * Test case for a valid request where no assurances exist in the repository.
     * The test expects a response indicating that no content was found.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(assuranceRepository.findAll().size(), 0);
        Assertions.assertEquals(new Response<>(0, "No Content, Assurance is empty", null), JSONObject.parseObject(response, Response.class));
    }
}
