package fdse.basic.integration;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
import fdse.basic.BasicApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test class for GET /api/v1/basicservice/basic/{stationName} endpoint.
 * This endpoint sends requests to ts-station-service to retrieve the stationId for the given station name.
 *
 * I changed the directory /src/main/java/fdse.microservice and /test/java/fdse.microservice to /src/main/java/fdse.basic and /test/java/fdse.basic
 * to avoid configuration problems with ts-station-name, because they have the same directory name.
 */
@SpringBootTest
@ContextConfiguration
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetBasicTest
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
    public static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @BeforeAll
    static void setup() {
        stationServiceMongoDBContainer.start();
        stationServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
    }



    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test case to retrieve the stationId with a valid station name, so a station with the given name exists in the repository of ts-station-service.
     * This test verifies that the service returns the correct response:
     * Response<>(1, "Success", "shanghai")
     */
    @Order(1)
    @Test
    void validTestGetAllObjects() throws Exception {
        String stationName = "Shang Hai";

        Response<String> expectedResponse = new Response<>(1, "Success", "shanghai");

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case to retrieve the stationId with a valid station name, so a station with the given name exists in the repository of ts-station-service.
     * This test verifies that the service returns the correct response:
     * Response<>(1, "Success", "jiaxingnan")
     */
    @Order(2)
    @Test
    void validTestGetAllObjects2() throws Exception {
        String stationName = "Jia Xing Nan";

        Response<String> expectedResponse = new Response<>(1, "Success", "jiaxingnan");

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case to retrieve the stationId with a non-existing station name, so no station with the given name exists in the repository of ts-station-service.
     * This test verifies that the service returns the correct response when a non-existent station name is provided:
     * Response<>(0, "Not exists", stationName)
     */
    @Order(3)
    @Test
    void validTestGetZeroObjects() throws Exception {
        String stationName = "NotExisting";

        Response<String> expectedResponse = new Response<>(0, "Not exists", stationName);

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(expectedResponse, JSONObject.parseObject(actualResponse, Response.class));
    }

    /*
     * Test case when one container in the chain stops working, and so the request chain is interrupted.
     * The test verifies that the response is null.
     */
    @Order(4)
    @Test
    void testStationServiceStops() throws Exception {
        String stationName = "Jia Xing Nan";

        stationServiceContainer.stop();

        String actualResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/basicservice/basic/{stationName}", stationName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response(null, null, null), JSONObject.parseObject(actualResponse, Response.class));
    }
}
