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
import org.mockito.Mock;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for PUT /api/v1/adminbasicservice/adminbasic/contacts endpoint.
 * This endpoint sends a PUT request to ts-contacts-service to update a specific contact object.
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutAdminBasicContactsTest
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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * Test a valid PUT request with correct object data.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Response<Contacts> response = new Response<>(1, "Modify success", contact);

        mockServer.expect(ExpectedCount.once(), requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, new TypeReference<Response<Contacts>>(){}));

    }

    /*
     * Test a valid PUT request that updates the object correctly.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", contact.getId());
        json.put("accountId", contact.getAccountId());
        json.put("name", contact.getName());
        json.put("documentType", contact.getDocumentType());
        json.put("documentNumber", contact.getDocumentNumber());
        json.put("phoneNumber", contact.getPhoneNumber());

        Response<Contacts> expected = new Response<>(1, "Modify success", contact);
        mockServer.expect(requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(expected)));

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Contacts> actualResponse = JSONObject.parseObject(result, new TypeReference<Response<Contacts>>(){});

        Assertions.assertEquals(actualResponse.getData().getName(), contact.getName());
        Assertions.assertEquals(actualResponse.getData().getDocumentType(), contact.getDocumentType());
        Assertions.assertEquals(actualResponse.getData().getPhoneNumber(), contact.getPhoneNumber());
        Assertions.assertEquals(actualResponse.getData().getDocumentNumber(), contact.getDocumentNumber());
        Assertions.assertEquals(actualResponse.getData().getAccountId(), contact.getAccountId());
    }


    /*
     * Test an invalid PUT request with multiple objects in the payload.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(contact);
        jsonArray.add(contact);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                .andExpect(status().is4xxClientError());
    }

    /*
     * Test an invalid PUT request with a malformed object in the payload.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: '1', name: 'Test Name'}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test a PUT request with a valid ID where the object is missing in the request body.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
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
     * Test a PUT request with a valid ID where the object does not exist.
     */
    @Test
    void bodyVar_id_validTestNotExisting() throws Exception {
        Contacts updatedContact = new Contacts();
        updatedContact.setId(contact.getId());
        updatedContact.setName("New Name");
        updatedContact.setAccountId(UUID.randomUUID());
        updatedContact.setDocumentNumber("A123456");
        updatedContact.setPhoneNumber("123456789");
        updatedContact.setDocumentType(1);

        Response<Object> response = new Response<>(0, "Contacts not found", null);

        mockServer.expect(ExpectedCount.once(), requestTo("http://ts-contacts-service:12347/api/v1/contactservice/contacts"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response)));

        JSONObject json = new JSONObject();
        json.put("mci", updatedContact);

        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    /*
     * Validates that an invalid PUT request fails when the id field exceeds the maximum allowed length.
     * Expects a Bad Request status indicating that the ID string is too long.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests an invalid PUT request where the id field is shorter than the minimum allowed length.
     * Expects a Bad Request status due to the ID string being too short.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Verifies that an invalid PUT request fails when the id field contains invalid characters.
     * Expects a Bad Request status indicating incorrect characters in the ID.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests a valid scenario where the id field in the payload is null.
     * Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    /*
     *  Validates a PUT request with a name field containing any characters within the allowed length.
     *  Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests a valid scenario where the name field exceeds the maximum allowed length.
     * Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Verifies a valid PUT request where the name field in the payload is null.
     * Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Tests a valid PUT request with documentType field containing any integer value, including negative numbers.
     * Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Validates that an invalid PUT request fails when the documentType field is a string instead of an integer.
     * Expects a Bad Request status due to incorrect variable type in documentType.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Tests a valid scenario where the documentType field in the payload is null.
     * Expects an OK status indicating successful processing of the request.
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

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/adminbasicservice/adminbasic/contacts")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
