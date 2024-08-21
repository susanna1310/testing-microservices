package consignprice.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import consignprice.entity.ConsignPrice;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to create or update a consign price with the index 0 via POST request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostConsignPriceTest extends BaseComponentTest{

	private final String url = "/api/v1/consignpriceservice/consignprice";

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating a new consign price works correctly, for a valid ConsignPrice.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the consign price.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();

		String jsonRequest = objectMapper.writeValueAsString(consignPrice);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignPrice> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignPrice.class));
		Assertions.assertEquals(new Response<>(1, "Success", consignPrice), response);
	}

	/*
	 * The test is designed to verify that the endpoint for updating the consign price works correctly, for a valid ConsignPrice.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectExistingObject() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();
		consignPriceConfigRepository.save(consignPrice);
		consignPrice.setBeyondPrice(4.0);

		String jsonRequest = objectMapper.writeValueAsString(consignPrice);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignPrice> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignPrice.class));
		Assertions.assertEquals(new Response<>(1, "Success", consignPrice), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple consign price objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();
		ConsignPrice[] array = {consignPrice, consignPrice};
		String jsonRequest = objectMapper.writeValueAsString(array);

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
	void invalidTestMissingBody() throws Exception {
		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}
}