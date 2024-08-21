package food.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import food.entity.FoodStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all food stores via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetFoodMapServiceFoodStoresTest extends  BaseComponentTest
{

	private final String url = "/api/v1/foodmapservice/foodstores";

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all food stores, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the food stores.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		FoodStore fs = new FoodStore();
		fs.setId(UUID.randomUUID());
		List<FoodStore> fsList = new ArrayList<>();
		fsList.add(fs);
		foodStoreRepository.save(fs);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodStore.class);
		Response<List<FoodStore>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", fsList), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all food stores correctly handles the case
	 * when there are no food stores in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodStore.class);
		Response<List<FoodStore>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "Foodstore is empty", null), response);
	}
}