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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the DELETE /api/v1/configservice/configs/{configName} endpoint.
 * This endpoint deletes a config with the given name.
 * The test class is divided into DELETE specific test cases and URL parameter specific test cases.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteConfigsTest
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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case for deleting a valid config object.
     * This test verifies that the config object gets successfully deleted.
     * It first saves a config object to the repository, then performs a DELETE request and
     * checks that the response is equal to the expected response:
     * Response<>(1, "Delete success", config)
     */
    @Test
    void validTestCorrectObject() throws Exception {
        configRepository.save(config);
        Assertions.assertEquals(1, configRepository.findAll().size());

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}", config.getName())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(0, configRepository.findAll().size());
        Assertions.assertEquals(new Response<>(1, "Delete success", config), JSONObject.parseObject(result, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Test case for deleting a config object with an invalid request containing multiple path variables.
     * This test verifies that the request returns an OK status, because only the first url parameter is used.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        Config config1 = new Config("name1", "value", "description");
        Config config2 = new Config("name2", "value2", "description2");
        configRepository.save(config1);
        configRepository.save(config2);

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}","name1", "name2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Delete success", config1), JSONObject.parseObject(result, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Test case for deleting a config object with a malformed path variable.
     * This test verifies that the request returns a client error status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}", "name/name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for deleting a config object with a missing path variable.
     * This test verifies that the request throws an IllegalArgumentException.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for deleting a config object with a non-existing name.
     * This test verifies that, when a config, that is not contained in the repository, is tried to get deleted, the response is equal to the expected response:
     * Response<>(0, "Config name doesn't exist", null).
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}", config.getName())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(0, "Config name doesn't exist", null), JSONObject.parseObject(result, new TypeReference<Response<Config>>(){}));
    }

    /*
     * Test case for deleting a config object with a name containing special characters.
     * The test verifies that the request returns an OK status, as the name variable is a String and does not have special requirements.
     */
    @Test
    void validTestSpecialCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/configservice/configs/{configName}", "%%%%%")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
