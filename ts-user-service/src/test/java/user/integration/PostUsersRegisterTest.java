package user.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import user.entity.User;
import user.repository.UserRepository;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;

/*
 * This endpoint POSTS a UserDto object to save/register a new user in the repository. To do that, it also communicates
 * with the authentication service to create an authentication for the new user. As such we need to test the equivalence
 * classes for the attributes of the UserDto object and defects for the endpoint. We also need to setup a MongoDBContainer
 * for the repository. But we only test equivalence classes/defects, that trigger a communication with the external service.
 * It is important to mention, that due to the fallback method in the controller having a different return type,
 * we couldn't perform a request on mockMVC. As such we changed its return type so that the tests could run. Else they would
 * all fail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostUsersRegisterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final static Network network = Network.newNetwork();

    @Container
    private static final MongoDBContainer userMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");

    @Container
    private static final MongoDBContainer authMongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-auth-mongo");

    @Container
    private static GenericContainer<?> authServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-auth-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12340)
            .withNetwork(network)
            .withNetworkAliases("ts-auth-service")
            .dependsOn(authMongoDBContainer);

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", userMongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", userMongoDBContainer.getMappedPort(27017).toString());
        userMongoDBContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.auth.service.url", authServiceContainer::getHost);
        registry.add("ts.auth.service.port", () -> authServiceContainer.getMappedPort(12340));
    }



    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * The first equivalence class test is for valid values for all attributes, which results in creating the object
     * with the name in the repository. The only requirements for the attributes are for the name, which can't be null or
     * empty and the password, which can't be null or length < 6.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception{
        UUID id = UUID.randomUUID();
        String requestJson = "{\"userId\":\""+ id + "\", \"userName\":\"name\", \"password\":\"password\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"me@gmail.com\"}";

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(userRepository.findByUserId(id));
        assertEquals(new Response<>(1, "REGISTER USER SUCCESS", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this equivalence class we test a valid object, that already exists in the repository with that name. As such
     * the POST is expected to fail.
     */
    @Test
    @Order(2)
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
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
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
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * For this equivalence class we test null or empty values for the String attributes and unusual int for
     * the int attributes, which would normally not be a correct value. The expected response is that there will be an
     * exception thrown in the authService, because of the name and password requirements. This returns a null response from
     * the authService and for this endpoint as a result as well. Normally this should be covered/caught.
     */
    @Test
    @Order(3)
    void bodyVarTestValueNullOrOutOfRange() throws Exception {
        String requestJson = "{\"userId\":null, \"userName\":\"\", \"password\":null, \"gender\":" + Integer.MIN_VALUE + ", \"documentType\":" + Integer.MIN_VALUE + ", \"documentNum\":null, \"email\":null}";

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this defect test case, we test the situation, where the external service is unavailable, so the request does
     * not get processed. As it is not caught, this results in a null response. Normally this case should be handled. That
     * is why this test fails.
     */
    @Test
    @Order(4)
    void defectTestUnavailableService() throws Exception{
        authServiceContainer.stop();
        UUID id = UUID.randomUUID();
        String requestJson = "{\"userId\":\""+ id + "\", \"userName\":\"name\", \"password\":\"password\", \"gender\":1, \"documentType\":1, \"documentNum\":\"1\", \"email\":\"me@gmail.com\"}";

        String result = mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNull(userRepository.findByUserId(id));
        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));

    }
}
