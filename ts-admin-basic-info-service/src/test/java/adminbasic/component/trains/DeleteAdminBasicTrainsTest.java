package adminbasic.component.trains;

import adminbasic.entity.TrainType;
import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class of DELETE /api/v1/adminbasicservice/adminbasic/trains/{id} endpoint.
 * This endpoint send a DELETE request to ts-train-service to delete a specific train object with given id.
 * The responses of the train service are mocked in every test case.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminBasicTrainsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    private TrainType trainType;

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
        trainType = new TrainType(UUID.randomUUID().toString(), 1, 2, 100);
    }

    /*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * This test verifies the correct behavior of the DELETE request to delete a train type object.
     * It mocks the successful deletion response from the train service and asserts that the correct status code is returned.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Boolean> response = new Response<>(1, "delete success", true);

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/" + trainType.getId()))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}", trainType.getId())
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test verifies the behavior when attempting to delete multiple objects, which is not supported.
     * It expects an OK status code, because only the first parameter is used, and the next ones are ignored.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}", 1, 2))
                .andExpect(status().isNotFound());
    }

    /*
     * his test verifies the behavior when attempting to delete an object with a malformed ID format.
     * It expects a 4xx client error status code.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}", "1/2"))
                .andExpect(status().is4xxClientError());

    }

    /*
     * This test verifies the behavior when attempting to delete an object without providing an ID.
     * It expects an IllegalArgumentException to be thrown.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/


    /*
     * This test verifies the behavior when attempting to delete a non-existing object by ID.
     * It mocks the response from the train service indicating that the object does not exist.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        String nonExistingId = "notExisting";
        Response<Object> response = new Response<>(0, "there is no train according to id", null);

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/" + nonExistingId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(response)));

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}", nonExistingId)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test verifies that the API gracefully handles a valid DELETE request where the ID may be an integer,
     * but is expected to be treated as a String due to the API requirements.
     * It ensures that the request URI is correctly formatted and returns an OK status.
     */
    // is valid test, because when id is an int, but should actually be a String
    // then when attaching the int to the uri, it is automatically transformed to a String
    // Uri: "/api/v1/adminbasicservice/adminbasic/trains/{id}", 1
    // to Request URI /api/v1/adminbasicservice/adminbasic/trains/1
    @Test
    void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/trains/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
