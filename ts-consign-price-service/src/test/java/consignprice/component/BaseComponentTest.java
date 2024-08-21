package consignprice.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import consignprice.entity.ConsignPrice;
import consignprice.repository.ConsignPriceConfigRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers

public abstract class BaseComponentTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ConsignPriceConfigRepository consignPriceConfigRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest").withExposedPorts(27017);


    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();

    }

    @BeforeEach
    public void setUp() {
        consignPriceConfigRepository.deleteAll();
    }

    protected ConsignPrice createSampleConsignPrice() {
        ConsignPrice consignPrice = new ConsignPrice();
        consignPrice.setId(UUID.randomUUID());
        consignPrice.setIndex(0);
        consignPrice.setInitialPrice(10.0);
        consignPrice.setInitialWeight(5.0);
        consignPrice.setWithinPrice(2.0);
        consignPrice.setBeyondPrice(3.0);
        return consignPrice;
    }
}
