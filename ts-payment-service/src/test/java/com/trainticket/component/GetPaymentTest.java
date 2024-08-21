package com.trainticket.component;

import com.alibaba.fastjson.JSONObject;
import com.trainticket.entity.Payment;
import com.trainticket.repository.PaymentRepository;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs all payment objects from the payment repository. As this endpoint has no body or argument, there is
 * only a few cases to test. It interacts only with the database, which is why we need to setup a MongoDBContainer for the
 * repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetPaymentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.15")
            .withExposedPorts(27017);

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    void beforeEach() {
        paymentRepository.deleteAll();
    }

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
    * This is to test if the GET endpoint can retrieve a large amount of objects, which is why the repository is filled
    * with non-specific objects. For the return response we have to check if the data list has the same amount of objects
    * and for the success message.
    */
    @Test
    void validTestGetAllObjects() throws Exception {
        for (int i = 0; i < 10000; i++) {
            paymentRepository.save(new Payment());
        }

        String result = mockMvc.perform(get("/api/v1/paymentservice/payment")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Payment> payments = (List<Payment>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(10000, payments.size());
        assertEquals(new Response<>(1, "Query Success", payments), JSONObject.parseObject(result, Response.class));
    }

    /*
    * A typical defect for a GET request is to not even have any data in the repository, so it should return nothing.
    * This is tested here.
    */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/paymentservice/payment")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No Content", null), JSONObject.parseObject(result, Response.class));
    }
}
