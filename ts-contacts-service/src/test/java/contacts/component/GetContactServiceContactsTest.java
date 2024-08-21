package contacts.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import contacts.entity.Contacts;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve all contacts via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 */
public class GetContactServiceContactsTest extends  BaseComponentTest
{
	private final String url = "/api/v1/contactservice/contacts";
	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving all contacts, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the contacts.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();
        contactsRepository.saveAll(contacts);

		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Contacts.class);
		Response<List<Contacts>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", contacts), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving all contacts correctly handles the case
	 * when there are no contacts in the database. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		String result = mockMvc.perform(get(url)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Contacts.class);
		Response<List<Contacts>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(0, "No content", null), response);
	}
}