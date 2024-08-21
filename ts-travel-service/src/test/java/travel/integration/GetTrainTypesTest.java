package travel.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import travel.entity.TrainType;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration test for the GET Train Types Endpoint in TravelService.
 *
 * Following service are connected to this Endpoint:
 * - TrainService
 *
 * Test containers are used to create real service instances for testing.
 * MongoDB is used to create a real database instance for testing.
 *
 * Endpoint: "/api/v1/travelservice/routes/{routeId}"
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetTrainTypesTest {
    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.0")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");
    @Container
    private static final GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);
    private final String url = "/api/v1/travelservice/train_types/{tripId}";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));

        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when the request is valid. It ensures that the endpoint returns a response with the appropriate message.
     */
    @Test
    void validTestGetTrainType() throws Exception {
        String tripId = "G1234";

        mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when the request is valid. It ensures that the endpoint returns a response with the appropriate content.
     */
    @Test
    void validTestGetTrainTypeDetail() throws Exception {
        String tripId = "G1234";
        TrainType expectedTrainType = new TrainType("GaoTieOne", Integer.MAX_VALUE, Integer.MAX_VALUE, 250);

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TrainType> response = objectMapper.readValue(result, new TypeReference<Response<TrainType>>() {
        });

        Assertions.assertEquals(expectedTrainType.getId(), response.getData().getId());
        Assertions.assertEquals(expectedTrainType.getConfortClass(), response.getData().getConfortClass());
        Assertions.assertEquals(expectedTrainType.getEconomyClass(), response.getData().getEconomyClass());
        Assertions.assertEquals(expectedTrainType.getAverageSpeed(), response.getData().getAverageSpeed());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when the request is valid, but the tripId does not exist in the tripRepository.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String tripId = "Non-Existing";

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TrainType> response = objectMapper.readValue(result, new TypeReference<Response<TrainType>>() {
        });
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }

}
