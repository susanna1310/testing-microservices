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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for the PUT /api/v1/configservice/configs endpoint.
 * Endpoint updates an already existing config with the new attributes.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutConfigsTest
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
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Test case for updating an existing config object with valid data.
     * First a config is saved in the repository, then a new config is created with the same name and a PUT request is executed.
     * The test verifies that the config with the updated attributes is contained in the repository and that the response is equal to the expected response:
     * Response<>(1, "Update success", newConfig)
     */
    @Test
    void validTestCorrectObject() throws Exception {
        configRepository.save(config);
        Config newConfig = new Config(config.getName(), "1.5", "updatedDescription");

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", "1.5");
        json.put("description", "updatedDescription");

        String response = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toJSONString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Update success", newConfig), JSONObject.parseObject(response, new TypeReference<Response<Config>>(){}));
        Assertions.assertTrue(configRepository.findAll().contains(newConfig));
    }

    /*
     * Test case for verifying that the attributes are correctly updated.
    */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        configRepository.save(config);
        Config newConfig = new Config(config.getName(), "1.5", "updateDescription");

        JSONObject json = new JSONObject();
        json.put("name", config.getName());
        json.put("value", "1.5");
        json.put("description", "updatedDescription");

        String response = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Config> re = JSONObject.parseObject(response, new TypeReference<Response<Config>>(){});
        Assertions.assertEquals(newConfig.getValue(), re.getData().getValue());
        Assertions.assertEquals(newConfig.getDescription(), re.getData().getDescription());
    }

    /*
     * Test case for attempt to update multiple configs in a single request.
     * The test expects a client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for attempt to update with a malformed JSON object.
     * The test expects a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{name: name, value: 0.5, description: description}";
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for attempt to update without providing a JSON object.
     * The test expects a bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case when updating a config with an empty string as name.
     * The test expects an OK status, because there is no restriction for the string length of the name field.
     */
    @Test
    void bodyVarNameValidTestStringShort() throws Exception {
        config.setName("");
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isOk());
    }

    /*
     * Test case for updating a config with a name containing special characters.
     * The test expects an OK status, because the name field is a String and has no restriction in what characters should be used.
     */
    @Test
    void bodyVarNameValidTestStringContainsSpecialCharacters() throws Exception {
        config.setName("%%%%%%%");
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isOk());
    }

    /*
     * Test case for updating a config with a null value for the name.
     * The test expects an OK status, because the null String field gets converted into an empty string and so is a valid.
     * (As seen in the other test case)
     */
    @Test
    void bodyVar_name_invalidTestStringIsNull() throws Exception {
        config.setName(null);
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(config)))
                .andExpect(status().isOk());
    }
}
