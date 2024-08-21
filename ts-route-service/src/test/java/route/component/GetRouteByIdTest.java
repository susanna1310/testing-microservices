package route.component;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import route.entity.Route;
import route.repository.RouteRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs the route object with the given id from the route repository. Which is why we need to test defect
 * cases for the URL parameter as well as the equivalence classes for the parameter and any specific defect tests for the
 * endpoint. It interacts only with the database, which is why we need to setup a MongoDBContainer for the
 * repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetRouteByIdTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RouteRepository routeRepository;

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
        routeRepository.deleteAll();
    }

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * This is the general defect test to see if the request to the endpoint can retrieve the route object in the
     * repository with the given id.
     */
    @Test
    void validTestGetObject() throws Exception {
        Route route = new Route();
        route.setTerminalStationId("test");
        routeRepository.save(route);

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}", route.getId())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        route = objectMapper.convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);
        assertEquals("test", route.getTerminalStationId());
        assertEquals(new Response<>(1, "Success", JSONObject.parseObject(result, Response.class).getData()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * A typical defect for a GET request is to not even have any data in the repository, so it should return nothing.
     * This is tested here.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}", "1")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No content with the routeId", null), JSONObject.parseObject(result, Response.class));

    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * In this test we give a malformed url parameter as the request argument, which causes a 4xx error, because it is
     * an error from the client side, which can happen frequently for such URL parameter requests.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}", "1/2/3")
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no url parameter is passed, which is also a case which can happen like the test above.
     * But the expected response in this case is an exception.
     */
    @Test
    void invalidTestMissingBody() {

        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}")
        );});
    }

    /*
     * This test takes more than one url parameter, but as only the first one will be used, this test is still valid.
     * This test is important because in contrast to endpoints with body arguments, it won't fail with more than one argument.
     */
    @Test
    void validTestMultipleIds() throws Exception {

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}", "1", "2", "3")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No content with the routeId", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * As the id as URL parameter is a String with no length restrictions etc, there are no possibilities for equivalence
     * classes as they are either valid or cause an exception like some tests above. Which is why we test an unusual id
     * as a representative for the valid case.
     */
    @Test
    void validTestUnusualId() throws Exception {

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{routeId}", "()$0129&!.,hallo)")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No content with the routeId", null), JSONObject.parseObject(result, Response.class));
    }

}
