package inside_payment.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import inside_payment.entity.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all money via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetInsidePayServiceInsidePaymentMoneyTest extends BaseComponentTest
{

	private final String url = "/api/v1/inside_pay_service/inside_payment/money";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all money, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 * Test does not return List<Money> but null
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Money money = new Money();
		addMoneyRepository.save(money);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(1, "Query Money Success", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all money correctly handles the case
	 * when there are no money in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(0, "", null), response);
	}
}