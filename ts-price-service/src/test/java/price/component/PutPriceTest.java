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
import price.entity.PriceConfig;
import price.repository.PriceConfigRepository;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint updates a priceConfig and saves it in the repository. It gets a PriceConfig object as
 * the body and uses its parameters to update the priceConfig. As such we test defect tests for the REST endpoint, equivalence
 * class tests for the attributes of the object and specific defect tests for this endpoint. It interacts only with the
 * database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class PutPriceTest {

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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

    /*
     * This is to test the defect case, where the body object has the same values as the already existing object in the
     * repository with the same id, which means nothing changes. Nevertheless, we expect as success response, because the
     * new values are not checked.
     */
    @Test
    void validTestsSameObject() throws Exception {
        PriceConfig priceConfig = new PriceConfig();
        UUID id = UUID.randomUUID();
        priceConfig.setId(id);
        priceConfig.setTrainType("old train");
        priceConfig.setRouteId("old id");
        priceConfig.setBasicPriceRate(0.0);
        priceConfig.setFirstClassPriceRate(0.0);
        System.out.println(priceConfig);
        priceConfigRepository.save(priceConfig);
        assertEquals(1, priceConfigRepository.findAll().size());
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"old train\", \"routeId\":\"old id\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("old train", priceConfigRepository.findById(id).getTrainType());
        assertEquals("old id", priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This is to test the standard case of PUT with a correct object and checking afterwards if it was correctly updated
     * in the repository and for the Success response. The object has to be saved in the repository before the request.
     */
    @Test
    void validTestUpdatesObjectCorrectly() throws Exception {
        PriceConfig priceConfig = new PriceConfig();
        UUID id = UUID.randomUUID();
        priceConfig.setId(id);
        priceConfig.setTrainType("old train");
        priceConfig.setRouteId("old id");
        System.out.println(priceConfig);
        priceConfigRepository.save(priceConfig);
        assertEquals(1, priceConfigRepository.findAll().size());
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"new train\", \"routeId\":\"new id\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("new train", priceConfigRepository.findById(id).getTrainType());
        assertEquals("new id", priceConfigRepository.findById(id).getRouteId());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        String requestJson = "[{\"id\":null, \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":null, \"trainType\":\"train\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";


        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object. These can be counted as their own equivalence class for these attributes.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":valid, \"routeId\":value, \"basicPriceRate\":null, \"firstClassPriceRate\":\"0.0\"}";

        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * In this defect test, the id of the object we want to update does not exist in the repository. As a result the response
     * will be different.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"new train\", \"routeId\":\"new id\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * The id has some equivalence classes. The valid randomly generated one was already tested in the first test above.
     * In this test, we assign an invalid value, which does not conform to the UUID standard. The expected result is a
     * client error
     */
    @Test
    void bodyVarIdInvalidTestValue() throws Exception {
        String requestJson = "[{\"id\":\"not a valid UUID\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        mockMvc.perform(put("/api/v1/priceservice/prices")
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
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The same test for the lowest UUID value
     */
    @Test
    void bodyVarIdvalidTestValueTooLow() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * UUID can also be null, which is a typical defect as well.
     */
    @Test
    void bodyVarIdInvalidTestIsNull() throws Exception {
        String requestJson = "{\"id\":null, \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The traintype and routeId are both Strings with no restrictions, so they have few equivalence classes. The first
     * would be to have a valid string. Here we test this for both attributes with unusual characters.
     */
    @Test
    void bodyVarTraintypeRouteIdValidTest() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"0.0&§%!\", \"routeId\":\"0.0&§%!()=*';:\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("0.0&§%!", priceConfigRepository.findById(id).getTrainType());
        assertEquals("0.0&§%!()=*';:", priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For the other equivalence class for the string types, we assign null to the attributes, which is often a typical
     * defect, which is why it is tested even if it has the same response.
     */
    @Test
    void bodyVarTraintypevalidTestStringIsNull() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":null, \"routeId\":null, \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotNull(priceConfigRepository.findById(id));
        assertNull(priceConfigRepository.findById(id).getTrainType());
        assertNull(priceConfigRepository.findById(id).getRouteId());
        assertEquals(0.0, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Like for the UUID, we also test the highest value of double as a typical defect.
     */
    @Test
    void bodyVarPricerateInvalidTestValueTooHigh() throws Exception{
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        double value = Double.MAX_VALUE;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(value, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * We also test the lowest possible value, which is negative and does normally not make sense for a price.
     */
    @Test
    void bodyVarPriceratevalidTestValueTooLow() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        double value = Double.MIN_VALUE;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findById(id).getTrainType());
        assertEquals("1", priceConfigRepository.findById(id).getRouteId());
        assertEquals(value, priceConfigRepository.findById(id).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findById(id).getFirstClassPriceRate());
        assertEquals(new Response<>(1, "Update success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

}
