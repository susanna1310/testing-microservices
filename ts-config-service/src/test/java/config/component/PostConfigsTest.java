package config.component;

import com.alibaba.fastjson.JSONArray;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the POST /api/v1/configservice/configs endpoint.
 * This endpoint creates a new config object and saves it to the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostConfigsTest
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
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for creating a new config object with valid attributes.
     * This test verifies that a valid config can be successfully created and saved to the repository, when a config with that same name does not already exist.
     * It performs a POST request and checks that the config was correctly saved in the repository and that the response is equal to the expected response:
     * Response<>(1, "Create success", config)
     */
    @Test
    void validTestCorrectObject() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", config.getValue());
        json.put("description", config.getDescription());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Create success", config), JSONObject.parseObject(result, new TypeReference<Response<Config>>(){}));
        Assertions.assertEquals(configRepository.findAll().size(), 1);
    }

    /*
     * Test case for handling a request that attempts to send a Json with multiple config objects.
     * The test verifies that such a request is rejected with a client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for handling a request that attempts to create a duplicate config object, so a config with that same name already exists in the repository.
     * This test verifies that no new config object got created and that the response is equal to the expected response:
     * Response<>(0, "Config " + config.getName() + " already exists.", null)
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        configRepository.save(config);
        Config newConfig = new Config(config.getName(), "1.5", "newDescription");

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(newConfig)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String result = "Config " + newConfig.getName() + " already exists.";
        Assertions.assertEquals(new Response<>(0, result, null), JSONObject.parseObject(response, Response.class));
        Assertions.assertEquals(configRepository.findAll().size(), 1);
    }

    /*
     * Test case for handling a request with a malformed JSON object.
     * This test verifies that such a request is rejected with a Bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{name: name, value: 0.5, description: description}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for handling a request with a missing JSON object.
     * This test verifies that such a request is rejected with a Bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case for creating a config object with an empty string as the name.
     * This test verifies that it is allowed to create a config object with an empty name string.
     */
    @Test
    void bodyVarNameValidTestStringEmpty() throws Exception {
       config.setName("");
       mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isCreated());
    }

    /*
     * Test case for creating a config object with special characters in the name. This test verifies that it is allowed to create a config object with special characters.
     */
    @Test
    void bodyVarNameValidTestStringContainsSpecialCharacters() throws Exception {
        config.setName("%%%%%%%");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isCreated());
    }

    /*
     * Test case for creating a config object with a null name.
     * This test verifies that it is allowed to create a config with a null name, because the null variable gets converted into an empty string.
     * (which is allowed, as seen in test case above)
     */
    @Test
    void bodyVar_name_validTestStringIsNull() throws Exception {
        config.setName(null);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isCreated());
    }
}
