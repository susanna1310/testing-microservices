package consign.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import consign.entity.Consign;
import consign.entity.ConsignRecord;
import consign.repository.ConsignRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
public abstract class BaseIntegrationTest {

    protected final static Network network = Network.newNetwork();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ConsignRepository consignRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-service");


    @Container
    public static final MongoDBContainer consignPriceServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-mongo");

    @Container
    public static GenericContainer<?> consignPriceContainer = new GenericContainer<>(DockerImageName.parse("local/ts-consign-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16110)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-service")
            .dependsOn(consignPriceServiceMongoDBContainer);


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.consign.price.service.url", consignPriceContainer::getHost);
        registry.add("ts.consign.price.service.port", () -> consignPriceContainer.getMappedPort(16110));

        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017).toString());
    }

    @BeforeEach
    public void setUp() {
        consignRepository.deleteAll();
    }

    protected Consign createSampleConsign() {
        return new Consign(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, true);
    }


    protected ConsignRecord createSampleConsignRecord() {
        return new ConsignRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, 3.0);
    }
}
