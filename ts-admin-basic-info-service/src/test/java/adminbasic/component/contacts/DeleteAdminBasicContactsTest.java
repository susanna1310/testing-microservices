package adminbasic.component.contacts;

import com.alibaba.fastjson.JSONObject;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/*
 * Test class for DELETE /api/v1/adminbasicservice/adminbasic/contacts/{contactsId} endpoint.
 * This endpoint sends a DELETE request to ts-contacts-service to delete a specific contact object with the given Id.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class DeleteAdminBasicContactsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
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

    }

	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * Valid test case for deleting a correct contacts object.
     * Verifies that the DELETE operation returns a success response with the deleted contacts ID.
     */
    @Test
    public void validTestCorrectObject() throws Exception {
        String contactsId = UUID.randomUUID().toString();

        Response<String> response = new Response<>(1, "Delete success", contactsId);

        String json = JSONObject.toJSONString(response);
        mockServer.expect(requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + contactsId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", contactsId)
                        .header(HttpHeaders.AUTHORIZATION, ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<String>>(){}));
    }

    /*
     * Valid test case for deleting multiple contacts objects.
     * Verifies that attempting to delete multiple objects results in a OK response, because only first parameter gets used.
     */
    @Test
    public void validTestMultipleObjects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", 1, 2))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for deleting a malformed contacts object.
     * Verifies that attempting to delete a malformed contacts ID results in a client error
     */
    @Test
    public void invalidTestMalformedObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", "1/2"))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for deleting a missing contacts object.
     * Verifies that attempting to delete without specifying a contacts ID results in an IllegalArgumentException.
     */
    @Test
    public void invalidTestMissingObject() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}")));
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Valid test case for deleting a non-existing contacts ID.
     * Verifies that attempting to delete a non-existing contacts ID results in a failed response.
     */
    @Test
    public void validTestNonexistingId() throws Exception {
        String nonExistingId = "notExisting";
        Response<String> response = new Response<>(0, "Delete failed", nonExistingId);

        mockServer.expect(requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + nonExistingId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsBytes(response)));

        String result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", nonExistingId)
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Valid test case for deleting a contacts object with ID in a non-correct format.
     * Verifies that the DELETE operation succeeds even with non-correct format ID.
     */
    @Test
    public void validTestNonCorrectFormatIdOrSpecialCharacters() throws Exception {
        int nonCorrectFormatId = 1;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", nonCorrectFormatId)
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
