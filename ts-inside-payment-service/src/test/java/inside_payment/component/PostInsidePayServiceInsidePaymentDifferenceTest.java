package inside_payment.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import inside_payment.entity.PaymentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to create and process a payment, checking if the user has enough balance, and if not, handles the payment through the payment service.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostInsidePayServiceInsidePaymentDifferenceTest extends BaseComponentTest
{
	@Autowired
	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	@BeforeEach
	public void setUp() {
		paymentRepository.deleteAll();
		addMoneyRepository.deleteAll();
		mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	private final String url = "/api/v1/inside_pay_service/inside_payment/difference";

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has enough balance.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectObject() throws Exception{
		PaymentInfo paymentInfo = new PaymentInfo();
		paymentInfo.setUserId("123");
		paymentInfo.setOrderId("1");
		paymentInfo.setPrice("50");
		addMoneyRepository.save(createSampleMoney());
		paymentRepository.save(createSamplePayment());
		String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(1, "Pay Difference Success", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint for creating a new payment works correctly, for the case that the user has not enough balance.
	 * It uses the payment service to create a new payment.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectObjectNotEnoughBalance() throws Exception{
		PaymentInfo paymentInfo = new PaymentInfo();
		paymentInfo.setUserId("123");
		paymentInfo.setOrderId("1");
		paymentInfo.setPrice("300.0");
		addMoneyRepository.save(createSampleMoney());
		paymentRepository.save(createSamplePayment());

		String jsonRequest = objectMapper.writeValueAsString(paymentInfo);
		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "Pay Difference Success", null), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple payment information objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		PaymentInfo paymentInfo = new PaymentInfo();
		PaymentInfo[] infos = {paymentInfo, paymentInfo};
		String jsonRequest = objectMapper.writeValueAsString(infos);

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