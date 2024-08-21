package contacts.component;

import com.fasterxml.jackson.databind.type.TypeFactory;
import contacts.entity.Contacts;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.NestedServletException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to delete contacts based on a given contacts ID via DELETE.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the DELETE request.
 * - URL parameter-specific test cases.
 */

public class DeleteContactServiceContactsContactsIdTest extends BaseComponentTest
{
	private final String url = "/api/v1/contactservice/contacts/{contactsId}";
	/*
	#######################################
	# Method (DELETE) specific test cases #
	#######################################
	*/

	/*
	 * The test is designed to verify that the endpoint for deleting contacts by contacts ID works correctly, for a valid ID, that exists in the database.
	 * It ensures that the endpoint returns a successful response with the appropriate message and the contact ID.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);
		contactsRepository.save(contact);

		String result = mockMvc.perform(delete(url, contact.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<UUID> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, UUID.class));
		Assertions.assertEquals(new Response<>(1, "Delete success", contact.getId()), response);
	}

	/*
	 * The test is designed to verify that the endpoint for deleting contacts by contacts ID correctly handles the case
	 * when there is no contact associated with the given contacts ID. It ensures that the endpoint returns a response with the appropriate message and the contact ID.
	 */
	@Test
	void invalidTestMissingObject() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);

		String result = mockMvc.perform(delete(url, contact.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<UUID> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, UUID.class));
		Assertions.assertEquals(new Response<>(1, "Delete success", contact.getId()), response);
	}
	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when no contacts ID parameter is provided in the request.
	 * It ensures that the application throws an IllegalArgumentException due to the missing required parameter.
	 */
	@Test
	void invalidTestNonExistingId()  {
		assertThrows(IllegalArgumentException.class, () -> {mockMvc.perform(delete(url));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided.
	 * It ensures that the application throws a NestedServletException due to the incorrect format of the UUID ID.
	 */
	@Test
	void invalidTestNonCorrectFormatId() {
		assertThrows(NestedServletException.class, () -> {mockMvc.perform(delete(url, "1234"));});
	}

	/*
	 * The test is designed to verify that the endpoint correctly handles the case when an invalid ID format is provided. With the character
	 * "/" the url changes and is therefore not found.
	 */
	@Test
	void invalidTestWrongCharacters() throws Exception {
		mockMvc.perform(delete(url, "3/4/5")
				)
				.andExpect(status().isNotFound());
	}

}