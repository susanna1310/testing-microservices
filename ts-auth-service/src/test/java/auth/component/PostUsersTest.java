package auth.component;

import auth.dto.BasicAuthDto;
import auth.dto.TokenDto;
import auth.entity.User;
import auth.repository.UserRepository;
import auth.security.jwt.JWTProvider;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/users/login endpoint.
 * The endpoint expects a basicAuthDto as input body.
 * To make it simpler, we send the basicAuthDto with an empty verificationCode, expect in the test case where we want to test the verification code.
 * We mock the authenticationManager, to simulate different scenarios.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostUsersTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private JWTProvider jwtProvider;
    @MockBean
    private AuthenticationManager authenticationManager;

    private BasicAuthDto basicAuthDto;
    private User user;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();

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
        mockServer = MockRestServiceServer.createServer(restTemplate);
        userRepository.deleteAll();
        basicAuthDto = new BasicAuthDto("username", "password", "verificationCode");
        user = new User(UUID.randomUUID(), basicAuthDto.getUsername(), basicAuthDto.getPassword(), new HashSet<>(Arrays.asList("ROLE_USER")));
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request were a correct BasicAuthDto object is posted.
     * The verificationCode attribute is empty, so we don't have to mock the response from ts-verification-code-service.
     * The authenticationManager is mocked and we expect a valid Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()); to be returned
     * The test expects a successful login response with a TokenDto object.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername(user.getUsername());
        basicAuthDto.setPassword(user.getPassword());

        userRepository.save(user);

        Assertions.assertTrue(StringUtils.isEmpty(basicAuthDto.getVerificationCode()));

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockServer.verify();
        Response<TokenDto> response = JSONObject.parseObject(result, new TypeReference<Response<TokenDto>>(){});

        Assertions.assertEquals(response.getStatus(), 1);
        Assertions.assertEquals(response.getMsg(), "login success");
        Assertions.assertEquals(response.getData().getUserId(), user.getUserId());
        Assertions.assertEquals(response.getData().getUsername(), user.getUsername());
    }

    /*
     * Test case for an invalid request where multiple BasicAuthDto objects are posted.
     * The test expects a client error response.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(basicAuthDto);
        jsonArray.add(basicAuthDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                .content(jsonArray.toJSONString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test case for an invalid request with a malformed JSON object.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{username: username, password: password}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case for an invalid request where the JSON object is mising.
     * The test expects a bad request response.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request where the username is an empty string.
     * The test expects a successful response.
     */
    @Test
    void bodyVar_username_validTestStringAnyLength() throws Exception {
        basicAuthDto.setUsername("");
        JSONObject json = new JSONObject();
        json.put("dto", basicAuthDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case for a valid request where the username is incorrect.
     * The mocked AuthenticationManager throws a BadCredentialsException("Incorrect username or password.").
     * The test expects a response equal to Response<>(0, "Incorrect username or password.", null).
     */
    @Test
    void bodyVar_username_validTestStringIsIncorrect() throws Exception {
        basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername("");
        basicAuthDto.setPassword(user.getPassword());

        Assertions.assertTrue(StringUtils.isEmpty(basicAuthDto.getVerificationCode()));

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Incorrect username or password."));

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockServer.verify();

        Assertions.assertEquals(new Response<>(0, "Incorrect username or password.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case for an valid request where the verification code is invalid.
     * The verification code is sent to ts-verification-code-service and receives a response with value false.
     * The test expects a response indicating a verification failure, equal to:
     * Response<>(0, "Verification failed.", null)
     */
    @Test
    void bodyVarVerifyCodeInvalid() throws Exception {
        userRepository.save(user);

        mockServer.expect(requestTo("http://ts-verification-code-service:15678/api/v1/verifycode/verify/" + basicAuthDto.getVerificationCode()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .body(mapper.writeValueAsBytes(false)));

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockServer.verify();

        Assertions.assertEquals(new Response<>(0, "Verification failed.", null), JSONObject.parseObject(result, Response.class));
    }
}
