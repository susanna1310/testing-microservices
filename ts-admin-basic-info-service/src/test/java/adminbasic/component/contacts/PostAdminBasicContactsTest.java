package adminbasic.component.contacts;

import adminbasic.entity.Contacts;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/adminbasicservice/adminbasic/contacts endpoint.
 * This endpoint send a POST request to ts-contacts-service to create a new contact object.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminBasicContactsTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private Contacts contact;

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
        contact = new Contacts();
        contact.setId(UUID.randomUUID());
        contact.setName("First Contact");
        contact.setAccountId(UUID.randomUUID());
        contact.setDocumentNumber("A123456");
        contact.setPhoneNumber("123456789");
        contact.setDocumentType(1);
    }

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Valid test case for creating a single contact object.
     * Verifies that the POST operation creates the contact successfully.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Object> expectedResponse = new Response<>(1, "Create Success", null);

        mockServer.expect(ExpectedCount.once(), requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, new TypeReference<Response<Contacts>>(){}));
    }

    /*
     * Valid test case for creating multiple contact objects.
     * Verifies that the POST operation returns a client error for multiple objects.
     */
    @Test
    void validTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(contact);
        jsonArray.add(contact);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Invalid test case for creating a duplicate contact object.
     * Verifies that the POST operation handles the duplicate case correctl
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        Contacts duplicateContact = new Contacts();
        duplicateContact.setId(contact.getId());
        duplicateContact.setName("First Contact");
        duplicateContact.setAccountId(UUID.randomUUID());
        duplicateContact.setDocumentNumber("A123456");
        duplicateContact.setPhoneNumber("123456789");
        duplicateContact.setDocumentType(1);

        Response<Contacts> response = new Response<>(0, "Already Exists", duplicateContact);

        mockServer.expect(ExpectedCount.once(), requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", duplicateContact.getId());
        json.put("accountId", duplicateContact.getAccountId());
        json.put("name", duplicateContact.getName());
        json.put("documentType", duplicateContact.getDocumentType());
        json.put("documentNumber", duplicateContact.getDocumentNumber());
        json.put("phoneNumber", duplicateContact.getPhoneNumber());

        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<Contacts>>(){}));
    }

    /*
     * Invalid test case for sending a malformed JSON object.
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', name: 'Test Name'}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for sending a request with a missing JSON object.
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
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
     * Invalid test case for ID field containing a string that is too long.
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void bodyVar_id_invalidTestStringTooLong() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32-68b4db4ac3e8-68b4db4ac3e8");
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for ID field containing a string that is too short.
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void bodyVar_id_invalidTestStringTooShort() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32");
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Invalid test case for ID field containing a string with invalid characters.
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void bodyVar_id_invalidTestStringContainsWrongCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Valid test case for ID field containing a null value.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_id_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    /*
     * Valid test case for name field containing a string with any characters and correct length.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_name_validTestCorrectLengthAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", "&/%)/%/&");
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for name field containing a string that exceeds the maximum allowed length.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_name_validTestStringTooLong() throws Exception {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String tooLongName = new String(chars);

        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", tooLongName);
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for name field containing a null value.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_name_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", null);
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Valid test case for documentType field containing a negative value.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_documenttype_validTestValueAnyRangeAndNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", -1);
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Invalid test case for documentType field containing a value of wrong variable type (string instead of integer).
     * Verifies that the POST operation returns a bad request.
     */
    @Test
    void bodyVar_documenttype_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", "shouldNotBeString");
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Valid test case for documentType field containing a null value.
     * Verifies that the POST operation is successful.
     */
    @Test
    void bodyVar_documenttype_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", null);
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
