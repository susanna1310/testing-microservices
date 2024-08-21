package food.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import food.repository.FoodStoreRepository;
import food.repository.TrainFoodRepository;
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

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers

public abstract class BaseComponentTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FoodStoreRepository foodStoreRepository;

    @Autowired
    protected TrainFoodRepository trainFoodRepository;

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
        foodStoreRepository.deleteAll();
        trainFoodRepository.deleteAll();
    }
}
