package contacts.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import contacts.entity.Contacts;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.NestedServletException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to retrieve contacts based on a given contact ID via GET.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the GET request.
 * - URL parameter-specific test cases.
 */
public class GetContactServiceContactsIdTest extends BaseComponentTest
{
	private final String url = "/api/v1/contactservice/contacts/{Id}";

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for retrieving a contacts by contacts ID works correctly, for a valid ID with a contacts that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the contacts.
	 */
	@Test
	void validTestGetAllObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);
		contactsRepository.save(contact);

		String result = mockMvc.perform(get(url, contact.getId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Contacts> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Contacts.class));
		Assertions.assertEquals(new Response<>(1, "Success", contact), response);
	}

	/*
	 * The test is designed to verify that the endpoint for retrieving contacts by contacts ID correctly handles the case
	 * when there is no contact associated with the given contact ID. It ensures that the endpoint returns a response with the appropriate message and no content.
	 */
	@Test
	void validTestGetZeroObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);

		String result = mockMvc.perform(get(url, contact.getId())
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Contacts> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Contacts.class));
		Assertions.assertEquals(new Response<>(0, "No contacts according to contacts id", null), response);
	}
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no contact ID parameter is provided in the request.
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