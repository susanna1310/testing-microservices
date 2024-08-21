package inside_payment.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import inside_payment.entity.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to drawback money with the given userId and money, if money with the userId exists in the database.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetInsidePayServiceInsidePaymentDrawbackUserIdMoneyTest extends  BaseComponentTest
{
	private final String url = "/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating drawback money works correctly, for valid path variables with money that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		Money money = createSampleMoney();
		addMoneyRepository.save(money);

		String result = mockMvc.perform(get(url, money.getId(), money.getMoney())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(1, "Draw Back Money Success", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint for creating drawback money correctly handles the case
	 * when there is no money in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 * Test fails because function logic of drawback() in InsidePaymentServiceImpl is incorrect. Return value for addMoneyRepository.findByUserId(userId)
	 * check for != null but not if list is empty
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url, "123", "1")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(0, "Draw Back Money Failed", null), response);
	}

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no money parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, "123"));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid user ID is provided.
	 * It ensures that the endpoint returns a response with the appropriate message and no content.
	 * Test does not check if userId is null.
	 * Test fails because function logic of drawback() in InsidePaymentServiceImpl is incorrect. Return value for addMoneyRepository.findByUserId(userId)
	 * check for != null but not if list is empty
	 */
	@Test
	void invalidTestNonCorrectFormatId() throws Exception {
		String result = mockMvc.perform(get(url, null, "1")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<String> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(0, "Draw Back Money Failed", null), response);
	}
}