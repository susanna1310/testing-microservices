package other.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
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
import other.entity.Order;
import other.entity.QueryInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to check if the orders, where the account ID matches the login ID from the order information, fit the requirements (same as PostOrderServiceOrderQueryTest)
 * and update the "from" and "to" station name of each order to the station id.
 * To update get the station id, it communicates with the station service.
 *
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostOrderOtherServiceOrderOtherRefreshTest extends BaseComponentTest
{

	@Autowired
	private RestTemplate restTemplate;
	private MockRestServiceServer mockServer;


	private final String url = "/api/v1/orderOtherService/orderOther/refresh";

	@BeforeEach
	public void setUp() {
		mockServer = MockRestServiceServer.createServer(restTemplate);
		orderOtherRepository.deleteAll();
	}
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for checking if the orders fit the requirements works correctly and updates the station IDs, for a valid order information
	 * where the login ID matches an account ID of orders in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and correct orders.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		QueryInfo info = new QueryInfo();
		info.setLoginId(UUID.randomUUID().toString());
		info.setEnableStateQuery(true);
		info.setEnableTravelDateQuery(false);
		info.setEnableBoughtDateQuery(false);
		info.setState(0);
		Order order = createSampleOrder();
		order.setAccountId(UUID.fromString(info.getLoginId()));
		orderOtherRepository.save(order);
		ArrayList<Order> orderList = new ArrayList<>();
		orderList.add(order);
		order.setFrom("123");
		order.setTo("321");

		List<String> stationList = new ArrayList<>();
		stationList.add("123");
		stationList.add("321");
		Response<List<String>> responseTrainFood = new Response<>(1, "Success", stationList);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/namelist").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		String jsonRequest = objectMapper.writeValueAsString(info);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Order.class);
		Response<ArrayList<Order>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", orderList), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple order information objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		QueryInfo[] orders = {new QueryInfo(), new QueryInfo()};
		String jsonRequest = objectMapper.writeValueAsString(orders);

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
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