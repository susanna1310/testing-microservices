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
import security.repository.SecurityRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is for creating a SecurityConfig object with the given attributes in the repository. It takes a SecurityConfig
 * object as body. As such we test equivalence classes for the input. It interacts only with the database, which is why
 * we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostSecurityConfigsTest {

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
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in creating and saving the
     * input object in the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}";

        String result = mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findByName("name"));
        assertNotEquals(id, securityRepository.findByName("name").getId());
        assertEquals("1", securityRepository.findByName("name").getValue());
        assertEquals("sec", securityRepository.findByName("name").getDescription());
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

        mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * For this test, we already insert the object of the body into the repository before the request. With the combination
     * of a valid attribute class and the insertion beforehand, we get a different response. There can't be two
     * securityConfig objects with the same name in the repository, so we change every attribute except the name
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}";

        String result = mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findByName("name"));
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

        id = UUID.randomUUID();
        assertEquals(1, securityRepository.findAll().size());
        requestJson = "{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"2\", \"description\":\"sec2\"}";

        result = mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findByName("name"));
        assertEquals(1, securityRepository.findAll().size());
        assertEquals(new Response<>(0, "Security Config Already Exist", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc. as an equivalence class, which should not be able to be converted into the right object.
     * We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
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

        mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
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
     * already above for these attributes. It is similar for the id attribute. Wrong values for all attributes were also tested.
     * Here we test null values for all attributes, because it could have lead to another outcome
     */
    @Test
    void bodyVarIdNameValueDescriptionValidTestNull() throws Exception {
        String requestJson = "{\"id\":null, \"name\":null, \"value\":null, \"description\":null}";

        String result = mockMvc.perform(post("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(securityRepository.findByName(null));
        assertEquals(1, securityRepository.findAll().size());
        assertNotEquals(null, securityRepository.findByName(null).getId());
        assertNull(securityRepository.findByName(null).getValue());
        assertNull(securityRepository.findByName(null).getDescription());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));


    }
}
