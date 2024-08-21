package price.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import price.repository.PriceConfigRepository;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint either creates or modifies a priceConfig and saves it in the repository. It gets a PriceConfig object as
 * the body and uses its parameters to create/modify the priceConfig. As such we test defect tests for the REST endpoint, equivalence
 * class tests for the attributes of the object and specific defect tests for this endpoint. It interacts only with the
 * database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PostPriceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceConfigRepository priceConfigRepository;

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
        priceConfigRepository.deleteAll();
    }

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This is to test the standard case of POST with a correct object and checking afterwards if it was correctly saved
     * in the repository and for the Success response.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        String requestJson = "{\"id\":null, \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertEquals(1, priceConfigRepository.findAll().size());
        assertEquals("train", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":null, \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":null, \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";


        mockMvc.perform(post("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * This defect test case is for if the priceConfig with the id already exists in the repository, which means it will
     * be modified with the new values and not newly created. We do this by performing 2 POST requests. The response is
     * the same even though it is modified.
     */
    @Test
    void validTestDuplicateObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

        requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"new train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        result = mockMvc.perform(post("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("new train", priceConfigRepository.findById(id).getTrainType());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object. These can be counted as their own equivalence class for these attributes.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":valid, \"routeId\":value, \"basicPriceRate\":null, \"firstClassPriceRate\":\"0.0\"}";

        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * In this test case the JSON body is empty, which means that there is no object to POST
     */
    @Test
    void invalidTestMissingBody() throws Exception {
        String requestJson = "";

        mockMvc.perform(post("/api/v1/priceservice/prices")
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
     * The id has some equivalence classes. The valid randomly generated one was already tested in the first test above.
     * In this test, we assign an invalid value, which does not conform to the UUID standard. The expexted result is a
     * client error
     */
    @Test
    void bodyVarIdInvalidTestValue() throws Exception {
        String requestJson = "[{\"id\":\"not a valid UUID\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we test the highest UUID, which can often cause a defect, because it is an edge case
     */
    @Test
    void bodyVarIdValidTestValueTooHigh() throws Exception {
        UUID id = UUID.fromString("fffffff-ffff-ffff-ffff-ffffffffffff");
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The same test for the lowest UUID value
     */
    @Test
    void bodyVarIdInvalidTestValueTooLow() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * UUID can also be null, which is a typical defect as well.
     */
    @Test
    void bodyVarIdvalidTestIsNull() throws Exception {
        String requestJson = "{\"id\":null, \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, priceConfigRepository.findAll().size());
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The traintype and routeId are both Strings with no restrictions, so they have few equivalence classes. The first
     * would be to have a valid string. Here we test this for both attributes with unusual characters.
     */
    @Test
    void bodyVarTraintypeRouteIdValidTest() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"0.0&§%!\", \"routeId\":\"0.0&§%!()=*';:\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("0.0&§%!", priceConfigRepository.findById(id).getTrainType());
        assertEquals("0.0&§%!()=*';:", priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For the other equivalence class for the string types, we assign null to the attributes, which is often a typical
     * defect, which is why it is tested even if it has the same response.
     */
    @Test
    void bodyVarTraintypeInvalidTestStringIsNull() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":null, \"routeId\":null, \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(priceConfigRepository.findById(id));
        assertNull(priceConfigRepository.findById(id).getTrainType());
        assertNull(priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Like for the UUID, we also test the highest value of double as a typical defect.
     */
    @Test
    void bodyVarPricerateInvalidTestValueTooHigh() throws Exception{
        UUID id = UUID.randomUUID();
        double value = Double.MAX_VALUE;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(value, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Here we test the smallest possible value, which is also negative. This does normally not make sense for a pricerate.
     */
    @Test
    void bodyVarPricerateInvalidTestValueTooLow() throws Exception {
        UUID id = UUID.randomUUID();
        double value = Double.MIN_VALUE;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(value, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Both price rates are from type double. Valid values of their equivalence class were already tested in the tests
     * above. This test tests negative values for the price rate, which does not make sense in the context, which is
     * why it is its own class.
     */
    @Test
    void bodyVarPricerateTestValueIsNegative() throws Exception{
        UUID id = UUID.randomUUID();
        double value = -100.203442;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(value, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Create success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }
}
