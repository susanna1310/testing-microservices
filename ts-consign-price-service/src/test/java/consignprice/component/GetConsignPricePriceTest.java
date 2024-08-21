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
 * This endpoint is designed to retrieve a descriptive string about the pricing details of the consign price with index 0 via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetConsignPricePriceTest extends BaseComponentTest{

	private final String url = "/api/v1/consignpriceservice/consignprice/price";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving the pricing details of the consign price works correctly with a consign price that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the pricing details.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		ConsignPrice consignPrice = createSampleConsignPrice();
		consignPriceConfigRepository.save(consignPrice);

        String sb = "The price of weight within " +
                consignPrice.getInitialWeight() +
                " is " +
                consignPrice.getInitialPrice() +
                ". The price of extra weight within the region is " +
                consignPrice.getWithinPrice() +
                " and beyond the region is " +
                consignPrice.getBeyondPrice() +
                "\n";


		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Double> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, String.class));
		Assertions.assertEquals(new Response<>(1, "Success", sb), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving the pricing details of the consign price correctly handles the case
	 * when there is no consign price in the database. It returns a NestedServletException because the consign price value is not tested for null in the function.
	 */
	@Test
	void validTestGetZeroObjects() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url));});
	}
}