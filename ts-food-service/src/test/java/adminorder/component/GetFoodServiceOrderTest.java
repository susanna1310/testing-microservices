package adminorder.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import foodsearch.entity.FoodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
 * This endpoint is designed to retrieve all food orders via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetFoodServiceOrderTest extends BaseComponentTest
{

	private final String url = "/api/v1/foodservice/orders";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all food orders works correctly,for food orders that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the food orders.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		FoodOrder fo = createSampleFoodOder();
		List<FoodOrder> foList = new ArrayList<>();
		foList.add(fo);
		foodOrderRepository.save(fo);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodOrder.class);
		Response<List<FoodOrder>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success.", foList), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all food orders correctly handles the case
	 * when there are no orders in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodOrder.class);
		Response<List<FoodOrder>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No Content", null), response);
	}
}