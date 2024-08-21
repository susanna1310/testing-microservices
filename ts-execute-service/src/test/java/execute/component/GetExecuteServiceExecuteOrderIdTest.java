package execute.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import execute.entity.Order;
import execute.entity.OrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to get and execute the order with the status COLLECTED and changes it to USED via GET request.
 * To do that it communicates with the ts-order-service and the ts-order-other-service.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetExecuteServiceExecuteOrderIdTest extends BaseComponentTest
{

	@Autowired
	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	@BeforeEach
	public void setUp() {
		mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	private final String url = "/api/v1/executeservice/execute/execute/{orderId}";
;
;
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for executing the order correctly, for all valid path variables that have matching objects in the database for the order service.
	 * The test uses the order service. It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestGetAllObjects() throws  Exception {
		Order order = new Order();
		order.setStatus(2);
		order.setId(UUID.randomUUID());
		Response<Order> responseOrder = new Response<>(1, "Success", order);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

		Response<Order> responseExecute = new Response<>(1, "Modify Order Success", order);
		uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/status/" + order.getId() + "/" + OrderStatus.USED.getCode()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseExecute), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, order.getId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "Success.", null), response);
	}


	/*
	 * The test is designed to verify that the endpoint for retrieving and executing the order by order ID correctly handles the case
	 * when there is no order associated with the given order ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		UUID orderId = UUID.randomUUID();;
		Response<Order> responseOrder = new Response<>(0, "Order Not Found", null);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

		Response<Order> responseOrderOther = new Response<>(0, "Order Not Found", null);
		uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseOrderOther), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, orderId)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(0, "Order Not Found", null), response);
	}

		/*
	 * The test is designed to verify that the endpoint for executing the order correctly, for all valid path variables that have matching objects in the database for the order other service.
	 * The test uses the order other service. It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestGetAllObjectsOrderOther() throws Exception {
		Order order = new Order();
		order.setStatus(OrderStatus.COLLECTED.getCode());
		order.setId(UUID.randomUUID());
		Response<Order> responseOrder = new Response<>(0, "Order Not Found", null);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

		Response<Order> responseOrderOther = new Response<>(1, "Success", order);
		uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseOrderOther), MediaType.APPLICATION_JSON));

		Response<Order> responseExecute = new Response<>(1, "Success", order);
		uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/status/" + order.getId() + "/" + OrderStatus.USED.getCode()).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseExecute), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, order.getId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "Success", null), response);
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
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url));});
	}


	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided. With the character
	 * "/" the url changes and is therefore not found.
	 */
	@Test
	void invalidTestWrongCharacters() throws Exception {
		mockMvc.perform(get(url, "3/4/5")
				)
				.andExpect(status().isNotFound());
	}
}