package other.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import other.entity.Order;
import other.entity.SoldTicket;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all sold tickets for a train that leaves at a certain date.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetOrderOtherServiceOrderOtherTravelDateTrainNumberTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderOtherService/orderOther/{travelDate}/{trainNumber}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving the sold tickets for a train works correctly, for valid path variables with orders that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the sold tickets.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Order order = createSampleOrder();
		orderOtherRepository.save(order);
		SoldTicket ticket = new SoldTicket();
		ticket.setTravelDate(order.getTravelDate());
		ticket.setTrainNumber(order.getTrainNumber());
		ticket.setFirstClassSeat(1);
		String result = mockMvc.perform(get(url, order.getTravelDate().toString(), order.getTrainNumber())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<SoldTicket> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, SoldTicket.class));
		Assertions.assertEquals(new Response<>(1, "Success", ticket), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving the sold tickets for a train correctly handles the case
	 * when there is no order associated with the given travel data and train number. It ensures that the endpoint returns a response with the appropriate message and the sold tickets.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		Order order = createSampleOrder();
		SoldTicket ticket = new SoldTicket();
		ticket.setTravelDate(order.getTravelDate());
		ticket.setTrainNumber(order.getTrainNumber());

		String result = mockMvc.perform(get(url, order.getTravelDate().toString(), order.getTrainNumber())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<SoldTicket> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, SoldTicket.class));
		Assertions.assertEquals(new Response<>(1, "Success", ticket), response);
	}
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no train number parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingTrainNumber() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, new Date().toString()));});
	}
}