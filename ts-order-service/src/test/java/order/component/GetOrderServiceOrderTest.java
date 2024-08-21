package order.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import order.entity.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all orders via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */

public class GetOrderServiceOrderTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderservice/order";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all orders, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the orders.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Order order = createSampleOrder();
		orderRepository.save(order);
		ArrayList<Order> orders = new ArrayList<>();
		orders.add(order);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Order.class);
		Response<ArrayList<Order>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success.", orders), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all orders correctly handles the case
	 * when there are no contacts in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Order.class);
		Response<ArrayList<Order>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No Content.", null), response);
	}
}