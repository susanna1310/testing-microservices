package adminuser.component;

import adminuser.dto.UserDto;
import adminuser.entity.User;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for DELETE /api/v1/adminuserservice/users/{userId} endpoint.
 * This endpoint sends a DELETE request to ts-user-service to delete a user with the specified userId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminUserTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private User user;

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }
    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        user = new User(UUID.randomUUID(), "username", "password", 2, 1, "A1234", "email");
    }


    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Test case for valid request with a valid user Id.
     * The response of ts-user-service is mocked with status 1, to simulate that the user would be deleted successfully.
     * The test expects an OK status and expects the same response as the mocked response from user service.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Object> expectedResponse = new Response<>(1, "DELETE SUCCESS", null);
        mockServer.expect(requestTo("http://ts-user-service:12342/api/v1/userservice/users/" + user.getUserId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}", user.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case for valid request when attempting to delete multiple users simultaneously, by sending a request with multiple path variables.
     * The test expects a status OK, because the first parameter will be used, and the second one is ignored.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userid}", user.getUserId(), 1)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk());
    }

    /*
     * Test case for an invalid request when sending a request with a malformed JSON object.
     * The test expects a client error status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}", "1/2")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for request with missing userId path variable.
     * The test expects that an IllegalArgumentException is thrown.
     */
    @Test
    void invalidTestMissingObject()  {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}")));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test case for a request with a non-existing user id, so the user with the specified user id does not exist.
     * The response of ts-user-service is mocked to simulate that the user does not exist with status 0.
     * The test expects a status OK and the response equal to the mocked response.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        Response<Object> expectedResponse = new Response<>(0, "USER NOT EXISTS", null);
        mockServer.expect(requestTo("http://ts-user-service:12342/api/v1/userservice/users/" + user.getUserId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}", user.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
}

    /*
     * Test case for a request with an userId with incorrect format (Integer instead of UUID).
     * The test expects a status OK, because the int is casted to a string and appended to the url.
     */
    @Test
    void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        int nonCorrectFormat = 1;
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}", nonCorrectFormat)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk());
    }

    /*
     * Test case for a request with an userId containing special characters.
     * The test expects a status not found, because the userId is of type UUID, which only consists of letters and numbers.
     */
    @Test
    void validTestWrongCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminuserservice/users/{userId}", "&/%()/&&")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isNotFound());
    }
}
