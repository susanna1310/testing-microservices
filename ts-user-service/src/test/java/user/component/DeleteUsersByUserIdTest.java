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
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import user.dto.AuthDto;
import user.entity.User;
import user.repository.UserRepository;
import user.service.UserService;


import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint deletes a User object with the given id per URL parameter in the repository. It also communicates
 * with the authentication service to create an authentication for the new user. As such we need to test the equivalence
 * classes for the attributes of the UserDto object. Because the service communicates with another service via RestTemplate,
 * we use MockRestServiceServer to mock the response of the external service. We also need to setup a MongoDBContainer
 * for the repository.
 * It is important to mention, that due to the fallback method in the controller having a different return type,
 * we can't perform a request on mockMVC that relies on mockServer for this endpoint. Not autowiring the restTemplate in the implementation
 * of the service, also causes an issue that we can't setup the MockRestServiceServer for external request. That is the
 * reason, why some test cases fail. This would be solved by changing the return type of the fallback method and either
 * autowiring the restTemplate via a bean or adding a getter or setter method for the restTemplate
 *
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteUsersByUserIdTest {

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * For the first equivalence class we want to delete the object, which is why we insert the object before the request.
     * As such the combination of a valid input id and the insertion before, we reach the corresponding response.
     */
    @Test
    void validTestCorrectObject() throws Exception{
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-auth-service:12340/api/v1/users/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);

        //Actual request to the endpoint we want to test
        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertNull(userRepository.findByUserId(id));
        assertEquals(0, userRepository.findAll().size());
        assertEquals(new Response<>(1, "DELETE SUCCESS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the second equivalence class, we do not insert anything, so we don't delete anything, because the
     * repository is empty. This is the same outcome as when we want to delete with an id, that no object in the repository has.
     */
    @Test
    void validTestMissingObject() throws Exception{

        UUID id = UUID.randomUUID();

        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString())
                )
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
    void invalidTestMultipleObjects() throws Exception {
        //Mock responses of external services for every request this service does for the endpoint

        UUID id = UUID.randomUUID();
        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-auth-service:12340/api/v1/users/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = User.builder().email("old")
                .password("old")
                .userId(id)
                .userName("name")
                .gender(0)
                .documentNum("0")
                .documentType(0).build();
        userRepository.save(user);

        //Actual request to the endpoint we want to test
        String result = mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString(), UUID.randomUUID().toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        assertNull(userRepository.findByUserId(id));
        assertEquals(0, userRepository.findAll().size());
        assertEquals(new Response<>(1, "DELETE SUCCESS", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we test the case, when the input parameter is malformed in any way, in other words if the parameter invalid characters,
     * wrong attribute types etc,which should not be able to be converted into the right object. We expect a 4xx client error.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {

        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", UUID.randomUUID() + "/" + UUID.randomUUID())
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give nothing to the endpoint, which means there is nothing to delete. This is the same outcome as when the
     * parameter is null.
     */
    @Test
    void invalidTestMissingBody() {
        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(delete("/api/v1/userservice/users/{userId}")
                );});

    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * For the last equivalence class, we test when the id has an incorrect format, which means it can't be converted to
     * UUID. This should cause a exception.
     */
    @Test
    void invalidTestNonCorrectFormatId() {
        assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete("/api/v1/userservice/users/{userId}", "not a valid UUID")
        );});
    }

}
