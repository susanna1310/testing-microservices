package security.component;

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
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint GETs all securityConfig objects from the repository. As this endpoint has no body or argument, there is
 * only a two equivalence classes to test. It interacts only with the database, which is why we need to setup a
 * MongoDBContainer for the repository.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class GetSecurityConfigsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityRepository securityRepository;

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
        securityRepository.deleteAll();
    }

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * With the first equivalence class we test the outcome when we get a list of securityConfigs back. For that we simply need
     * to insert them into the repository before the request
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        for (int i = 0; i < 10000; i++) {
            SecurityConfig securityConfig = new SecurityConfig();
            securityConfig.setId(UUID.randomUUID());
            securityRepository.save(securityConfig);
        }

        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        List<SecurityConfig> configs = (List<SecurityConfig>) (JSONObject.parseObject(result, Response.class).getData());
        assertEquals(10000, configs.size());
        assertEquals(new Response<>(1, "Success", configs), JSONObject.parseObject(result, Response.class));

    }

    /*
     * For the second equivalence class test we do not get any content back. This can be achieved by leaving the repository
     * empty.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        String result = mockMvc.perform(get("/api/v1/securityservice/securityConfigs")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(new Response<>(0, "No Content", null), JSONObject.parseObject(result, Response.class));
    }

}
