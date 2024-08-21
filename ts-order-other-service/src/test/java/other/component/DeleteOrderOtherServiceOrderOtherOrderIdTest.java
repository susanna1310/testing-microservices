package other.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;
import other.entity.Order;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to delete an order based on a given order ID via DELETE request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the DELETE request.
 * - URL parameter-specific test cases.
 */
public class DeleteOrderOtherServiceOrderOtherOrderIdTest extends BaseComponentTest
{
	private final String url = "/api/v1/orderOtherService/orderOther/{orderId}";
	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

	/*
	 * The test is designed to verify that the endpoint for deleting the order by order ID works correctly, for a valid id with an order that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the order.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		Order order = createSampleOrder();
		orderOtherRepository.save(order);

		String result = mockMvc.perform(delete(url, order.getId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<UUID> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, UUID.class));
		Assertions.assertEquals(new Response<>(1, "Success", order.getId()), response);
	}


	/*
	 * The test is designed to verify that the endpoint for deleting an order by order ID correctly handles the case
	 * when there is no order associated with the given order ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void invalidTestMissingObject() throws Exception {
		String result = mockMvc.perform(delete(url, "5ad7750b-a68b-49c0-a8c0-32776b067703")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<UUID> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, UUID.class));
		Assertions.assertEquals(new Response<>(0, "Order Not Exist.", null), response);
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
}