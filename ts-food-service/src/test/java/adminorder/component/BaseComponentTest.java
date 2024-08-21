package adminorder.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import foodsearch.FoodApplication;
import foodsearch.entity.FoodOrder;
import foodsearch.repository.FoodOrderRepository;
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

@SpringBootTest(classes = { FoodApplication.class })
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
public abstract class BaseComponentTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FoodOrderRepository foodOrderRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);


    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    public void setUp() {
        foodOrderRepository.deleteAll();
    }

    protected FoodOrder createSampleFoodOder() {
        return new FoodOrder(UUID.randomUUID(), UUID.randomUUID(), 2, "station_name", "store_name", "food_name", 3.0);
    }
}
