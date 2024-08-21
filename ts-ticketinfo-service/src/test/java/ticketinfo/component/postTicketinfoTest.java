package ticketinfo.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;



/*
 * This endpoint POST /api/v1/ticketinfoservice/ticketinfo is only a proxy to the basic service, so we only need to
 * test the connection to the basic service.
 * The request is simply forwarded to the basic service and the response is returned.
 * The basic service is tested in its own component tests.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
public class postTicketinfoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /*
     * Test with an empty response of the basic service.
     */
    @Test
    void validTestPostZeroObjects() throws Exception {
        String requestBody = "";

        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(""));

        mockServer.verify();
    }

    /*
     * Test the endpoint with valid info. Expect a success response.
     */
    @Test
    void validTestExistingTravelInfo() throws Exception {
        String requestBody = "{\"field\": \"value\"}";
        String responseBody = "{\"result\":\"success\"}";

        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(responseBody));

        mockServer.verify();
    }

    /*
     * Test the endpoint with a malformed JSON. Expect a Bad Request.
     */
    @Test
    void invalidTestMalformedJson() throws Exception {
        String malformedRequestBody = "{field: \"value\"}";

        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(malformedRequestBody))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content(malformedRequestBody))
                .andExpect(status().isBadRequest());
    }

    /*
     * Test the endpoint with an invalid travel info structure. Expect a Bad Request.
     */
    @Test
    void invalidTestInvalidTravelInfo() throws Exception {
        String invalidRequestBody = "{\"invalidField\": \"value\"}";

        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(invalidRequestBody))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ticketinfoservice/ticketinfo")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());

        mockServer.verify();
    }
}
