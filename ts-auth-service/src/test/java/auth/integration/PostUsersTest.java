package auth.integration;

import auth.dto.BasicAuthDto;
import auth.dto.TokenDto;
import auth.entity.User;
import auth.repository.UserRepository;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.security.authentication.AuthenticationManager;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;

/*
 * Test class for POST /api/v1/users/login endpoint.
 * The endpoint expects a basicAuthDto as input body.
 * Because the external VerificationCode Service uses Cookie and Cache, which we can't influence or replicate via tests,
 * we mock the service via WireMock. That way the request to the external service still happens and is not mocked, but only
 * the data of the response is mocked, because else we could not get a different response from false. It was agreed upon, that
 * this method via WireMock is acceptable in the meeting for setting up the integration tests with the other teams.
 * We test the equivalence classes for the input body and defects of the endpoint.
 * We set up a MongoDB testcontainer for the repository.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class PostUsersTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    private BasicAuthDto basicAuthDto;
    private User user;

    private final static Network network = Network.newNetwork();


    @Container
    private static final MongoDBContainer userMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-auth-mongo");

    //If we wanted to use the external service without mocking the data of the response
    /*@Container
    private static GenericContainer<?> verificationCodeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-verification-code-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15678)
            .withNetwork(network)
            .withNetworkAliases("ts-verification-code-service");*/

    @RegisterExtension
    static WireMockExtension verificationCodeServiceWireMock = WireMockExtension.newInstance().options(wireMockConfig().port(15678)).build();

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", userMongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", userMongoDBContainer.getMappedPort(27017).toString());
        userMongoDBContainer.start();
        configureFor("localhost", 15678);
    }

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        basicAuthDto = new BasicAuthDto("username", "password", "verificationCode");
        user = new User(UUID.randomUUID(), basicAuthDto.getUsername(), basicAuthDto.getPassword(), new HashSet<>(Arrays.asList("ROLE_USER")));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.verification.code.service.url", () -> "localhost");
        registry.add("ts.verification.code.service.port", () -> "15678");
    }


    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request were a correct BasicAuthDto object is posted.
     * The verificationCode attribute is not empty, so we have a request to the ts-verification-code-service.
     * The authenticationManager is mocked and we expect a valid Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()); to be returned
     * The test expects a successful login response with a TokenDto object.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername(user.getUsername());
        basicAuthDto.setPassword(user.getPassword());
        basicAuthDto.setVerificationCode("verificationCode");

        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockVerificationCodeService();

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Response<TokenDto> response = JSONObject.parseObject(result, new TypeReference<Response<TokenDto>>(){});

        verifyMockVerificationCodeService();
        Assertions.assertEquals(response.getStatus(), 1);
        Assertions.assertEquals(response.getMsg(), "login success");
        Assertions.assertEquals(response.getData().getUserId(), user.getUserId());
        Assertions.assertEquals(response.getData().getUsername(), user.getUsername());
    }

	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Equivalence-class/defect test case for the defect case, where the interaction with the verificationCodeService and the following
     * authentication is successful, but the user with the name is not in the repository. This returns a different response.
     */
    @Test
    void invalidDefectUserNotInRepositoryTest() throws Exception {
        basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername(user.getUsername());
        basicAuthDto.setPassword(user.getPassword());
        basicAuthDto.setVerificationCode("verificationCode");

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockVerificationCodeService();

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        System.out.println(result);

        verifyMockVerificationCodeService();
        Assertions.assertEquals(new Response<>(0, "Verification failed.", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Equivalence-class/defect test case for a valid request where the username or password is incorrect.
     * The AuthenticationManager throws a BadCredentialsException("Incorrect username or password.").
     * The test expects a response equal to Response<>(0, "Incorrect username or password.", null).
     */
    @Test
    void bodyVar_username_validTestStringIsIncorrect() throws Exception {
        basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername("");
        basicAuthDto.setPassword("");
        basicAuthDto.setVerificationCode("verificationCode");

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Incorrect username or password."));

        mockVerificationCodeService();

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        verifyMockVerificationCodeService();
        Assertions.assertEquals(new Response<>(0, "Incorrect username or password.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Equivalence-class/defect test case for a valid request where the verification code is invalid.
     * The verification code is sent to ts-verification-code-service and receives a response with value false.
     * The test expects a response indicating a verification failure, equal to:
     * Response<>(0, "Verification failed.", null)
     */
    @Test
    void bodyVarVerifyCodeInvalid() throws Exception {
        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        stubFor(get(urlEqualTo("/api/v1/verifycode/verify/verificationCode"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("false")));

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        verifyMockVerificationCodeService();
        Assertions.assertEquals(new Response<>(0, "Verification failed.", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Test case for a valid request where the verification code service is unavailable so the request does not get processed.
     * We simulate this by not stubbing anything for the external service. Similar to the previous test, this exception
     * results in the response that the verification failed.
     */
    @Test
    void verificationServiceUnavailable() throws Exception {
        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Verification failed.", null), JSONObject.parseObject(result, Response.class));
    }

    private void mockVerificationCodeService() {
        stubFor(get(urlEqualTo("/api/v1/verifycode/verify/verificationCode"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));
    }

    private void verifyMockVerificationCodeService() {
        verify(getRequestedFor(urlEqualTo("/api/v1/verifycode/verify/verificationCode")));
    }
}

