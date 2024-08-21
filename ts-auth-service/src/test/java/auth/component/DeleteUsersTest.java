package auth.component;

import auth.entity.User;
import auth.repository.UserRepository;
import com.alibaba.fastjson.JSONObject;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/users/{userId} endpoint.
 * This endpoint deletes a user from the repository with the userId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteUsersTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    private User admin;
    private User user;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }
    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        admin = new User(UUID.randomUUID(), "admin", "passwordAdmin", new HashSet<>(Arrays.asList("ROLE_ADMIN")));
        user = new User(UUID.randomUUID(), "user", "passwordUser", new HashSet<>(Arrays.asList("ROLE_USER")));
    }


    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case for a valid request where the user with the given userId exists in the repository.
     * The test expects a successful deletion and the response:
     * Response(1, "DELETE USER SUCCESS", null)
     * The test also verifies, that the user actually got deleted in the repository, so that the size got down by 1.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        userRepository.save(user);
        userRepository.save(admin);

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}", user.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<Object> response = JSONObject.parseObject(result, Response.class);

        Assertions.assertEquals(new Response(1, "DELETE USER SUCCESS", null), response);
        Assertions.assertEquals(userRepository.findAll().size(), 1);
        Assertions.assertTrue(userRepository.findAll().contains(admin));
    }

    /*
     * Test case for a valid request where multiple user Ids are provided.
     * The test is valid, because only the first provided is used and the second one is ignored.
     * The test expects a client error response.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}", 1, 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for an invalid request with a malformed user Id.
     * The test expects a not found response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}", "1/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /*
     * Test case for an invalid request with a missing user Id.
     * The test expects an IllegalArgumentException to be thrown due to the missing path variable.
     */
    @Test
    void invalidTestMissingObject() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}")));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request with a non existing user Id, so the user does not exist in the repository.
     * The test verifies that no users are deleted and expects a successful response:
     * Response(1, "DELETE USER SUCCESS", null), because it is not checked if the user even was contained in the repository before deletion method or not.
     */
    @Test
    void validTestNonexistingId() throws Exception {
        userRepository.save(user);
        userRepository.save(admin);

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<Object> response = JSONObject.parseObject(result, Response.class);

        // No response for status 0, always status 1
        Assertions.assertEquals(new Response(1, "DELETE USER SUCCESS", null), response);
        Assertions.assertEquals(userRepository.findAll().size(), 2);
        Assertions.assertTrue(userRepository.findAll().contains(admin));
        Assertions.assertTrue(userRepository.findAll().contains(user));
    }

    /*
     * test case for an invalid request with a user ID containing special characters.
     * The test expects a not found response, because the userId entity is of the type UUID which has the requirement that it only consists of letters and numbers.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{userId}", "+)*(&*=)-+&=?-ç&*?-+%&/-)(/&%ç+=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
