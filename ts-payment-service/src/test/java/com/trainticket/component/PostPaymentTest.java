package com.trainticket.component;

import com.alibaba.fastjson.JSONObject;
import com.trainticket.repository.PaymentRepository;
import edu.fudan.common.util.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This POST endpoint expects a Payment object in the body and adds it to the paymentRepository depending on if an object
 * with the same orderId already exists in the repository. Therefore we have test cases for the equivalence classes of the
 * body object attributes as well as defects tests and tests for the REST endpoint itself. It interacts only with the database,
 * which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostPaymentTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

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
    void beforeEach() {
        paymentRepository.deleteAll();
    }


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This is to test the standard case of POST with a correct object and checking afterwards if it was correctly saved
     * in the repository and for the success response.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"orderId\":\"1\", \"userId\":\"1\", \"price\":\"1\"}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(paymentRepository.findByOrderId("1"));
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":\"1234567890\", \"orderId\":\"1\", \"userId\":\"1\", \"price\":\"1\"},{\"id\":\"id2\", \"orderId\":\"2\", \"userId\":\"2\", \"price\":\"1\"},{\"id\":\"id3\", \"orderId\":\"3\", \"userId\":\"3\", \"price\":\"1\"}]";

        mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * This defect test for the endpoint is similar to the one before, but this time we have the same object multiple times.
     */
    @Test
    void invalidTestDuplicateObject() throws Exception {
        String requestJson = "[{\"id\":\"1234567890\", \"orderId\":\"1\", \"userId\":\"1\", \"price\":\"1\"},{\"id\":\"1234567890\", \"orderId\":\"1\", \"userId\":\"1\", \"price\":\"1\"}";

        mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());

    }

    /*
     * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        String requestJson = "{\"id\":not, \"orderId\":right, \"userId\":type}";

        mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());

    }

    /*
     * In this test case the JSON body is empty, which means that there is no object to POST
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());

    }
    /*
    #####################################
    # Body variable specific test cases #
    #####################################
    */

    /*
     * As all attributes of the body object are Strings we can test them the different equivalence classes for Strings on
     * them simultaneously. For equivalence based testing we only look at inputs and outputs and not at the implemented logic.
     * Valid values for the attributes was already tested in the first test case. Here we assign long Strings to the attributes,
     * which are also valid.
     */
    @Test
    void bodyVarinvalidTestStringTooLong() throws Exception {
        String requestJson = "{\"id\":\"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                "\"orderId\":\"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                "\"userId\":\"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                "\"price\":\"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890\"}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(paymentRepository.findByOrderId("123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"));
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we assign a short String to the attributes, which means an empty String, and test the response of the endpoint.
     * This is valid as well.
     */
    @Test
    void bodyVarinvalidTestStringTooShort() throws Exception {
        String requestJson = "{\"id\":\"\", \"orderId\":\"\", \"userId\":\"\", \"price\":\"\"}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(paymentRepository.findByOrderId(""));
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we assign Strings with unexpected but valid characters to the attributes, because for attributes like id and price you would
     * expect a number.
     */
    @Test
    void bodyVarinvalidTestStringContainsWrongCharacters() throws Exception {
        String requestJson = "{\"id\":\"this is an id()/\", \"orderId\":\"also and id13&\", \"userId\":\"as well\", \"price\":\")(/&!/§$!\"}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(paymentRepository.findByOrderId("also and id13&"));
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * As String is an object, we obviously also try to assign null to the attributes and test the endpoint with this object.
     */
    @Test
    void bodyVarinvalidTestStringIsNull() throws Exception {
        String requestJson = "{\"id\":null, \"orderId\":null, \"userId\":null, \"price\":null}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, paymentRepository.findAll().size());
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));

    }
    /*
    #####################################
    #      Specific endpoint tests      #
    #####################################
    */

    /*
     * This specific defect based test, tests the case when there is already a payment object with the orderId in the
     * repository and checks the specific response for that case.
     */
    @Test
    void paymentAlreadyInRepository() throws Exception {
        String requestJson = "{\"id\":\"1234567890\", \"orderId\":\"1\", \"userId\":\"1\", \"price\":\"1\"}";

        mockMvc.perform(post("/api/v1/paymentservice/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isOk());

        assertNotNull(paymentRepository.findByOrderId("1"));

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(paymentRepository.findByOrderId("1"));
        assertEquals(new Response<>(0, "Pay Failed, order not found with order id1", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * We also test if the JSON does not have any attributes set, which default to a specific value
     */
    @Test
    void noSetAttributesTest() throws Exception {
        String requestJson = "{}";

        String result = mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, paymentRepository.findAll().size());
        assertEquals(new Response<>(1, "Pay Success", null), JSONObject.parseObject(result, Response.class));
    }
}
