package auth.component;

import auth.dto.AuthDto;
import auth.exception.UserOperationException;
import auth.repository.UserRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/auth endpoint.
 * This endpoint gets a authDto, builds a user with the given attributes and saves it to the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAuthTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    private AuthDto authDto;

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
        userRepository.deleteAll();
        authDto = new AuthDto(UUID.randomUUID().toString(), "username", "password");
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request where a correct authDto object is posted.
     * The test expects a successful creation response with the authDto object
     */
    @Test
    void validTestCorrectObject() throws Exception {
        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSONObject.toJSONString(authDto)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        // get status 500, Internal Server Error, no idea why

        Assertions.assertEquals(new Response<>(1, "SUCCESS", authDto), JSONObject.parseObject(result, new TypeReference<Response<AuthDto>>(){}));
        Assertions.assertEquals(userRepository.findAll().size(), 1);
    }

    /*
     * Test case for an invalid request where multiple authDto objects are posted.
     * The test expects a client error response.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(authDto);
        jsonArray.add(authDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request with a malformed JSON object.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{username: username, password: password}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for an invalid request where the JSON object is missing.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case for an invalid request where the username is empty.
     * The test expects that an UserOperationException is thrown because the username cannot be empty,
     * and so status is Internal server error and the content is the message of the exception.
     */
    @Test
    void bodyVar_username_invalidTestEmpty() throws Exception {
        authDto = new AuthDto(UUID.randomUUID().toString(), "", "password");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSONObject.toJSONString(authDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[username cannot be empty.]"));
    }

    /*
     * Test case for an invalid request where the username is null.
     * The test expects a UserOperationException to be thrown, because the username cannot be null,
     * and so status is Internal server error and the content is the message of the exception.
     */
    @Test
    void bodyVar_username_invalidTestStringIsNull() throws Exception {
        authDto = new AuthDto(UUID.randomUUID().toString(), null, "password");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[username cannot be empty.]"));
    }

    /*
     * Test case for an invalid request where the password is too short.
     * The test expects a UserOperationException to be thrown, because shortest excepted length for password is 6
     * TEST FAILS
     * Response should contain message "Passwords must contain at least 6 characters.", password is empty
     * and checkUserCreateInfo(User user) method throws an UserOperationException with that message and status should be Internal server error.
     * But the actual response is with status created, no exception and message success.
     * Do not know why the user actually gets created, because it shouldn't
     */
    @Test
    void bodyVar_password_invalid_TestStringTooShort() throws Exception {
        authDto.setPassword("");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[Passwords must contain at least 6 characters.]"));
    }

    /*
     * Test case for an invalid request where the password is null.
     * The test expects an UserOperationException, because the password cannot be null.
     */
    @Test
    void bodyVar_password_invalid_TestStringNull()  {
        authDto = new AuthDto(UUID.randomUUID().toString(), "username", null);

        Assertions.assertThrows(NestedServletException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto))));
    }
}
