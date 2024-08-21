package security.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
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
 * This endpoint is for deleting a SecurityConfig object with the given id in the repository, regardless if it exists.
 * It takes an id as a URL parameter. As such we test equivalence classes for the input. It interacts only with the database, which is why
 * we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteSecurityConfigsTest {

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * The first equivalence class test is for a valid id which exists in the repository, which results in deleting the
     * object with the id in the repository.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);

        securityRepository.save(securityConfig);

        String result = mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNull(securityRepository.findById(id));
        assertEquals(0, securityRepository.findAll().size());
        assertEquals(new Response<>(1, "Success", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For this input class we test the case when we give the endpoint request more than one id as the URL parameter. This
     * does not cause an error, because it only takes the first value. This equivalence class test is still important,
     * because it highlights the difference to endpoints which take body object json, which causes an error if it is too
     * many objects.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityRepository.save(securityConfig);

        String result =mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id, UUID.randomUUID().toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(0, securityRepository.findAll().size());
        assertEquals(new Response<>(1, "Success", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we test the case, when the URL parameter is malformed in any way, in other words it has wrong characters, is a
     * wrong attribute type etc. as an equivalence class, which should not be able to be converted into the right type.
     * We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", UUID.randomUUID() + "/" + UUID.randomUUID())
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to delete. No input causes an exception.
     */
    @Test
    void invalidTestMissingObject() {
        assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", "not a correct format id")
        );});
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * The id is normally of type UUID but is a String as a URL parameter. We already tested valid values for the UUID
     * in some tests above when the id also existed in the repository. Now we test the equivalence class from id, where
     * it is valid but does not exist in the repository.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        UUID id = UUID.randomUUID();

        String result = mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNull(securityRepository.findById(id));
        assertEquals(0, securityRepository.findAll().size());
        assertEquals(new Response<>(1, "Success", id.toString()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * In the last equivalence class for the id, we give an id, which is not the correct format for an id. As it only
     * gets converted into an UUID object in the logic layer, this class of inputs will cause an exception.
     */
    @Test
    void invalidTestNonCorrectFormatId() {
        assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", "not a correct format id")
                );});
    }

}
