package travel2.integration;

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
import travel2.entity.TrainType;
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.repository.TripRepository;

import java.util.Date;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/travel2service/train_types/{tripId} endpoint.
 * This endpoint sends a request to ts-train-service to retrieve the trainType with the given tripId.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetTrainTypesTripIdTravel2ServiceTest {
    private static final Network network = Network.newNetwork();
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.0")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");
    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");
    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);
    private final String url = "/api/v1/travel2service/train_types/{tripId}";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TripRepository tripRepository;
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
     * The test is designed to verify that the endpoint for retrieving the train type works correctly, for a valid ID with a trip that exists in the database and a matching train type.
     * It ensures that the endpoint returns a successful response with the appropriate message and data is not empty.
     */
    @Test
    void validTestGetTrainTypeSuccess() throws Exception {
        String tripId = "Z1234";

        mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success query Train by trip id"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type works correctly, for a valid ID with a trip that exists in the database and a matching train type.
     * It ensures that the endpoint returns a successful response with the correct trainType.
     */
    @Test
    void validTestGetTrainTypeCorrectly() throws Exception {
        String tripId = "Z1234";

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TrainType trainType = new TrainType("ZhiDa", Integer.MAX_VALUE, Integer.MAX_VALUE, 120);
        Response<TrainType> response = objectMapper.readValue(result, new TypeReference<Response<TrainType>>() {
        });
        Assertions.assertEquals(trainType.getId(), response.getData().getId());
        Assertions.assertEquals(trainType.getConfortClass(), response.getData().getConfortClass());
        Assertions.assertEquals(trainType.getEconomyClass(), response.getData().getEconomyClass());
        Assertions.assertEquals(trainType.getAverageSpeed(), response.getData().getAverageSpeed());
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when there is no trip associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String tripId = "K1235";
        // Not existing trip with that tripId in tripRepository

        String result = mockMvc.perform(get(url, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<TrainType> response = objectMapper.readValue(result, new TypeReference<Response<TrainType>>() {
        });
        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }

    /*
     * The test is designed to verify that the endpoint for retrieving the train type correctly handles the case
     * when there is no train type that matches the trip. It ensures that the endpoint returns a response with the appropriate message and no content.
     */
    @Test
    void validTestGetNoTrainType() throws Exception {
        Trip trip = new Trip(new TripId("K1234"), "trainTypeId", "stationA", "stations", "stationB", new Date(), new Date());
        // Saving new trip in tripRepository with a not known trainTypeId in train service
        tripRepository.save(trip);

        String result = mockMvc.perform(get(url, trip.getTripId())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Response<TrainType> response = objectMapper.readValue(result, new TypeReference<Response<TrainType>>() {
        });

        Assertions.assertEquals(new Response<>(0, "No Content", null), response);
    }
}
