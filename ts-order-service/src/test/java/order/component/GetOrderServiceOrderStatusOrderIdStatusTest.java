package order.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import order.entity.Order;
import order.entity.OrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to update the status of the order and return the order via GET request
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetOrderServiceOrderStatusOrderIdStatusTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderservice/order/status/{orderId}/{status}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for updating and retrieving the order by order ID works correctly, for a valid id with an order that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the order.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Order order = createSampleOrder();
		orderRepository.save(order);
		order.setStatus(OrderStatus.PAID.getCode());
		String result = mockMvc.perform(get(url, order.getId().toString(), OrderStatus.PAID.getCode())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Order> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
		Assertions.assertEquals(new Response<>(1, "Modify Order Success", order), response);
	}

	/*
	 * The test is designed to verify that the endpoint for updating and retrieving the order security by order ID correctly handles the case
	 * when there is no order associated with the given order ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		Order order = createSampleOrder();

		String result = mockMvc.perform(get(url, order.getId().toString(), OrderStatus.PAID.getCode())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Order> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
		Assertions.assertEquals(new Response<>(0, "Order Not Found",null), response);
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
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, OrderStatus.PAID.getCode()));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided.
	 * It ensures that the application throws an NestedServletException due to the null parameter.
	 */
	@Test
	void invalidTestNonCorrectFormatId() throws Exception {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url,null, OrderStatus.PAID.getCode()));});
	}
}