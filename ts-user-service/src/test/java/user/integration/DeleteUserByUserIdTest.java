package user.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;

/*
 * This endpoint deletes a User object with the given id per URL parameter in the repository. It also communicates
 * with the authentication service to delete an authentication for the new user. As such we need to test the equivalence
 * classes for the attributes of the UserDto object and defects of the endpoint. We also need to setup a MongoDBContainer
 * for the repository. But we only test equivalence classes/defects, that trigger a communication with the external service.
 * It is important to mention, that due to the fallback method in the controller having a different return type,
 * we couldn't perform a request on mockMVC. As such we changed its return type so that the tests could run. Else they would
 * all fail.
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeleteUserByUserIdTest {

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

	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * For the first equivalence class we want to delete the object, which is why we delete the object of the init class.
     * As such the combination of a valid input id with UUID format and the insertion before, we reach the corresponding response.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception{
        assertNotNull(userRepository.findByUserId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")));
        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNull(userRepository.findByUserId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")));
        assertEquals(0, userRepository.findAll().size());
        assertEquals(new Response<>(1, "DELETE SUCCESS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the second equivalence class, we do not insert anything, so we don't delete anything, because the
     * repository is empty. This is the same outcome as when we want to delete with an id, that no object in the repository has.
     */
    @Test
    @Order(2)
    void validTestMissingObject() throws Exception{

        UUID id = UUID.randomUUID();

        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNull(userRepository.findByUserId(id));
        assertEquals(new Response<>(0, "USER NOT EXISTS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For this input class we test the case when we give the endpoint request more than one parameter. This
     * is expected to not cause issues, because opposite to JSON body objects, only the first parameter is used.
     */
    @Test
    @Order(3)
    void validTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .userId(id)
                .userName("test")
                .password("111111")
                .gender(1)
                .documentType(1)
                .documentNum("2135488099312X")
                .email("test").build();
        userRepository.save(user);

        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString(), UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNull(userRepository.findByUserId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")));
        assertEquals(0, userRepository.findAll().size());
        assertEquals(new Response<>(1, "DELETE SUCCESS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this defect test case, we test the situation, where the external service is unavailable, so the request does
     * not get processed. As it is not caught, this results in a null response. Normally this case should be handled.
     */
    @Test
    @Order(4)
    void defectTestUnavailableService() throws Exception{
        authServiceContainer.stop();
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .userId(id)
                .userName("test")
                .password("111111")
                .gender(1)
                .documentType(1)
                .documentNum("2135488099312X")
                .email("test").build();
        userRepository.save(user);
        assertNotNull(userRepository.findByUserId(id));
        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }

}
