package adminorder.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import foodsearch.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import edu.fudan.common.util.Response;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve retrieves all trip food for a given train trip, including available train food and food stores at stations along the route,
 * based on the provided date, start station, end station, and trip ID. To do that it communicates with the ts-food-map-service to get the train foods with the same trip ID,
 * the ts-travel-service to get the route of the trip id and its stations, the ts-station-service to get id form the start and end station. This is used to filter out the station that
 * matches the ids. Lastly it gets all the food stores from the ts-food-map-service and matches them with the station ids, in order to get all trip train foods and food stores.
 *
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetFoodServiceFoodsDateStartStationEndStationTripIdTest extends BaseComponentTest
{
	@Autowired
	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	private final String url = "/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}";

	@BeforeEach
	public void setUp() {
		mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	private List<TrainFood> createSampleTrainFood() {
		TrainFood tf = new TrainFood();
		tf.setId(UUID.randomUUID());
		List<TrainFood> list = new ArrayList<>();
		list.add(tf);
        return list;
	}

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all trip food correctly, for all valid path variables that have matching objects in the database
	 * It ensures that the endpoint returns a successful response with the appropriate message and all trip food.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		AllTripFood allTripFood = new AllTripFood();
		String tripId = "1234";
		String startStation = "Tokio";
		String endStation = "Osaka";

		List<TrainFood> trainFoodList = createSampleTrainFood();
		Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + tripId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		Route route = new Route();
		List<String> ids = new ArrayList<>();
		ids.add("13");
		ids.add("21");
		route.setStations(ids);
		Response<Route> responseRoute = new Response<>(1, "Success", route);
		uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + tripId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

		String startStationId = "13";
		Response<String> responseStartStation = new Response<>(1, "Success", startStationId);
		uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + startStation).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseStartStation), MediaType.APPLICATION_JSON));

		String endStationId = "21";
		Response<String> responseEndStation = new Response<>(1, "Success", endStationId);
		uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + endStation).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseEndStation), MediaType.APPLICATION_JSON));

		FoodStore fs = new FoodStore();
		fs.setId(UUID.randomUUID());
		fs.setStationId("21");
		List<FoodStore> fsList = new ArrayList<>();
		fsList.add(fs);
		Response<List<FoodStore>> responseFoodStore = new Response<>(1, "Success", fsList);
		uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/foodstores").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseFoodStore), MediaType.APPLICATION_JSON));

		Map<String, List<FoodStore>> foodStoreListMap = new HashMap<>();
		foodStoreListMap.put("13", new ArrayList<>());
		foodStoreListMap.put(fs.getStationId(), fsList);
		allTripFood.setTrainFoodList(trainFoodList);
		allTripFood.setFoodStoreListMap(foodStoreListMap);

		String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<AllTripFood> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(1, "Get All Food Success", allTripFood), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all trip food correctly handles the case
	 * when there are no train foods associated with the given trip ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroTrainFoodObjects() throws Exception {
		Response<List<TrainFood>> responseTrainFood = new Response<>(0, "No content", null);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + "tripId").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, "date", "startStation", "endStation", "tripId")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<AllTripFood> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(0, "Get the Get Food Request Failed!", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all trip food correctly handles the case
	 * when there are no food stores in the database. It ensures that the endpoint returns a  response with the appropriate message and an empty all trip food list.
	 */
	@Test
	void validTestGetZeroFoodStoreObjects() throws  Exception {
		String tripId = "1234";
		String startStation = "Tokio";
		String endStation = "Osaka";

		List<TrainFood> trainFoodList = createSampleTrainFood();
		Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + tripId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		Route route = new Route();
		List<String> ids = new ArrayList<>();
		ids.add("13");
		ids.add("21");
		route.setStations(ids);
		Response<Route> responseRoute = new Response<>(1, "Success", route);
		uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + tripId).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

		String startStationId = "13";
		Response<String> responseStartStation = new Response<>(1, "Success", startStationId);
		uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + startStation).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseStartStation), MediaType.APPLICATION_JSON));

		String endStationId = "21";
		Response<String> responseEndStation = new Response<>(1, "Success", endStationId);
		uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + endStation).build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseEndStation), MediaType.APPLICATION_JSON));

		Response<List<FoodStore>> responseFoodStore = new Response<>(0, "No content", null);
		uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/foodstores").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseFoodStore), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, "date", startStation, endStation, tripId)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<AllTripFood> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(0, "Get All Food Failed", new AllTripFood()), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all trip food correctly handles the case
	 * when there are no route associated with the given trip ID. It ensures that the endpoint returns a successful response with the appropriate message and an empty all trip food list.
	 */
	@Test
	void validTestGetNoRouteObject() throws Exception {
		List<TrainFood> trainFoodList = createSampleTrainFood();
		Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
		URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + "tripId").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

		Response<Route> responseRoute = new Response<>(0, "No content", null);
		uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + "tripId").build().toUri();

		mockServer.expect(ExpectedCount.once(), requestTo(uri))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(objectMapper.writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

		String result = mockMvc.perform(get(url, "date", "startStation", "endStation", "tripId")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<AllTripFood> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
		mockServer.verify();
		Assertions.assertEquals(new Response<>(0, "Get All Food Failed", new AllTripFood()), response);
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
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url, "date", "start", "end"));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid trip ID is provided.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void invalidTestNonCorrectFormatId() throws Exception {
		String result = mockMvc.perform(get(url,"date", "startStation", "endStation", "Id")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<AllTripFood> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
		Assertions.assertEquals(new Response<>(0, "Trip id is not suitable", null), response);
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided. With the character
	 * "/" the url changes and is therefore not found.
	 */
	@Test
	void invalidTestWrongCharacters() throws Exception {
		mockMvc.perform(get(url, "date", "startStation", "endStation","3/4/5")
				)
				.andExpect(status().isNotFound());
	}

}