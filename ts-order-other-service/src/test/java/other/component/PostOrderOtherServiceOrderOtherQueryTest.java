package other.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import other.entity.Order;
import other.entity.QueryInfo;

import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to check if the orders, where the account ID matches the login ID from the order information, fit the requirements.
 * It checks the state with the status, the travel date, and the bought date and returns all orders that fit.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostOrderOtherServiceOrderOtherQueryTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderOtherService/orderOther/query";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for checking if the orders fit the requirements works correctly, for a valid order information
	 * where the login ID matches an account ID of orders in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and correct orders.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		QueryInfo info = new QueryInfo();
		info.setLoginId(UUID.randomUUID().toString());
		info.setEnableStateQuery(true);
		info.setEnableTravelDateQuery(false);
		info.setEnableBoughtDateQuery(false);
		info.setState(0);
		Order order = createSampleOrder();
		order.setAccountId(UUID.fromString(info.getLoginId()));
		orderOtherRepository.save(order);
		ArrayList<Order> orderList = new ArrayList<>();
		orderList.add(order);

		String jsonRequest = objectMapper.writeValueAsString(info);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Order.class);
		Response<ArrayList<Order>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Get order num", orderList), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple order information objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		QueryInfo[] infos = {new QueryInfo(), new QueryInfo()};
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