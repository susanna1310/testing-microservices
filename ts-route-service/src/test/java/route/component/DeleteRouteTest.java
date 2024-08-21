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
import route.entity.Route;
import route.repository.RouteRepository;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This DELETE endpoint takes an id as an URL parameter and deletes the corresponding Route object from the routeRepository
 * regardless if it exists. As such we test delete specific REST defect tests, url parameter specific equivalence class
 * based tests as well as defect tests. It interacts only with the database, which is why we need to setup a MongoDBContainer for the
 * repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class DeleteRouteTest {

    @Autowired
    private MockMvc mockMvc;

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
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

    /*
     * In this test we test the general case, where the url parameter id is a valid id of a Route object in the repository,
     * which means the deletion is executed normally.
     */
    @Test
    void validTestCorrectObject() throws Exception {
        Route route = new Route();
        routeRepository.save(route);
        assertTrue(routeRepository.findById(route.getId()).isPresent());

        String result = mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", route.getId())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertFalse(routeRepository.findById(route.getId()).isPresent());
        assertEquals(new Response<>(1, "Delete Success", route.getId()), JSONObject.parseObject(result, Response.class));

    }

    /*
     * This test takes more than one url parameter, but as only the first one will be used, this test is still valid.
     * This test is important because in contrast to endpoints with body arguments, it won't fail with more than one argument.
     */
    @Test
    void validTestMultipleIds() throws Exception {

        String result = mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", "1", "2", "3")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(1, "Delete Success", "1"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * In this test we give a malformed url parameter as the request argument, which causes a 4xx error, because it is
     * an error from the client side, which can happen frequently for such URL parameter requests.
     */
    @Test
    void invalidTestMalformedObject() throws Exception {

        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", "1/2/3")
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no url parameter is passed, which is also a case which can happen like the test above.
     * But the expected response in this case is an exception.
     */
    @Test
    void invalidTestMissingBody() {

        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}")
                );});
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Now we test the defect case, where no object with the id exists. But as this endpoint performs the deletion without checking
     * for the id in the repository, the response is the same as if it existed.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        assertFalse(routeRepository.findById("1").isPresent());

        String result = mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", "1")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertFalse(routeRepository.findById("1").isPresent());
        assertEquals(new Response<>(1, "Delete Success", "1"), JSONObject.parseObject(result, Response.class));
    }

    /*
     * As the id as URL parameter is a String with no length restrictions etc, there are no possibilities for equivalence
     * classes as they are either valid or cause an exception like some tests above. Which is why we test an unusual id
     * as a representative for the valid case.
     */
    @Test
    void validTestUnusualId() throws Exception {

        String result = mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", "()$0129&!.,hallo)")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(1, "Delete Success", "()$0129&!.,hallo)"), JSONObject.parseObject(result, Response.class));
    }
}
