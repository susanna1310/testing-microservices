package food.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import food.entity.FoodStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve food stores based on given station IDs via POST request.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 * - Body variable specific test cases
 */
public class PostFoodMapServiceFoodStoresTest extends BaseComponentTest
{
	private final String url = "/api/v1/foodmapservice/foodstores";
	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving food stores by station IDs works correctly, for a valid ids list with food stores that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the food stores.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		List<FoodStore> foodStoreList = new ArrayList<>();
		FoodStore fs1 = new FoodStore();
		fs1.setId(UUID.randomUUID());
		fs1.setStationId("1");
		foodStoreList.add(fs1);
		FoodStore fs2 = new FoodStore();
		fs2.setId(UUID.randomUUID());
		fs2.setStationId("2");
		foodStoreList.add(fs2);

		foodStoreRepository.saveAll(foodStoreList);

		List<String> stationIds = new ArrayList<>();
		stationIds.add("1");
		stationIds.add("2");
		String jsonRequest = objectMapper.writeValueAsString(stationIds);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodStore.class);
		Response<List<FoodStore>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", foodStoreList), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving food stores by station IDs correctly handles the case
	 * when there are no food stores associated with the given station IDs.
	 * Test fails because getFoodStoresByStationIds() function in FoodMapServiceImpl only checks if train foods is null, not if the list is empty.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		List<String> stationIds = new ArrayList<>();
		stationIds.add("1");
		String jsonRequest = objectMapper.writeValueAsString(stationIds);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodStore.class);
		Response<List<FoodStore>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No content", null), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple food store objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		List<String>[] lists = new ArrayList[2];
		lists[0] = new ArrayList<>();
		lists[1] = new ArrayList<>();
		String jsonRequest = objectMapper.writeValueAsString(lists);

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}

	/*
	* [~doc]
	* [doc~]
	*/
	@Test
	void invalidTestDuplicateObject() {
	// not needed
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
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving food stores by station IDs works correctly, for an empty station IDs list
	 * Test fails because getFoodStoresByStationIds() function in FoodMapServiceImpl only checks if train foods is null, not if the list is empty.
	 */
	@Test
	void BodyVarStationIdsValidTestEmptyList() throws Exception {
		List<String> stationIds = new ArrayList<>();
		String jsonRequest = objectMapper.writeValueAsString(stationIds);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, FoodStore.class);
		Response<List<FoodStore>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No content", null), response);
	}
}