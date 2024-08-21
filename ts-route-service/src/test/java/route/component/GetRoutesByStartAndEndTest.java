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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs a list of routes from the route repository, which have the given startId and terminalId in their station list.
 * Which is why we need to test defect cases for the URL parameter as well as the equivalence classes for the URL parameters
 * and any specific defect tests for the endpoint. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetRoutesByStartAndEndTest {

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
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * This is to test if the GET endpoint can retrieve a large amount of objects, which is why the repository is filled
     * with objects with the ids in their station list. For the return response we have to check if the data list has
     * the same amount of objects and for the success message.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        List<String> stations = new ArrayList<>();
        stations.add("1");
        stations.add("2");
        for (int i = 0; i < 10000; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<Route> routes = (List<Route>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(10000, routes.size());
        assertEquals(new Response<>(1, "Success", routes), JSONObject.parseObject(result, Response.class));

    }

    /*
     * A typical defect for a GET request is to not even have any data in the repository, so it should return nothing.
     * This is tested here.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No routes with the startId and terminalId", null), JSONObject.parseObject(result, Response.class));

    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Now we test the defect case, where no station with the id exists in any of the stationLists, so it should return
     * nothing.
     */
    @Test
    void invalidTestNonexistingId() throws Exception {
        List<String> stations = new ArrayList<>();
        stations.add("1");
        stations.add("2");
        for (int i = 0; i < 10; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }
        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "3")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No routes with the startId and terminalId", null), JSONObject.parseObject(result, Response.class));

    }

    /*
     * In this test we give a malformed url parameter as the request argument, which causes a 4xx error, because it is
     * an error from the client side, which can happen frequently for such URL parameter requests.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1/2/3", "1/2/3\"")
                )
                .andExpect(status().is4xxClientError());
    }

    /*
     * Now we test the defect case, where no url parameter is passed, which is also a case which can happen like the test above.
     * But the expected response in this case is an exception.
     */
    @Test
    void invalidTestMissingObject() {
        assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}")
        );});
    }

    /*
     * This test takes more than one url parameter, but as only the first one will be used, this test is still valid.
     * This test is important because in contrast to endpoints with body arguments, it won't fail with more than one argument.
     */
    @Test
    void validTestMultipleIds() throws Exception {

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "2", "3")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        assertEquals(new Response<>(0, "No routes with the startId and terminalId", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * As the id as URL parameter is a String with no length restrictions etc, there are only a few possibilities for equivalence
     * classes as they are either valid or cause an exception like some tests above. Which is why we test an unusual id
     * as a representative for the valid case.
     */
    @Test
    void validTestUnusualId() throws Exception {

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "()$0129&!.,hallo)", "()$0129&!.,hallo)")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No routes with the startId and terminalId", null), JSONObject.parseObject(result, Response.class));
    }

    /*
     * This is to test a defect, where for some routes in the repository the startId is after the terminalId in the stationList,
     * which basically means that the route is the opposite direction. The endpoint should therefore not return these routes.
     */
    @Test
    void validTestSwitchedStations() throws Exception {
        List<String> stations = new ArrayList<>();
        stations.add("1");
        stations.add("2");
        for (int i = 0; i < 5; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }
        stations.remove(0);
        stations.add("1");
        assertEquals(1, stations.indexOf("1"));
        for (int i = 5; i < 10; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        String result = mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "2")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<Route> routes = (List<Route>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(5, routes.size());
        assertEquals(new Response<>(1, "Success", routes), JSONObject.parseObject(result, Response.class));

    }
}
