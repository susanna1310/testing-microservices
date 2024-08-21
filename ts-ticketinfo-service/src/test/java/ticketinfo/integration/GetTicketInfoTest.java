package ticketinfo.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/ticketinfoservice/ticketinfo/{name} endpoint.
 * This endpoint sends a request to ts-basic-service to retrieve the stationId to the given stationName.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetTicketInfoTest
{
    @Autowired
    private MockMvc mockMvc;

    private static final Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    private static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @BeforeAll
    static void setUp() {
        stationServiceMongoDBContainer.start();
        stationServiceContainer.start();
        basicServiceContainer.start();
    }

    @DynamicPropertySource
    private static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port",() -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
    }

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case for a request with a valid stationName, so a station with the given name exists in the repository of ts-station-service.
     * The test verifies that the correct response is being returned:
     * Response<>(1, "Success", "shanghai")
     */
    @Test
    @Order(1)
    void validTestGetStationId() throws Exception {
        String stationName = "Shang Hai";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").value("shanghai"));
    }

    /*
     * Test case for a request with a valid stationName and a station with the given name exists in the repository of ts-station-service.
     * The test verifies that the correct response is being returned:
     * Response<>(1, "Success", "shijiazhuang")
     */
    @Test
    @Order(2)
    void validTestGetStationId2() throws Exception {
        String stationName = "Shi Jia Zhuang";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").value("shijiazhuang"));
    }

    /*
     * Test case for a valid stationName and no station with the given name exists in the repository of ts-station-service.
     * The test verifies that the correct response is being returned:
     * Response<>(0, "Not exists", "Muenchen")
     */
    @Test
    @Order(3)
    void validTestGetZeroObjects() throws Exception {
        String notExistingName = "Muenchen";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/{name}", notExistingName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.msg").value("Not exists"))
                .andExpect(jsonPath("$.data").value(notExistingName));
    }

    /*
     * Test case when one container stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Test
    @Order(4)
    void testStationContainerStopped() throws Exception {
        String stationName = "Shang Hai";

        stationServiceContainer.stop();

        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(null, null, null), JSONObject.parseObject(result, Response.class));
    }
}