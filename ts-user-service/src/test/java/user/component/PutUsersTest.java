package user.component;

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
import user.entity.User;
import user.repository.UserRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint updates a User object by creating a new one and replacing it in the repository. It gets a UserDto object as
 * the body and uses its parameters to create the User object (except for the id). As such we test equivalence class tests for the
 * attributes of the object. It interacts only with the database, which is why we need to setup a MongoDBContainer for
 * the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutUsersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        userRepository.deleteAll();
    }

	/*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in updating the object
     * with the name in the repository. The documentNum, password, name are all Strings so every String is valid. The
     * gender and documentType are of type int, but they are not checked for specific values, so every int is valid as well.
     * The id has to be of type UUID.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);
        String requestJson = "{\"userId\":\""+ id + "\", \"userName\":\"name\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}";

        String result = mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(userRepository.findByUserId(id));
        assertEquals("name", userRepository.findByUserId(id).getUserName());
        assertEquals("newPassword", userRepository.findByUserId(id).getPassword());
        assertEquals("new@gmail.com", userRepository.findByUserId(id).getEmail());
        assertEquals("1", userRepository.findByUserId(id).getDocumentNum());
        assertEquals(1, userRepository.findByUserId(id).getGender());
        assertEquals(1, userRepository.findByUserId(id).getDocumentType());
        assertEquals(new Response<>(1, "SAVE USER SUCCESS", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the case of a valid object, which does not exist in the repository with the name. As a result the
     * update should fail and return the response with status code 0. We do this by updating the object but with a new
     * name but same id. This fails, because the repository is searched by unique names. This is in the same equivalence
     * class as if the repository was empty.
     */
    @Test
    void validTestObjectNonExisting() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);
        String requestJson = "{\"userId\":\""+ id + "\", \"userName\":\"newName\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}";

        String result = mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(userRepository.findByUserId(id));
        assertNotEquals("newName", userRepository.findByUserId(id).getUserName());
        assertNotEquals("newPassword", userRepository.findByUserId(id).getPassword());
        assertNotEquals("new@gmail.com", userRepository.findByUserId(id).getEmail());
        assertNotEquals("1", userRepository.findByUserId(id).getDocumentNum());
        assertNotEquals(1, userRepository.findByUserId(id).getGender());
        assertNotEquals(1, userRepository.findByUserId(id).getDocumentType());
        assertEquals(new Response<>(0, "USER NOT EXISTS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"userId\":\""+ id + "\", \"userName\":\"newName\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}, {\"userId\":\""+ id + "\", \"userName\":\"newName\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}]";


        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we test the case, when the input JSON is malformed in any way, in other words if the object has too many attributes,
     * wrong attribute types etc,which should not be able to be converted into the right object. We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"userId\":\"notAnId\", \"userName\":1\", \"password\":wrong\", \"gender\":invalid, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}";

        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to update.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(put("/api/v1/userservice/users")
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
     * For the last equivalence class we test null values for the String attributes except the name and unusual int for
     * the int attributes, which would normally not be a correct value. The expected response is that every value
     * except the id will be updated.
     */
    @Test
    void bodyVarTestValueNullOrOutOfRange() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);
        String requestJson = "{\"userId\":null, \"userName\":\"name\", \"password\":null, \"gender\":" + Integer.MIN_VALUE + ", \"documentType\":" + Integer.MIN_VALUE + ", \"documentNum\":null, \"email\":null}";

        String result = mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(userRepository.findByUserId(id));
        assertNotEquals(null, userRepository.findByUserId(id).getUserId());
        assertEquals("name", userRepository.findByUserId(id).getUserName());
        assertEquals(null, userRepository.findByUserId(id).getPassword());
        assertEquals(null, userRepository.findByUserId(id).getEmail());
        assertEquals(null, userRepository.findByUserId(id).getDocumentNum());
        assertEquals(Integer.MIN_VALUE, userRepository.findByUserId(id).getGender());
        assertEquals(Integer.MIN_VALUE, userRepository.findByUserId(id).getDocumentType());
        assertEquals(new Response<>(1, "SAVE USER SUCCESS", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }
}
