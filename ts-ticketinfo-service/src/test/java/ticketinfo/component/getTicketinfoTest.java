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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;



/*
 * This endpoint GET /api/v1/ticketinfoservice/ticketinfo/{name} is only a proxy to the basic service, so we only need to
 * test the connection to the basic service.
 * The request is simply forwarded to the basic service and the response is returned.
 * The basic service is tested in its own component tests.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
public class getTicketinfoTest
{

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
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test with an empty reponse of the basic service.
     */
    @Test
    void validTestGetZeroObjects() throws Exception {
        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/test")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(""));

        mockServer.verify();
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Test the endpoint with a valid id. Check if the endpoint returns the correct object.
     */
    @Test
    void validTestExistingId() throws Exception {
        String responseBody = "{\"id\":\"1\",\"name\":\"test\"}";

        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/test")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(responseBody));

        mockServer.verify();
    }

    /*
     * Test the endpoint with a non-existing id. Check if the endpoint returns a not found.
     */
    @Test
    void validTestNonCorrectFormatId() throws Exception {
        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/12345"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/12345")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());

        mockServer.verify();
    }

    /*
     * Test the endpoint with wrong characters in the id. Check if the endpoint returns a bad request.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        mockServer.expect(requestTo("http://ts-basic-service:15680/api/v1/basicservice/basic/!@#$%"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ticketinfoservice/ticketinfo/!@#$%")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        mockServer.verify();
    }
}
