package adminuser.component;

import adminuser.dto.UserDto;
import adminuser.entity.User;
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

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/adminuserservice/users endpoint.
 * This endpoint sends a POST request to ts-user-service to add a new user.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminUserTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private UserDto dto;

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
        dto = new UserDto("username", "password", 2, 1, "A1234", "email");
    }


    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case to verify the successful creation of a new user.
     * It mocks the response from ts-order-service to return a response with status 1 and the newly created user.
     * The test expects status OK, and the response to be equal to the mocked response.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        User user = new User(UUID.randomUUID(), dto.getUserName(), dto.getPassword(), dto.getGender(), dto.getDocumentType(), dto.getDocumentNum(), dto.getEmail());
        Response<User> expectedResponse = new Response<>(1, "REGISTER USER SUCCESS", user);
        mockServer.expect(MockRestRequestMatchers.requestTo("http://ts-user-service:12342/api/v1/userservice/users/register"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("userDto", dto);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, new TypeReference<Response<User>>(){}));
    }

    /*
     * Test case for invalid request where multiple users are included in the request body.
     * The test expects a client error status.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(dto);
        jsonArray.add(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for the handling of duplicate user creation attempts.
     * The mocked response has status 0 and message indicating that the user already exists.
     * The test expects status OK and the same response as the mocked response.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Response<Object> expectedResponse = new Response<>(0, "USER HAS ALREADY EXISTS", null);

        mockServer.expect(MockRestRequestMatchers.requestTo("http://ts-user-service:12342/api/v1/userservice/users/register"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("userDto", dto);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case for invalid request with malformed JSON object.
     * The test expects a bad request status.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{username: 'username', password: 'password'}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for invalid request with missing request body.
     * The test expects bad request status.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case for valid request when name attribute contain special characters.
     * The test expects status OK, because the string name attribute dos not have any limitations.
     */
    @Test
    void bodyVar_username_validTestCorrectLengthAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", "%%%%%");
        json.put("password", dto.getPassword());
        json.put("gender", dto.getGender());
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for valid request with a very long username.
     * The test expects status OK, again becaus e the string attribute does not have any limitations.
     */
    @Test
    void bodyVar_username_validTestStringLong() throws Exception {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String tooLongName = new String(chars);

        JSONObject json = new JSONObject();
        json.put("username", tooLongName);
        json.put("password", dto.getPassword());
        json.put("gender", dto.getGender());
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for valid request with null value for username attribute.
     * The test expects status OK, because in this service, it is not verified, if the value for the attribute is valid or not.
     * The JSON object containing all the attributes is immediately sent to ts-user-service.
     */
    @Test
    void bodyVar_username_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", null);
        json.put("password", dto.getPassword());
        json.put("gender", dto.getGender());
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for valid request with too high value for gender attribute.
     * The test expects status OK, because in this service, it is not verified, if the value for the attribute is valid or not, s long as it is the right type (int).
     * The JSON object containing all the attributes is immediately sent to ts-user-service.
     */
    @Test
    void bodyVar_gender_validTestValueTooHigh() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", dto.getUserName());
        json.put("password", dto.getPassword());
        json.put("gender", 4);
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for valid request with negative value for gender attribute.
     * The test expects status OK, because in this service, it is not verified, if the value for the attribute is valid or not, as long as it is the right type (int)
     * The JSON object containing all the attributes is immediately sent to ts-user-service.
     */
    @Test
    void bodyVar_gender_validTestValueLowOrNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", dto.getUserName());
        json.put("password", dto.getPassword());
        json.put("gender", -4);
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }



    /*
     * Test case for invalid request with a gender attribute with wrong variable type. The type of gender is an integer, but here it is a string.
     * The test expects a status bad request.
     */
    @Test
    void bodyVar_gender_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", dto.getUserName());
        json.put("password", dto.getPassword());
        json.put("gender", "shouldNotBeString");
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for valid request with null value for gender attribute.
     * The test expects status OK, because in this service, it is not verified, if the value for the attribute is valid or not, as long as it is the right type or null.
     * The JSON object containing all the attributes is immediately sent to ts-user-service.
     */
    @Test
    void bodyVar_gender_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", dto.getUserName());
        json.put("password", dto.getPassword());
        json.put("gender", null);
        json.put("documentType", dto.getDocumentType());
        json.put("documentNum", dto.getDocumentNum());
        json.put("email", dto.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminuserservice/users")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
