package contacts.component;

import com.fasterxml.jackson.databind.type.CollectionType;
import contacts.entity.Contacts;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve contacts based on a given account ID via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetContactServiceContactsAccountIdTest extends BaseComponentTest
{
	private final String url = "/api/v1/contactservice/contacts/account/{accountId}";

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving contacts by account ID works correctly, for a valid ID with contacts that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the contact.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);
		contactsRepository.save(contact);
		contacts.remove(1);

		String result = mockMvc.perform(get(url, contact.getAccountId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Contacts.class);
		Response<List<Contacts>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		Assertions.assertEquals(new Response<>(1, "Success", contacts), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving contacts by account ID correctly handles the case
	 * when there is no contact associated with the given account ID. It ensures that the endpoint returns a response with the appropriate message and the contact.
	 * Implementation does not have a response with status 0.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);

		String result = mockMvc.perform(get(url, contact.getAccountId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Contacts.class);
		Response<List<Contacts>> response = objectMapper.readValue(result, objectMapper.getTypeFactory().constructParametricType(Response.class, collectionType));
		List<Contacts> expected = new ArrayList<>();
		Assertions.assertEquals(new Response<>(1, "Success", expected), response);
	}
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no account ID parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId() {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(get(url));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided.
	 * It ensures that the application throws a NestedServletException due to the incorrect format of the UUID ID.
	 */
	@Test
	void invalidTestNonCorrectFormatId() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(get(url, "1234"));});
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