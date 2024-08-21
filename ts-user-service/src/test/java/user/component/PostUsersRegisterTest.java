package user.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import user.dto.AuthDto;
import user.entity.User;
import user.repository.UserRepository;


import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a UserDto object to save/register a new user in the repository. To do that, it also communicates
 * with the authentication service to create an authentication for the new user. As such we need to test the equivalence
 * classes for the attributes of the UserDto object. Because the service communicates with another service via RestTemplate,
 * we use MockRestServiceServer to mock the response of the external service. We also need to setup a MongoDBContainer
 * for the repository.
 * It is important to mention, that due to the fallback method in the controller having a different return type,
 * we can't perform a request on mockMVC that relies on mockServer for this endpoint. Not autowiring the restTemplate in the implementation
 * of the service, also causes an issue that we can't setup the MockRestServiceServer for external request. That is the
 * reason, why some test cases fail. This would be solved by changing the return type of the fallback method and either
 * autowiring the restTemplate via a bean or adding a getter or setter method for the restTemplate
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostUsersRegisterTest {

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

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
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        userRepository.deleteAll();
    }


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in creating the object
     * with the name in the repository. The documentNum, password, name are all Strings so every String is valid. The
     * gender and documentType are of type int, but they are not checked for specific values, so every int is valid as well.
     * The id has to be of type UUID.
     */
    @Test
    void validTestCorrectObject() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint

        Response<AuthDto> mockResponse1 = new Response<>(1, "Success", new AuthDto());
        URI uri = UriComponentsBuilder.fromUriString("http://ts-auth-service:12340/api/v1/auth").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
        UUID id = UUID.randomUUID();
        String requestJson = "{\"userId\":\""+ id + "\", \"userName\":\"name\", \"password\":\"password\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"me@gmail.com\"}";

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertNotNull(userRepository.findByUserId(id));
        assertEquals(new Response<>(1, "REGISTER USER SUCCESS", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this equivalence class we test a valid object, that already exists in the repository with that name. As such
     * the POST is expected to fail.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);
        String requestJson = "{\"userId\":\""+ UUID.randomUUID() + "\", \"userName\":\"name\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}";

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(userRepository.findByUserId(id));
        assertEquals("name", userRepository.findByUserId(id).getUserName());
        assertEquals("old", userRepository.findByUserId(id).getPassword());
        assertEquals("old", userRepository.findByUserId(id).getEmail());
        assertEquals("0", userRepository.findByUserId(id).getDocumentNum());
        assertEquals(0, userRepository.findByUserId(id).getGender());
        assertEquals(0, userRepository.findByUserId(id).getDocumentType());
        assertEquals(new Response<>(0, "USER HAS ALREADY EXISTS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one object in the JSON. This
     * is expected to cause as 4xx client error.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"userId\":\""+ id + "\", \"userName\":\"newName\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}, {\"userId\":\""+ id + "\", \"userName\":\"newName\", \"password\":\"newPassword\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"new@gmail.com\"}]";


        mockMvc.perform(post("/api/v1/userservice/users/register")
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

        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to post.
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/userservice/users/register")
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
     * the int attributes, which would normally not be a correct value. The expected response is that the object will
     * be created anyway. Even if the response of the external service were to fail, because it is not checked.
     */
    @Test
    void bodyVarTestValueNullOrOutOfRange() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        Response<AuthDto> mockResponse1 = new Response<>(1, "Success", new AuthDto());
        URI uri = UriComponentsBuilder.fromUriString("http://ts-auth-service:12340/api/v1/auth").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));



        //Actual request to the endpoint we want to test
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

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
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
