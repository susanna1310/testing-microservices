package adminorder.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import foodsearch.entity.FoodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to update a food order via PUT request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the PUT request.
 */
public class PutFoodServiceOrderTest extends BaseComponentTest
{
	private final String url = "/api/v1/foodservice/orders";
	/*
	####################################
	# Method (PUT) specific test cases #
	####################################
	*/


	/*
	 * The test is designed to verify that the endpoint for updating a food order works correctly, for a food order that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the food order.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		FoodOrder fo = createSampleFoodOder();
		foodOrderRepository.save(fo);
		fo.setFoodName("newFoodName");
		String jsonRequest = objectMapper.writeValueAsString(fo);

		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<FoodOrder> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, FoodOrder.class));;
		fo.setId(response.getData().getId());
		Assertions.assertEquals(new Response<>(1, "Success", fo), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a PUT request with multiple food orders objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		FoodOrder fo = createSampleFoodOder();
		FoodOrder[] orders = {fo, fo};
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
	 * The test is designed to verify that the endpoint for updating a food order works correctly, for a food order that does not exist in the database.
	 * It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestMissingObject() throws Exception {
		FoodOrder fo = createSampleFoodOder();
		String jsonRequest = objectMapper.writeValueAsString(fo);

		String result = mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<FoodOrder> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, FoodOrder.class));
		Assertions.assertEquals(new Response<>(0, "Order Id Is Non-Existent.", null), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when a PUT request is made without any object in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that the request body is missing, and thus cannot be processed as expected.
	 */
	@Test
	void invalidTestMissingBody() throws Exception {
		mockMvc.perform(put(url)
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}
}