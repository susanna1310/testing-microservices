package assurance.component;

import assurance.entity.Assurance;
import assurance.entity.AssuranceType;
import assurance.entity.AssuranceTypeBean;
import assurance.repository.AssuranceRepository;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/assuranceservice/assurances/types endpoint.
 * The endpoint creates AssuranceTypeBeans for every AssuranceType that exists and saves it to an ArrayList.
 * When the GET request is sent, the Arraylist is returned containing all the existing Assurance Types.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAssurancesTypesTest
{
    @Autowired
    private MockMvc mockMvc;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);


    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for a valid request to retrieve all assurance types.
     * The test expects a successful response indicating that all assurance types were found and
     * verifies that the assurance type beans in the returned arraylist are as expected.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        AssuranceTypeBean assuranceTypeBean = new AssuranceTypeBean(AssuranceType.TRAFFIC_ACCIDENT.getIndex(), AssuranceType.TRAFFIC_ACCIDENT.getName(), AssuranceType.TRAFFIC_ACCIDENT.getPrice());
        List<AssuranceTypeBean> assuranceTypeList = new ArrayList<>();
        assuranceTypeList.add(assuranceTypeBean);

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/types")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<List<AssuranceTypeBean>> response = JSONObject.parseObject(result, new TypeReference<Response<List<AssuranceTypeBean>>>(){});

        Assertions.assertEquals(new Response<>(1, "Find All Assurance", assuranceTypeList), response);
        Assertions.assertEquals(response.getData().size(), 1);
        Assertions.assertEquals(response.getData(), assuranceTypeList);
        Assertions.assertTrue(response.getData().contains(assuranceTypeBean));
    }

    /*
     * Test case for an invalid request where the assurance type list cannot be empty.
     * One assurance type is already predefined, so the assurance type list can never be empty.
     * That means that the response can never have status 0.
     */
    @Test
    void invalidTestGetZeroObjects() throws Exception {
        // Invalid Test because one assurance type is predefined, so assurance type list cannot be empty
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurances/types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<List<AssuranceTypeBean>> response = JSONObject.parseObject(result, new TypeReference<Response<List<AssuranceTypeBean>>>(){});

        Assertions.assertNotEquals(response.getStatus(), 0);
        Assertions.assertNotEquals(response.getMsg(), "Assurance is Empty");
        Assertions.assertNotEquals(response.getData(), null);
    }
}
