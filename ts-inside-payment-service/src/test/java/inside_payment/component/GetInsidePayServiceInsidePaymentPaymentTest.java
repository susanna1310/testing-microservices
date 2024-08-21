package inside_payment.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import inside_payment.entity.Payment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all payments via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetInsidePayServiceInsidePaymentPaymentTest extends  BaseComponentTest
{
	private final String url = "/api/v1/inside_pay_service/inside_payment/payment";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all payments, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the payments.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Payment payment = new Payment();
		paymentRepository.save(payment);
		List<Payment> paymentList = new ArrayList<>();
		paymentList.add(payment);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Payment.class);
		Response<List<Payment>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Query Payment Success", paymentList), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all payments correctly handles the case
	 * when there are no payments in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Payment.class);
		Response<List<Payment>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "Query Payment Failed", null), response);
	}
}