package price.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import price.entity.PriceConfig;
import price.repository.PriceConfigRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs the priceConfig from the repository, which has the given routeId and trainType.
 * Which is why we need to test defect cases for the URL parameter as well as the equivalence classes for the URL parameters
 * and any specific defect tests for the endpoint. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetPriceForRouteTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

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
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * This is the general equivalence class test to see if the request to the endpoint can retrieve the route object in the
     * repository with the given valid parameters.
     */
    @Test
    void validTestGetObject() throws Exception {
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setRouteId("1");
        priceConfig.setTrainType("2");
        priceConfigRepository.save(priceConfig);

        String result = mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        priceConfig = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), PriceConfig.class);
        assertEquals("1", priceConfig.getRouteId());
        assertEquals("2", priceConfig.getTrainType());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));
    }

    /*
     * A typical test for a GET request is to not even have any data in the repository, so it should return nothing.
     * This is tested here.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
    }
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Now we test the equivalence class case, where no priceConfig with the two exact ids exists in the repository, so it should return
     * nothing. We also switch them around for some objects.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        for (int i = 0; i < 5; i++) {
            PriceConfig priceConfig = new PriceConfig();
            priceConfig.setId(UUID.randomUUID());
            priceConfig.setRouteId("1");
            priceConfig.setTrainType("3");
            priceConfigRepository.save(priceConfig);
        }
        for (int i = 0; i < 5; i++) {
            PriceConfig priceConfig = new PriceConfig();
            priceConfig.setId(UUID.randomUUID());
            priceConfig.setRouteId("2");
            priceConfig.setTrainType("1");
            priceConfigRepository.save(priceConfig);
        }
        String result = mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));

    }


    /*
     * In this test we give a malformed url parameter as the request argument, which causes a 4xx error, because it is
     * an error from the client side, which can happen frequently for such URL parameter requests.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1/2/3", "3/4/5")
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the case, where no url parameter is passed, which is also a case which can happen like the test above.
     * But the expected response in this case is an exception.
     */
    @Test
    void invalidTestMissingBody() {

        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}")
        );});
    }

    /*
     * This test takes more than two url parameter, but as only the first one will be used, this test is still valid.
     * This test is important because in contrast to endpoints with body arguments, it won't fail with more than two arguments.
     */
    @Test
    void validTestMultipleIds() throws Exception {

        String result = mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2", "3")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * As the id as URL parameter is a String with no length restrictions etc, there are no possibilities for equivalence
     * classes as they are either valid or cause an exception like some tests above. Which is why we test an unusual id
     * as a representative for the valid case.
     */
    @Test
    void validTestUnusualId() throws Exception {

        String result = mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "()$0129&!.,hallo)", "()$0129&!.,hallo")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No that config", null), JSONObject.parseObject(result, Response.class));
    }



    /*
     * In this defect test, we save several objects with the same routeId and trainType in the repository. As this endpoint,
     * only returns one priceConfig we want to check how it would handle the defect case where there are more than one
     * priceConfig even if it should not happen. This causes an exception as expected.
     */
    @Test
    void duplicateIdTest() {
        for (int i = 0; i < 5; i++) {
            PriceConfig priceConfig = new PriceConfig();
            priceConfig.setId(UUID.randomUUID());
            priceConfig.setRouteId("1");
            priceConfig.setTrainType("2");
            priceConfigRepository.save(priceConfig);
        }
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2")
            );
        });
    }
}