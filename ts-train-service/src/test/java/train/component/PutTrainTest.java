package train.component;

import com.alibaba.fastjson.JSONObject;
import train.entity.TrainType;
import train.repository.TrainTypeRepository;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This is a test class for the PUT endpoint of the Train Service. It is used to test the update of a existing train type.
 * (/api/v1/trainservice/trains)
 * It interacts only with the database, which is why we need to setup a MongoDBContainer for the repository.
 *
 * REMARK: The PUT endpoint is not implemented correctly. The PUT endpoint should be used to update an existing object.
 * It creates a new object instead. This is why the test cases are not working.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PutTrainTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TrainTypeRepository trainTypeRepository;

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
            trainTypeRepository.deleteAll();
        }

	/*
	#####################################
	# Method (PUT) specific test cases #
	#####################################
	*/

        /*
         * Test if a valid object can be updated. And the object is udpated in the repository.
         */
        @Test
        void validTestCorrectObject() throws Exception {
            TrainType trainType = new TrainType("123", 100, 50);
            trainTypeRepository.save(trainType);

            TrainType updatedTrainType = new TrainType("123", 150, 80);
            String updatedTrainTypeJson = JSONObject.toJSONString(updatedTrainType);

            String result = mockMvc.perform(put("/api/v1/trainservice/trains")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedTrainTypeJson))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Response expectedResponse = new Response(1, "update success", true);
            assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));

            TrainType updatedFromRepo = trainTypeRepository.findById("123").orElse(null);

            assertNotNull(updatedFromRepo);
            assertEquals(150, updatedFromRepo.getEconomyClass());
            assertEquals(80, updatedFromRepo.getConfortClass());
        }


        /*
         * Test if an non existing object cannot be created. Ensure that the object is not stored in the repository.
         * REMARK: The implementation of the PUT endpoint is incorrect. This is why the test fails.
         * */
        @Test
        void invalidMissingObject() throws Exception {
            TrainType updatedTrainType = new TrainType("999", 150, 80);
            String updatedTrainTypeJson = JSONObject.toJSONString(updatedTrainType);

            String result = mockMvc.perform(put("/api/v1/trainservice/trains")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedTrainTypeJson))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Response expectedResponse = new Response(0, "there is no trainType with the trainType id", false);
            assertEquals(expectedResponse, JSONObject.parseObject(result, Response.class));
            assertFalse(trainTypeRepository.findById("999").isPresent());
        }

        /*
         * Test if an object with a malformed object cannot be updated. Ensure that the object is not updated in the repository.
         */
        @Test
        void invalidTestMalformedObject() throws Exception {
            TrainType trainType = new TrainType("123", 100, 50);
            trainTypeRepository.save(trainType);
            String malformedJson = "{ \"id\": \"123\", \"economyClass\": \"not-an-integer\" }";

            mockMvc.perform(put("/api/v1/trainservice/trains")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());

            TrainType notUpdatedFromRepo = trainTypeRepository.findById("123").orElse(null);
            assertNotNull(notUpdatedFromRepo);
            assertEquals(100, notUpdatedFromRepo.getEconomyClass());
            assertEquals(50, notUpdatedFromRepo.getConfortClass());
        }


        /*
         * Test if an object with a missing body cannot be used. Ensure that the object is not updated in the repository.
         */
        @Test
        void invalidTestMissingBody() throws Exception {
            TrainType trainType = new TrainType("123", 100, 50);
            trainTypeRepository.save(trainType);

            mockMvc.perform(put("/api/v1/trainservice/trains")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            TrainType notUpdatedFromRepo = trainTypeRepository.findById("123").orElse(null);
            assertNotNull(notUpdatedFromRepo);
            assertEquals(100, notUpdatedFromRepo.getEconomyClass());
            assertEquals(50, notUpdatedFromRepo.getConfortClass());
        }

        // No special test cases for body validation, as the body is validated by the framework.
    }
