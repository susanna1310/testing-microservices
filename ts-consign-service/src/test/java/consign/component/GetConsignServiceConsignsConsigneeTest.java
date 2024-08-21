package consign.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import consign.entity.ConsignRecord;
import edu.fudan.common.util.Response;
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
 * This endpoint is designed to retrieve consign records based on a given consignee via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetConsignServiceConsignsConsigneeTest extends BaseComponentTest
{
	private final String url = "/api/v1/consignservice/consigns/{consignee}";

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving consign records by consignee works correctly, for a valid id, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the consign record.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		ConsignRecord record = createSampleConsignRecord();
		consignRepository.save(record);
		List<ConsignRecord> records = new ArrayList<>();
		records.add(record);

		String result = mockMvc.perform(get(url, record.getConsignee())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, ConsignRecord.class);
		Response<List<ConsignRecord>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Find consign by consignee success", records), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving consign records by consignee correctly handles the case
	 * when there is no record associated with the given consignee. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url, UUID.randomUUID())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<List<ConsignRecord>> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, ConsignRecord.class));
		Assertions.assertEquals(new Response<>(0, "No Content according to consignee", null), response);
	}

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no consignee parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url));});
	}

	/*
	* The test is designed to verify the behavior of the endpoint when an empty string is passed as the parameter.
	* This test is crucial for ensuring that the endpoint correctly handles invalid or inappropriate requests.
	*/
	@Test
	void invalidTestNonCorrectFormatId() throws Exception {
		mockMvc.perform(get(url, "")
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isMethodNotAllowed());
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