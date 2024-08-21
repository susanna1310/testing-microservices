package contacts.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import contacts.entity.Contacts;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint is designed to create a new contacts via POST request.
 * This endpoint is for the admin.
 *
 * The tests are divided into two main categories:
 * - Method-specific test cases for the POST request.
 */
public class PostContactServiceContactsAdminTest extends BaseComponentTest
{
	private final String url = "/api/v1/contactservice/contacts/admin";

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

	/*
	 * The test is designed to verify that the endpoint for creating a new contacts works correctly, for a valid Contacts.
	 * It ensures that the endpoint returns a successful response with the appropriate message and no content.
	 */
	@Test
	void validTestCorrectObject() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);
		String jsonRequest = objectMapper.writeValueAsString(contact);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Contacts> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Contacts.class));
		Assertions.assertEquals(new Response<>(1, "Create Success", null), response);
	}

	/*
	 * The test verifies the behavior of the endpoint when attempting to perform a POST request with multiple contacts objects provided in the request payload.
	 * It expects the endpoint to return a 400 Bad Request status code, indicating that handling multiple objects in a single request is not supported or not correctly implemented.
	 */
	@Test
	void invalidTestMultipleObjects() throws Exception {
		List<Contacts> contacts = createSampleContacts();

		String jsonRequest = objectMapper.writeValueAsString(contacts.toArray());

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isBadRequest());
	}


	/*
	 * The test is designed to verify that the endpoint for creating a new contacts correctly handles the case
	 * when there already exists a contact with the same ID. It ensures that the endpoint returns a response with the appropriate message and the contact.
	 * The test fails because the controller creates a new ID for the contact, therefore the old contact ID is not found and the contact
	 * is created with the new ID
	 */
	@Test
	void invalidTestDuplicateObject() throws Exception {
		List<Contacts> contacts = createSampleContacts();
		Contacts contact = contacts.get(0);
		contactsRepository.save(contact);
		String jsonRequest = objectMapper.writeValueAsString(contact);

		String result = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		TypeFactory typeFactory = objectMapper.getTypeFactory();
		Response<Contacts> response = objectMapper.readValue(result, typeFactory.constructParametricType(Response.class, Contacts.class));
		Assertions.assertEquals(new Response<>(0, "Already exists", contact), response);
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
}