package adminuser.component;

import adminuser.entity.User;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.Assert;
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
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/adminuserservice/users endpoint.
 * This endpoint send a GET request to ts-user-service to retrieve a list with all existing users.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetAdminUserTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());

    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }


    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case with a valid request to retrieve all users. The mocked response from ts-user-service returns an arraylist with all users.
     * The test expects a response with status OK, response equal to the mocked response
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        User user = new User(UUID.randomUUID(), "username", "password", 2, 1, "A1234", "email");
        User user2 = new User(UUID.randomUUID(), "username2", "password2", 2, 2, "B1234", "email2");

        List<User> users = new ArrayList<>();
        users.add(user);
        users.add(user2);

        Response<List<User>> expectedResponse = new Response<>(1, "Success", users);
        mockServer.expect(requestTo("http://ts-user-service:12342/api/v1/userservice/users"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<List<User>> re = JSONObject.parseObject(actualResponse, new TypeReference<Response<List<User>>>(){});
        Assertions.assertEquals(expectedResponse, re);
        Assertions.assertEquals(2, re.getData().size());
    }

    /*
     * Test case for valid request to retrieve all users, when no users exist.
     * The test expects the ts-user-service to return a mocked response with status 0 and a massage indicating no user.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        Response<Object> expectedResponse = new Response<>(0, "NO User", null);
        mockServer.expect(requestTo("http://ts-user-service:12342/api/v1/userservice/users"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Response<Object> re = JSONObject.parseObject(actualResponse, Response.class);
        Assertions.assertEquals(expectedResponse, re);
    }
}
