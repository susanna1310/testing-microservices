package adminbasic.component.contacts;

import adminbasic.entity.Contacts;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/adminbasicservice/adminbasic/contacts endpoint.
 * This endpoint sends a GET request to ts-contacts-service to retireve all existing contact objects.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetAdminBasicContactsTest
{

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

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
     * Valid test case for retrieving all contacts.
     * Verifies that the GET operation returns a list of contacts successfully.
     */
    @Test
    public void validTestGetAllObjects() throws Exception {
        // Create test data
        Contacts contact1 = new Contacts();
        contact1.setId(UUID.randomUUID());
        contact1.setName("Max Mustermann");
        contact1.setPhoneNumber("123456789");
        contact1.setDocumentNumber("A1234567");
        contact1.setAccountId(UUID.randomUUID());
        contact1.setDocumentType(1);

        Contacts contact2 = new Contacts();
        contact2.setId(UUID.randomUUID());
        contact2.setName("Anna");
        contact2.setPhoneNumber("987654321");
        contact2.setDocumentNumber("B9876543");
        contact2.setAccountId(UUID.randomUUID());
        contact2.setDocumentType(2);

        ArrayList<Contacts> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);

        Response<ArrayList<Contacts>> response = new Response<>(1, "Success", contacts);

        // Mock the response from ts-contacts-service

        String json = JSONObject.toJSONString(response);
        this.mockServer.expect(requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        // Perform GET request
        String result = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        this.mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<ArrayList<Contacts>>>(){}));
    }

    /*
     * Valid test case for retrieving zero contacts.
     * Verifies that the GET operation returns a successful response with no content.
     */
    @Test
    public void validTestGetZeroObjects() throws Exception {
        // Create empty response
        Response<ArrayList<Contacts>> response = new Response<>(0, "No content", null);

        // Mock response from ts-contacts-service
        mockServer.expect(requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsBytes(response)));

        // GET request to ts-admin-basic-info-service
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }
}
