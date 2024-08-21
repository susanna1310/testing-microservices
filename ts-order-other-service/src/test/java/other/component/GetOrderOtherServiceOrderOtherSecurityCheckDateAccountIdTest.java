package other.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;
import other.entity.Order;
import other.entity.OrderSecurity;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to check the security of orders by counting valid orders and those placed in the last hour for a given account,
 * retrieving the counts in an order security object.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetOrderOtherServiceOrderOtherSecurityCheckDateAccountIdTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderOtherService/orderOther/security/{checkDate}/{accountId}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving the order security by account ID works correctly, for a valid id with orders that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the order security.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Order order = createSampleOrder();
		orderOtherRepository.save(order);
		OrderSecurity security = new OrderSecurity();
		security.setOrderNumOfValidOrder(1);
		security.setOrderNumInLastOneHour(1);

		String result = mockMvc.perform(get(url, order.getTravelDate().toString() ,order.getAccountId().toString())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<OrderSecurity> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, OrderSecurity.class));
		Assertions.assertEquals(new Response<>(1, "Success", security), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving the order security by account ID correctly handles the case
	 * when there are no orders associated with the given account ID. It ensures that the endpoint returns a successful response with the appropriate message and the order security.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		Order order = createSampleOrder();
		OrderSecurity security = new OrderSecurity();

		String result = mockMvc.perform(get(url, order.getTravelDate().toString() ,order.getAccountId().toString())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<OrderSecurity> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, OrderSecurity.class));
		Assertions.assertEquals(new Response<>(1, "Success", security), response);
	}
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no account ID parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, new Date().toString()));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided.
	 * It ensures that the application throws a NestedServletException due to the incorrect format of the UUID ID.
	 */
	@Test
	void invalidTestNonCorrectFormatId() throws Exception {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url, new Date().toString(), "§$!"));});
	}
}