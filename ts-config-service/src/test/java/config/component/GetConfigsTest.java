package config.component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import config.entity.Config;
import config.repository.ConfigRepository;
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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the GET /api/v1/configservice/configs endpoint.
 * This endpoint retrieves all configs contained in the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetConfigsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConfigRepository configRepository;
    private Config config;

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
        configRepository.deleteAll();
        config = new Config("name", "0.5", "description");
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for retrieving all config objects.
     * This test verifies that all saved config objects can be successfully retrieved.
     * It first saves multiple config objects, then performs a GET request and verifies that the response is equal to the expected response:
     * Response<>(1, "Find all  config success", configList), where configList is a list containing all the config objects.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        Config config2 = new Config("name2", "1.5", "description2");
        configRepository.save(config);
        configRepository.save(config2);

        List<Config> configList = new ArrayList<>();
        configList.add(config);
        configList.add(config2);

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<List<Config>> response = JSONObject.parseObject(result, new TypeReference<Response<List<Config>>>(){});
        Assertions.assertEquals(new Response<>(1, "Find all  config success", configList), response);
        Assertions.assertTrue(response.getData().contains(config));
        Assertions.assertTrue(response.getData().contains(config2));
    }

    /*
     * Test case for retrieving config objects when none exist.
     * The test verifies that a GET request returns a response equal to the expected response:
     * Response<>(0, "No content", null)
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "No content", null), JSONObject.parseObject(result, Response.class));
        Assertions.assertEquals(configRepository.findAll().size(), 0);
    }
}
