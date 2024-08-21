package consignprice.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import consignprice.entity.ConsignPrice;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve consign price with index 0 via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetConsignPriceConfigTest extends BaseComponentTest {

	private final String url = "/api/v1/consignpriceservice/consignprice/config";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving a consign price works correctly with a consign price that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the consign price.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();

		consignPriceConfigRepository.save(consignPrice);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignPrice> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignPrice.class));
		Assertions.assertEquals(new Response<>(1, "Success", consignPrice), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving the consign price correctly handles the case
	 * when there is no consign price in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<ConsignPrice> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignPrice.class));
		Assertions.assertEquals(new Response<>(1, "Success", null), response);
	}
}
