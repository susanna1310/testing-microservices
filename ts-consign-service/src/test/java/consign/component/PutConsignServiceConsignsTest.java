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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to update a consign record based on a given consign via PUT.
 * It communicates with the ts-consign-price-service to update the consign price if needed.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the PUT request.
 * - Body variable specific test cases
 */
public class PutConsignServiceConsignsTest extends BaseComponentTest
{

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
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

	/*
	* The test verifies the functionality of updating a consign object through a PUT request .
	* It ensures that when a valid consign object is updated, the endpoint correctly processes the request, updates the corresponding consign record in the repository,
	* and returns the updated consign record.
	*/
	@Test
	void validTestCorrectObject() throws Exception {
		Consign consign = createSampleConsign();
		ConsignRecord consignRecord = createSampleConsignRecord();
		consignRecord.setAccountId(consign.getAccountId());
		consignRecord.setId(consign.getId());
		consignRecord.setOrderId(consign.getOrderId());
		consignRepository.save(consignRecord);
		consign.setConsignee("updatedConsignee");
		consignRecord.setConsignee("updatedConsignee");

		String jsonRequest = objectMapper.writeValueAsString(consign);

		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignRecord> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignRecord.class));
		Assertions.assertEquals(new Response<>(1, "Update consign success", consignRecord), response);
	}

	/*
	* The test verifies the behavior of the endpoint when attempting to perform a PUT request with multiple consign objects provided in the request payload.
	* It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	*/
	@Test
	void invalidTestMultipleObjects() throws Exception {
		Consign consign1 = createSampleConsign();
		Consign consign2 = createSampleConsign();

		String jsonRequest = objectMapper.writeValueAsString(Arrays.asList(consign1, consign2));

		mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	* The test verifies the behavior of the endpoint when a PUT request is made with a malformed or null object in the request payload.
	* It expects the endpoint to return a 400 Bad Request status code, indicating that the request body does not conform to the expected format or is missing essential data.
	*/
	@Test
	void invalidTestMalformedObject() throws Exception {
		String jsonRequest = objectMapper.writeValueAsString(null);
		mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	* The test verifies the behavior of the endpoint when a PUT request is made without any object in the request payload.
	* It expects the endpoint to return a 400 Bad Request status code, indicating that the request body is missing, and thus cannot be processed as expected.
	*/
	@Test
	void invalidTestMissingObject() throws Exception {
		mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

	/*
	 * The test verifies the functionality of updating a consign object through a PUT request, while communicating with the ts-consin-price-service to update the price.
	 * It ensures that when a valid consign object is updated, the endpoint correctly processes the request, updates the corresponding consign record in the repository,
	 * and returns the updated consign record with a success message.
	 */
	@Test
	void validTestCorrectObjectNewWeight() throws Exception {
		Consign consign = createSampleConsign();
		ConsignRecord consignRecord = createSampleConsignRecord();
		consignRecord.setAccountId(consign.getAccountId());
		consignRecord.setId(consign.getId());
		consignRecord.setOrderId(consign.getOrderId());
		consignRecord.setWeight(2.0);
		consignRepository.save(consignRecord);
		consignRecord.setWeight(1.0);
		consignRecord.setPrice(3.0);

		Response<Double> responseTrainFood = new Response<>(1, "Success", 3.0);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/"
				+ consign.getWeight() + "/" + consign.isWithin()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		String jsonRequest = objectMapper.writeValueAsString(consign);
		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignRecord> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignRecord.class));;
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "Update consign success", consignRecord), response);
	}
}