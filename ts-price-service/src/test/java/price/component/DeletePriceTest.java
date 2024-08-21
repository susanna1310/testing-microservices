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
 * This endpoint DELETEs the priceConfig object in the repository with the same attribute values as the body object.
 * As such we test delete specific REST defect tests, url parameter specific equivalence class based tests as well as
 * defect tests. It interacts only with the database, which is why we need to setup a MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class DeletePriceTest {

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * In this test we test the general case, where the body object id is an existing object in the repository,
     * which means the deletion is executed normally.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        PriceConfig priceConfig = new PriceConfig();
        UUID id = UUID.randomUUID();
        priceConfig.setId(id);
        System.out.println(priceConfig);
        priceConfigRepository.save(priceConfig);
        assertEquals(1, priceConfigRepository.findAll().size());
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":null, \"routeId\":null, \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";

        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }


    /*
     * This test tests the REST endpoint on how it handles more than one object in the JSON body. As this is a mistake from
     * the client side, the expected status code should be 4xx as it is for every following test with invalid JSON bodies
     */
    @Test
    void invalidTestMultipleObjects() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";


        mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Here we give a malformed object with wrong attributes types as JSON body, which should not be able to be converted
     * in the right object. These can be counted as their own equivalence class for these attributes.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":valid, \"routeId\":value, \"basicPriceRate\":null, \"firstClassPriceRate\":\"0.0\"}";

        mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no object body json is passed, which is also a case which can happen like the test above.
     */
    @Test
    void invalidTestMissingObject() throws Exception {
        String requestJson = "";

        mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no object with the id exists in the repository, which means there is nothing
     * to delete. The response is therefore different.
     */
    @Test
    void validTestNonexistingObject() throws Exception {
        UUID id = UUID.randomUUID();
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
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
        String requestJson = "[{\"id\":\"not a valid UUID\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        mockMvc.perform(delete("/api/v1/priceservice/prices")
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
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * The same test for the lowest UUID value
     */
    @Test
    void bodyVarIdInvalidTestValueTooLow() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * UUID can also be null, which is a typical defect as well.
     */
    @Test
    void bodyVarIdInvalidTestIsNull() throws Exception {
        String requestJson = "{\"id\":null, \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"},{\"id\":\"1\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}]";

        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
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


        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * For the other equivalence class for the string types, we assign null to the attributes, which is often a typical
     * defect, which is why it is tested even if it has the same response.
     */
    @Test
    void bodyVarTraintypeInvalidTestStringIsNull() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":null, \"routeId\":null, \"basicPriceRate\":\"0.0\", \"firstClassPriceRate\":\"0.0\"}";


        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
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


        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * We also test the lowest possible value, which is negative and does normally not make sense for a price.
     */
    @Test
    void bodyVarPricerateInvalidTestValueTooLow() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        double value = Double.MIN_VALUE;
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"" + value + "\", \"firstClassPriceRate\":\"" + value + "\"}";


        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * Now we test the defect, where the body object does have the same UUID but not all the same values for the other
     * attributes. It gets deleted regardless, but in our opinion this should not be the case and if it is intended, then
     * the endpoint should have been implemented with an URL parameter.
     */
    @Test
    void notTheSameValuesTest() throws Exception {
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        price.setBasicPriceRate(900.24);
        price.setFirstClassPriceRate(300.123);
        price.setTrainType("Fast train");
        price.setRouteId("23");
        priceConfigRepository.save(price);
        String requestJson = "{\"id\":\"" + id + "\", \"trainType\":\"1\", \"routeId\":\"1\", \"basicPriceRate\":\"1.0\", \"firstClassPriceRate\":\"1.0\"}";


        String result = mockMvc.perform(delete("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertNull(priceConfigRepository.findById(id));
        assertEquals(0, priceConfigRepository.findAll().size());
        assertEquals(new Response<>(1, "Delete success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }
}
