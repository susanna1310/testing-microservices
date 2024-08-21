package inside_payment.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import inside_payment.entity.AccountInfo;
import inside_payment.entity.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to create new money with the given account information via POST request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostInsidePayServiceInsidePaymentAccountTest extends BaseComponentTest
{
	private final String url = "/api/v1/inside_pay_service/inside_payment/account";

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating new money works correctly, for valid account information.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectObject() throws Exception{
		AccountInfo info = createSampleAccountInfo();
		String jsonRequest = objectMapper.writeValueAsString(info);
		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(1, "Create Account Success", null), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple account information objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		AccountInfo info = createSampleAccountInfo();
		AccountInfo[] infos = {info, info};
		String jsonRequest = objectMapper.writeValueAsString(infos);

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	 * The test is designed to verify that the endpoint for creating new money correctly handles the case
	 * when there already exists a money account with the same user ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void invalidTestDuplicateObject() throws Exception {
		Money money = createSampleMoney();
		addMoneyRepository.save(money);

		AccountInfo info = createSampleAccountInfo();
		String jsonRequest = objectMapper.writeValueAsString(info);
		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(0, "Create Account Failed, Account already Exists", null), response);
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