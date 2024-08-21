package inside_payment.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import inside_payment.entity.AccountInfo;
import inside_payment.entity.Money;
import inside_payment.entity.Payment;
import inside_payment.entity.PaymentType;
import inside_payment.repository.AddMoneyRepository;
import inside_payment.repository.PaymentRepository;
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
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
public abstract class BaseComponentTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AddMoneyRepository addMoneyRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

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
        paymentRepository.deleteAll();
        addMoneyRepository.deleteAll();
    }

    protected Money createSampleMoney() {
        Money money = new Money();
        money.setUserId("123");
        money.setMoney("200.0");
        return money;
    }

    protected Payment createSamplePayment() {
        Payment payment = new Payment();
        payment.setOrderId(UUID.randomUUID().toString());
        payment.setPrice("100.0");
        payment.setUserId("123");
        payment.setType(PaymentType.P);
        return payment;
    }

    protected AccountInfo createSampleAccountInfo() {
        AccountInfo info = new AccountInfo();
        info.setUserId("123");
        info.setMoney("10000");
        return info;
    }
}
