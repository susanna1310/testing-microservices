package inside_payment.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import inside_payment.entity.Balance;
import inside_payment.entity.Money;
import inside_payment.entity.Payment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to processes and retrieves account balances for users based on their money transactions and payments via GET request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetInsidePayServiceInsidePaymentAccountTest extends BaseComponentTest
{

	private final String url = "/api/v1/inside_pay_service/inside_payment/account";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving account balances, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the balances of each account.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		List<Balance> balances = new ArrayList<>();
		Money money = createSampleMoney();
		addMoneyRepository.save(money);
		Payment payment = createSamplePayment();
		Balance balance = new Balance();
		balance.setUserId("123");
		balance.setBalance("200.0");
		balances.add(balance);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Balance.class);
		Response<List<Balance>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", balances), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving account balances correctly handles the case
	 * when there is no money in the database. It ensures that the endpoint returns a successful response with the appropriate message and an empty balances list.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Balance.class);
		Response<List<Balance>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		List<Balance> balances = new ArrayList<>();
		Assertions.assertEquals(new Response<>(1, "Success",balances), response);
	}
}