package consignprice.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import consignprice.entity.ConsignPrice;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve the calculated consign price based on the given weight and region via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetConsignPriceWeightIsWithinRegionTest extends BaseComponentTest {

	private final String url = "/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving the consign price works correctly with a consign price that exists in the database and valid region and weight values.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the price.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();
		consignPriceConfigRepository.save(consignPrice);

		double weight = 10.0;
		String isWithinRegion = "true";

		String result = mockMvc.perform(get(url, weight, isWithinRegion)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Double> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Double.class));
		double expectedPrice = 10.0 + 5.0 * 2.0;
		Assertions.assertEquals(new Response<>(1, "Success", expectedPrice), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving the consign price correctly handles the case
	 * when there is no consign price in the database. It returns a NestedServletException because the consign price value is not tested for null in the function.
	 */
	@Test
	void validTestGetZeroObjects() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url, 1.0, "true"));});
	}

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no weight parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingVariable()  {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, true));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid variable format is provided in the request.
	 * It ensures that the application throws a NestedServletException, because the provided string cannot be converted to a double.
	 */
	@Test
	void invalidTestNonCorrectFormatVariable() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url, "weight",true));});
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