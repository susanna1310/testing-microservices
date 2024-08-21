package adminorder.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import foodsearch.entity.FoodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to delete the food order based on a given order ID via DELETE request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the DELETE request.
 * - URL parameter-specific test cases.
 */
public class DeleteFoodServiceOrderOrderIdTest extends BaseComponentTest
{
	private final String url = "/api/v1/foodservice/orders/{orderId}";
	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

	/*
	 * The test is designed to verify that the endpoint for deleting the food order by order ID works correctly, for a valid id with a food order that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		FoodOrder fo = createSampleFoodOder();
		foodOrderRepository.save(fo);

		String result = mockMvc.perform(delete(url, fo.getOrderId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<FoodOrder> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, FoodOrder.class));
		Assertions.assertEquals(new Response<>(1, "Success.", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint for deleting a food order by order ID correctly handles the case
	 * when there is no food order associated with the given order ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void invalidTestMissingObject() throws Exception {
		FoodOrder fo = createSampleFoodOder();

		String result = mockMvc.perform(delete(url, fo.getOrderId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<FoodOrder> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, FoodOrder.class));
		Assertions.assertEquals(new Response<>(0, "Order Id Is Non-Existent.", null), response);
	}

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no order ID parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId()  {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(delete(url));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided.
	 * It ensures that the application throws a NestedServletException due to the incorrect format of the UUID ID.
	 */
	@Test
	void invalidTestNonCorrectFormatId() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete(url, "1234"));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided. With the character
	 * "/" the url changes and is therefore not found.
	 */
	@Test
	void invalidTestWrongCharacters() throws Exception {
		mockMvc.perform(delete(url, "3/4/5"))
				.andExpect(status().isNotFound());
	}

}