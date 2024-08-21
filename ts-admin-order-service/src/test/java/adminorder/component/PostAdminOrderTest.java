package adminorder.component;

import adminorder.entity.Order;
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
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for POST /api/v1/adminorderservice/adminorder endpoint.
 * This endpoint send POST request to either ts-order-service or ts-order-other-service, depending on the first letter of trainNumber attribute,
 * to create a new order object.
 * The responses of both services are mocked in all test cases.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostAdminOrderTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private Order order;

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

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(UUID.randomUUID());
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order.setContactsName("Name");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("FirstClass");
        order.setFrom("berlin");
        order.setTo("muenchen");
        order.setStatus(0);
        order.setPrice("100");
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case to verify that a valid order object is correctly processed by the ts-order-service.
     * This test ensures that when a POST request is made with a valid order object to the adminorderservice endpoint,
     * the ts-order-service responds with an OK status and the expected response body containing the newly added order.
     * It validates the correct handling of the order creation process.
     */
    @Test
    void validTestCorrectObjectOrderService() throws Exception {
        // Add order to ts-order-service because trainNumber starts with 'G'
        Response<Order> expectedResponse = new Response<>(1, "Add new Order Success", order);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        // Create JSON object for request payload
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        // Perform POST request
        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, new TypeReference<Response<Order>>(){}));
    }

    /*
     * Test case to verify that a valid order object is correctly processed by the ts-order-other-service.
     * This test checks that when the trainNumber of the order does not start with 'G' or 'D', the order is added
     * to the ts-order-other-service. It expects an OK status from the adminorderservice endpoint and validates
     * the response body containing the newly added order from the ts-order-other-service.
     */
    @Test
    void validTestCorrectObjectOrderOtherService() throws Exception {
        // Change trainNumber to not start with 'G' or 'D' to add order to ts-order-other-service
        order.setTrainNumber("B1234");

        Response<Order> expectedOtherResponse = new Response<>(1, "Success", order);
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedOtherResponse)));

        // Create JSON object for request payload with modified trainNumber
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        String actualOtherResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedOtherResponse, JSONObject.parseObject(actualOtherResponse, new TypeReference<Response<Order>>(){}));
    }

    /*
     * Test case to verify handling of an invalid request with multiple objects.
     * This test checks that attempting to process a JSON array containing multiple orders results in a
     * client error. It ensures that the system correctly rejects requests with multiple
     * objects when only a single object is expected.
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(order);
        jsonArray.add(order);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(jsonArray.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }

    /*
     * Test case to verify handling of a duplicate order object in ts-order-service.
     * This test ensures that when a duplicate order (same ID) is attempted to be added to the ts-order-service,
     * the system correctly responds with an error indicating that the order already exists. It expects an OK status
     * with the appropriate error message in the response body.
     */
    @Test
    void invalidTestDuplicateObjectOrderService() throws Exception {
        // Add to ts-order-service
        Response<Object> expectedResponse = new Response<>(0, "Order already exist", null);
        mockServer.expect(requestTo("http://ts-order-service:12031/api/v1/orderservice/order/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedResponse)));

        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case to verify handling of a duplicate order object in ts-order-other-service.
     * This test ensures that when a duplicate order (same ID) is attempted to be added to the ts-order-other-service,
     * the system correctly responds with an error indicating that the order already exists. It expects an OK status
     * with the appropriate error message in the response body.
     */
    @Test
    void invalidTestDuplicateObjectOrderOtherService() throws Exception {
        // Add to ts-order-other-service
        order.setTrainNumber("B1234");
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        Response<Object> expectedOtherResponse = new Response<>(0, "Order already exist", null);
        mockServer.expect(requestTo("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/admin"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JSONObject.toJSONString(expectedOtherResponse)));

        String actualOtherResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockServer.verify();
        Assertions.assertEquals(expectedOtherResponse, JSONObject.parseObject(actualOtherResponse, Response.class));
    }

    /*
     * Test case to verify handling of a malformed JSON object in the request.
     * This test checks that when the request payload contains a malformed JSON object,
     * the system correctly responds with a 400 Bad Request status, indicating an invalid request format.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String malformedJson = "{id: 1, trainNumber: G1234}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify handling of a request with a missing object in the payload.
     * This test ensures that when the request payload does not contain a valid order object,
     * the system correctly responds with a 400 Bad Request status, indicating a missing required object.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
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
     *  Test case to verify handling of an order object with an 'id' field that exceeds maximum length.
     * This test checks that when the 'id' field of the order object exceeds the maximum allowed length,
     * the system correctly responds with a Bad Request status, indicating an invalid 'id' length.
     */
    @Test
    void bodyVar_id_invalidTestStringTooLong() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32-68b4db4ac3e8-68b4db4ac3e8");
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify handling of an order object with an 'id' field that is shorter than minimum length.
     * This test checks that when the 'id' field of the order object is shorter than the minimum required length,
     * the system correctly responds with a Bad Request status, indicating an invalid 'id' length.
     */
    @Test
    void bodyVar_id_invalidTestStringTooShort() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "57dbd8af-2bf3-424f-8c32");
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify handling of an order object with an 'id' field that contains invalid characters.
     * This test checks that when the 'id' field of the order object contains characters not allowed,
     * the system correctly responds with a Bad Request status, indicating invalid characters in the 'id'.
     */
    @Test
    void bodyVar_id_invalidTestStringContainsWrongCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "+*ç%&/-()=)-?%*/-+*%&-+*%&/()=()()");
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify handling of an order object with a null 'id' field.
     * This test ensures that when the 'id' field of the order object is null,
     * the system correctly processes the request without errors, as null 'id' is considered valid.
     */
    @Test
    void bodyVar_id_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", null);
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case to verify handling of a valid order object where the 'travelDate' is set before 'boughtDate'.
     * This test checks that when the 'travelDate' of the order object is set before the 'boughtDate',
     * the system correctly processes the request without errors. It validates the handling of travel dates
     * that are earlier than the purchase date.
     */
    // Valid because it is never checked if travelDate is before boughtDate
    @Test
    void bodyVar_travelDate_validTestDateTooEarlyOrDateInRange() throws Exception {
        // switch travel date and bought date, so that travel date is before bought date
        Date date = order.getTravelDate();
        order.setTravelDate(order.getBoughtDate());
        order.setBoughtDate(date);

        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    /*
     * Test case to verify handling of an order object with an 'travelDate' field in an incorrect format.
     * This test checks that when the 'travelDate' field of the order object is in an incorrect format,
     * the system correctly responds with a 400 Bad Request status, indicating an invalid date format.
     */
    @Test
    void bodyVar_traveldate_invalidTestDateIsInWrongFormat() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", "wrongFormat");
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test case to verify handling of an order object with a null 'travelDate' field.
     * This test ensures that when the 'travelDate' field of the order object is null,
     * the system correctly processes the request without errors, as null 'travelDate' is considered valid.
     */
    @Test
    void bodyVar_traveldate_validTestDateIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", null);
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * Test case to verify handling of an order object with a 'contactsName' field of correct length and valid characters.
     * This test checks that when the 'contactsName' field of the order object contains characters of correct length,
     * the system correctly processes the request without errors, validating the correct handling of 'contactsName'.
     */
    @Test
    void bodyVar_contactsname_validTestCorrectLengthAndAnyCharacters() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", "%%%%%");
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());


        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when the contactsName field in the JSON payload is null.
     * The test expects a response with statusOK.
     */
    @Test
    void bodyVar_contactsname_validTestStringIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", null);
        json.put("documentType", order.getDocumentType());
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when the documentType field in the JSON payload is set to a negative integer value.
     * The test expects a response with status OK.
     */
    @Test
    void bodyVar_documenttype_validTestValueNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", -2);
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
     * This test verifies the behavior when the documentType field in the JSON payload has an invalid type (string instead of integer).
     * This test expects a response with status Bad request.
     */
    @Test
    void bodyVar_documenttype_invalidTestWrongVariableType() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", "shouldNotBeString");
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
     * This test verifies the behavior when the documentType field in the JSON payload is null.
     * This test expects a response with status OK.
     */
    @Test
    void bodyVar_documenttype_validTestIsNull() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", order.getId());
        json.put("trainNumber", order.getTrainNumber());
        json.put("accountId", order.getAccountId());
        json.put("boughtDate", order.getBoughtDate());
        json.put("coachNumber", order.getCoachNumber());
        json.put("contactsDocumentNumber", order.getContactsDocumentNumber());
        json.put("contactsName", order.getContactsName());
        json.put("documentType", null);
        json.put("from", order.getFrom());
        json.put("price", order.getPrice());
        json.put("seatClass", order.getSeatClass());
        json.put("seatNumber", order.getSeatNumber());
        json.put("status", order.getStatus());
        json.put("to", order.getTo());
        json.put("travelDate", order.getTravelDate());
        json.put("travelTime", order.getTravelTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/adminorderservice/adminorder")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
