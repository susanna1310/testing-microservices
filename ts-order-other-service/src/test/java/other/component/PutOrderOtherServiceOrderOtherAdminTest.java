package other.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import other.entity.Order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to update an order via PUT request.
 * This endpoint is for the admin.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the PUT request.
 */
public class PutOrderOtherServiceOrderOtherAdminTest extends  BaseComponentTest
{

	private final String url = "/api/v1/orderOtherService/orderOther/admin";
	/*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for updating an order by order ID works correctly, for a valid id with an order that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the order.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		Order order = createSampleOrder();
		orderOtherRepository.save(order);
		order.setPrice("40.0");

		String jsonRequest = objectMapper.writeValueAsString(order);
		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Order> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
		Assertions.assertEquals(new Response<>(1, "Success", order), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a PUT request with multiple orders objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		Order[] orders = {createSampleOrder(), createSampleOrder()};
		String jsonRequest = objectMapper.writeValueAsString(orders);

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
	 * The test is designed to verify that the endpoint for updating an order by order ID works correctly, for an order that does not exist in the database.
	 * It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void invalidTestMissingObject() throws Exception {
		Order order = createSampleOrder();

		String jsonRequest = objectMapper.writeValueAsString(order);
		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Order> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
		Assertions.assertEquals(new Response<>(0, "Order Not Found", null), response);
	}
}