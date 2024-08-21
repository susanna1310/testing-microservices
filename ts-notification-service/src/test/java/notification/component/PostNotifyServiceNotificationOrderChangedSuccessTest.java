package notification.component;

import notification.entity.NotifyInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to send an email notification for an order changed success
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostNotifyServiceNotificationOrderChangedSuccessTest extends BaseComponentTest
{
	private final String url = "/api/v1/notifyservice/notification/order_changed_success";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for sending an email works correctly, for a valid notification information.
	 * It ensures that the endpoint returns true for a valid notification information.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		NotifyInfo info = createSampleNotifyInfo();

		String jsonRequest = objectMapper.writeValueAsString(info);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Assertions.assertTrue(Boolean.parseBoolean(result));
	}

	/*
	 * The test is designed to verify that the endpoint for sending an email works correctly, for an empty notification information.
	 * It ensures that the endpoint returns true for an invalid notification information.
	 */
	@Test
	void invalidTestEmptyObject() throws Exception {
		NotifyInfo info = new NotifyInfo();

		String jsonRequest = objectMapper.writeValueAsString(info);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Assertions.assertFalse(Boolean.parseBoolean(result));
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple notification information objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		NotifyInfo info1 = createSampleNotifyInfo();
		NotifyInfo info2 = createSampleNotifyInfo();
		NotifyInfo[] infos = {info1, info2};
		String jsonRequest = objectMapper.writeValueAsString(infos);

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