package food.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import food.entity.TrainFood;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve train foods based on a given trip ID via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */

public class GetFoodMapServiceTrainFoodsTripIdTest extends  BaseComponentTest
{
	private final String url = "/api/v1/foodmapservice/trainfoods/{tripId}";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving train food by trip ID works correctly, for a valid id with train foods that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the train foods.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		TrainFood tf = new TrainFood();
		tf.setId(UUID.randomUUID());
		tf.setTripId("1");
		TrainFood tf2 = new TrainFood();
		tf2.setId(UUID.randomUUID());
		tf2.setTripId("1");
		List<TrainFood> tfList = new ArrayList<>();
		tfList.add(tf);
		tfList.add(tf2);
		trainFoodRepository.saveAll(tfList);

		String result = mockMvc.perform(get(url, tf.getTripId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, TrainFood.class);
		Response<List<TrainFood>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", tfList), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving train foods by trip ID correctly handles the case
	 * when there are no train foods associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 * Test fails because listTrainFoodByTripId() function in FoodMapServiceImpl only checks if train foods is null, not if the list is empty.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url, "1")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, TrainFood.class);
		Response<List<TrainFood>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No content", null), response);
	}

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no trip ID parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url));});
	}
}