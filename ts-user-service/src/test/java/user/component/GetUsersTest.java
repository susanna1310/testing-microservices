package user.component;

import com.alibaba.fastjson.JSONObject;
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
import user.entity.User;
import user.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs all User objects from the user repository. As this endpoint has no body or argument, there is
 * only two equivalence classes to test. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
public class GetUsersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        userRepository.deleteAll();
    }


	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * With the first equivalence class we test the outcome when we get a list of users back. For that we simply need
     * to insert them into the repository before the request
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        for (int i = 0; i < 10000; i++) {
            User user = new User();
            user.setUserId(UUID.randomUUID());
            userRepository.save(user);
        }

        String result = mockMvc.perform(get("/api/v1/userservice/users")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<User> users = (List<User>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(10000, users.size());
        assertEquals(new Response<>(1, "Success", users), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the second equivalence class test we do not get any content back. This can be achieved by leaving the repository
     * empty.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/userservice/users")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "NO User", null), JSONObject.parseObject(result, Response.class));
    }
}
