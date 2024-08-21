package consign.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import consign.entity.Consign;
import consign.entity.ConsignRecord;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to create a consign record based on a given consign via POST.
 * It communicates with the ts-consign-price-service to update the consign price.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 * - URL parameter-specific test cases.
 */
public class PostConsignServiceConsignsTest  extends BaseComponentTest{

	@Autowired
	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	@BeforeEach
	public void setUp() {
		mockServer = MockRestServiceServer.createServer(restTemplate);
		consignRepository.deleteAll();
	}

	private final String url = "/api/v1/consignservice/consigns";


	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test verifies the functionality of creating a consign object through a POST request.
	 * It ensures that the endpoint correctly processes the request, updates the corresponding consign record in the repository,
	 * and returns the updated consign record.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		Consign consign = createSampleConsign();
		ConsignRecord consignRecord = new ConsignRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
						"place_from", "place_to", "consignee", "10001", 1.0, 3.0);
		consignRecord.setOrderId(consign.getOrderId());
		consignRecord.setAccountId(consign.getAccountId());

		Response<Double> responseTrainFood = new Response<>(1, "Success", 3.0);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/"
				+ consign.getWeight() + "/" + consign.isWithin()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));


		String jsonRequest = objectMapper.writeValueAsString(consign);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignRecord> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignRecord.class));
		consignRecord.setId(response.getData().getId());
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "You have consigned successfully! The price is " + consignRecord.getPrice(), consignRecord), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple consign objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		Consign consign  = createSampleConsign();
		String jsonRequest = objectMapper.writeValueAsString(Arrays.asList(consign, consign));

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	 * The test verifies the behavior of the endpoint when a POST request is made with a malformed or null object in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that the request body does not conform to the expected format or is missing essential data.
	 */
	@Test
	void invalidTestMalformedObject() throws Exception {
		String jsonRequest = objectMapper.writeValueAsString(null);

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	 * The test verifies the behavior of the endpoint when a POST request is made without any object in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that the request body is missing, and thus cannot be processed as expected.
	 */
	@Test
	void invalidTestMissingObject() throws Exception {
		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

}