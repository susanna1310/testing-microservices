package security.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is for modifying a SecurityConfig object with the given attributes in the repository. It takes a SecurityConfig
 * object as body. As such we test equivalence classes for the input. It interacts only with the database, which is why
 * we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutSecurityConfigsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityRepository securityRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    void beforeEach() {
        securityRepository.deleteAll();
    }


	/*
	#####################################
	# Method (PUT) specific test cases #
	#####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes with the object id already existing in the
     * repository, which results in modifying input object in the repository.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setName("oldName");
        securityConfig.setValue("oldValue");
        securityConfig.setDescription("oldDec");
        securityRepository.save(securityConfig);

        String requestJson = "{\"id\":\"" + id + "\", \"name\":\"newName\", \"value\":\"newValue\", \"description\":\"newDec\"}";

        String result = mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findById(id));
        assertEquals("newName", securityRepository.findById(id).getName());
        assertEquals("newValue", securityRepository.findById(id).getValue());
        assertEquals("newDec", securityRepository.findById(id).getDescription());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));


    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"},{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}]";

        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * For this test, we do not insert the object of the body into the repository before the request. With the combination
     * of a valid attribute class and no insertion beforehand, we get a different response, because there is no object
     * with the id to update.
     */
    @Test
    void invalidTestNotExistingObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}";

        String result = mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNull(securityRepository.findById(id));
        assertEquals(0, securityRepository.findAll().size());
        assertEquals(new Response<>(0, "Security Config Not Exist", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc. as an equivalence class, which should not be able to be converted into the right object.
     * We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to post.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String requestJson = "";

        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * The name, value and description attributes of the object are Strings with no restriction, which means there is
     * only one equivalence classes with valid values, because they all lead to the same output. We tested that class
     * already above for these attributes. It is similar for the id attribute. Invalid values for all attributes were also tested.
     * Here we test null values for all attributes except id, because it could have lead to another outcome. Id can't be null,
     * because else it can't be saved in the repository.
     */
    @Test
    void bodyVarNameValueDescriptionValidTestNull() throws Exception {
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setName("oldName");
        securityConfig.setValue("oldValue");
        securityConfig.setDescription("oldDec");
        securityRepository.save(securityConfig);

        String requestJson = "{\"id\":\"" + id + "\", \"name\":null, \"value\":null, \"description\":null}";

        String result = mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findById(id));
        assertNull(securityRepository.findById(id).getName());
        assertNull(securityRepository.findById(id).getValue());
        assertNull(securityRepository.findById(id).getDescription());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));


    }
}
