package order.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import order.entity.LeftTicketInfo;
import order.entity.Order;
import order.entity.Seat;
import order.entity.Ticket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed retrieves the tickets information for the train with matching train number and travel date that is given in the body as seat object.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostOrderServiceOrderTicketsTest extends BaseComponentTest
{

	private final String url = "/api/v1/orderservice/order/tickets";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating a new order works correctly, for a valid order.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the order.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		Order order = createSampleOrder();
		orderRepository.save(order);
		Seat seat = new Seat();
		seat.setTravelDate(order.getTravelDate());
		seat.setTrainNumber(order.getTrainNumber());
		String jsonRequest = objectMapper.writeValueAsString(seat);
		Set ticketSet = new HashSet();
		ticketSet.add(new Ticket(Integer.parseInt(order.getSeatNumber()),
				order.getFrom(), order.getTo()));
		LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
		leftTicketInfo.setSoldTickets(ticketSet);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<LeftTicketInfo> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, LeftTicketInfo.class));
		Assertions.assertEquals(new Response<>(1, "Success", leftTicketInfo), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple seat objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		Seat[] seats = {new Seat(), new Seat()};
		String jsonRequest = objectMapper.writeValueAsString(seats);

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all sold tickets correctly handles the case
	 * when there are no orders in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void invalidMissingObject() throws Exception {
		Order order = createSampleOrder();

		String jsonRequest = objectMapper.writeValueAsString(order);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<LeftTicketInfo> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, LeftTicketInfo.class));
		Assertions.assertEquals(new Response<>(0, "Order is Null.", null), response);
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