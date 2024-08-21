package user.component;

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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import user.entity.User;
import user.repository.UserRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs the User objects from the user repository with the given id. As this endpoint has no body or argument, there is
 * only a few equivalence classes to test. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetUserByUserIdTest {

    private ObjectMapper objectMapper = new ObjectMapper();

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
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * For the first equivalence class we want to get the object, which is why we insert the object before the request.
     * As such the combination of a valid input id and the insertion before, we reach the corresponding response. As the
     * id is of type UUID, it has to follow the UUID format to be valid.
     */
    @Test
    void validTestGetObject() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("new@gmail.com")
                .password("newPassword")
                .userId(id)
                .userName("name")
                .gender(1)
                .documentNum("1")
                .documentType(1).build();
        userRepository.save(user);

        String result = mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", id.toString())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        user = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), User.class);
        assertEquals("name", user.getUserName());
        assertEquals(id, user.getUserId());
        assertEquals("newPassword", user.getPassword());
        assertEquals("new@gmail.com", user.getEmail());
        assertEquals("1", user.getDocumentNum());
        assertEquals(1, user.getGender());
        assertEquals(1, user.getDocumentType());
        assertEquals(new Response<>(1, "Find User Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For the second equivalence class, we do not insert anything, so we don't get any name for the stationId, because the
     * repository is empty. This is the outcome as when we query with an id, that no object in the repository has.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        UUID id = UUID.randomUUID();

        String result = mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", id.toString())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No User", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to delete. This is the same outcome as when the
     * parameter is null.
     */
    @Test
    void invalidTestMissingBody() {
        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/userservice/users/id/{userId}")
        );});

    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * For this equivalence class we give an malformed id as the URL parameter, which should cause a 4xx client error.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", UUID.randomUUID() + "/" + UUID.randomUUID())
                )
                .andExpect(status().is4xxClientError());
    }



    /*
     * For the last equivalence class, we give an invalid id not of the UUID form to the endpoint. This should cause
     * an exception in the service layer.
     */
    @Test
    void invalidTestWrongCharacters() {
        assertThrows(NestedServletException.class, () -> {mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", "invalid id format")
        );});
    }
}
