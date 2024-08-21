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

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/assuranceservice/assurance/orderid/{orderId} endpoint.
 * This endpoint retrieves an existing assurance from the repository with the given orderId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAssurancesOrderIdTest
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
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for a valid request where the existing assurance with the specific order Id is retrieved.
     * The test expects a response containing the found assurance.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        assuranceRepository.save(assurance);

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", assurance.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Assurance> response = JSONObject.parseObject(result, new TypeReference<Response<Assurance>>(){});

        Assertions.assertEquals(new Response<>(1, "Find Assurace Success", assurance), response);
        Assertions.assertEquals(response.getData().getId(), assurance.getId());
        Assertions.assertEquals(response.getData().getOrderId(), assurance.getOrderId());
        Assertions.assertEquals(response.getData().getType(), assurance.getType());
    }

    /*
     * Test case for a valid request where no assurances with the given orderId exist.
     * The test expects a response indicating that no content was found.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", assurance.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);

        Assertions.assertEquals(new Response<>(0, "No Content by this orderId", null), response);
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for an invalid request where the order Id contains special characters.
     * The test expects a client error response, because the orderId is of type UUID, and so can only consist of letters and numbers.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        String wrongCharactersId = "%&/()=?*-)(/&-+*ç%-?=)(/&%ç";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", wrongCharactersId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
