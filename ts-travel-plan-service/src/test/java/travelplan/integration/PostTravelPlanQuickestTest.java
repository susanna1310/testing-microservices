package travelplan.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travelplan.entity.TripInfo;

import java.util.Date;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration test for the POST Travel Plan Quickest in the TravelPlanService.
 *
 * Following service are connected to this Endpoint:
 * - station-service
 * - seat-service
 * - ticketinfo-service
 * - travel-service
 * - travel2-service
 * - route-plan-service
 *
 * Test containers are used to create real service instances for testing.
 *
 * Endpoint: POST /api/v1/travelplanservice/travelPlan/quickest
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PostTravelPlanQuickestTest {
    private final static Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer travel2MongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");
    @Container
    private static final GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2MongoDBContainer);
    @Container
    private static final MongoDBContainer travelMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    private static final GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelMongoDBContainer);
    @Container
    private static final GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");
    @Container
    private static final GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");
    @Container
    private static final GenericContainer<?> routePlanContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-plan-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14578)
            .withNetwork(network)
            .withNetworkAliases("ts-route-plan-service");
    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");
    @Container
    private static final GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);
    private final String url = "/api/v1/travelplanservice/travelPlan/quickest";
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts-station-service.url", stationServiceContainer::getHost);
        registry.add("ts-station-service.port", () -> stationServiceContainer.getMappedPort(12345));

        registry.add("ts-seat-service.url", seatServiceContainer::getHost);
        registry.add("ts-seat-service.port", () -> seatServiceContainer.getMappedPort(18898));

        registry.add("ts-ticketinfo-service.url", ticketinfoServiceContainer::getHost);
        registry.add("ts-ticketinfo-service.port", () -> ticketinfoServiceContainer.getMappedPort(15681));

        registry.add("ts-travel2-service.url", travel2ServiceContainer::getHost);
        registry.add("ts-travel2-service.port", () -> travel2ServiceContainer.getMappedPort(16346));

        registry.add("ts-travel-service.url", travelServiceContainer::getHost);
        registry.add("ts-travel-service.port", () -> travelServiceContainer.getMappedPort(12346));

        registry.add("ts-route-plan-service.url", routePlanContainer::getHost);
        registry.add("ts-route-plan-service.port", () -> routePlanContainer.getMappedPort(14578));
    }

    /*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * Test case for a valid request with correct object. Check if the response status is ok and the response body is as expected.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception {
        TripInfo tripInfo = new TripInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"));

        String jsonRequest = objectMapper.writeValueAsString(tripInfo);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * Test case for a valid request with correct object but no found trip. Check if the response status is ok and the response body is as expected.
     */
    @Test
    @Order(2)
    void validTestNoTrips() throws Exception {
        TripInfo tripInfo = new TripInfo("Berlin", "Munich", new Date("Mon May 04 09:00:00 GMT+0800 2025"));

        String jsonRequest = objectMapper.writeValueAsString(tripInfo);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.msg").value("Cannot Find"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /*
     * Test case for an invalid request with service unavailable.
     * This test fails, because the implementation does not handle the case.
     */
    @Test
    @Order(3)
    void invalidTestServiceUnavailable() throws Exception {
        TripInfo tripInfo = new TripInfo("Berlin", "Munich", new Date("Mon May 04 09:00:00 GMT+0800 2025"));

        String jsonRequest = objectMapper.writeValueAsString(tripInfo);

        routePlanContainer.stop();

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.msg").value("Service unavailable"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
