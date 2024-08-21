package code.component;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



/*
 * This endpoint is used to verify the CAPTCHA code.
 * By sending a GET request to /api/v1/verifycode/verify/{verifyCode}, the service will validate the CAPTCHA code.
 *
 * The service will return a boolean value indicating if the CAPTCHA code is valid or not.
 * Remark: The Code needs to be cached in the service. So /generate needs to be called before /verify.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
public class getVerifiyCodeTest
{

    @Autowired
    private MockMvc mockMvc;

    private String invalidCode;
    private String invalidFormattedCode;
    private String invalidCharacterCode;
    private String validCookieId;

    @BeforeEach
    public void setUp() throws Exception {
        invalidCode = "INVALID123";
        invalidFormattedCode = "VALID 123";
        invalidCharacterCode = "VALID@123";

        // Retrieve a valid cookie
        MvcResult cookieResult = mockMvc.perform(get("/some/endpoint/to/get/cookie")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse cookieResponse = cookieResult.getResponse();
        Cookie validCookie = cookieResponse.getCookie("ysbCaptcha");
        assert validCookie != null;
        validCookieId = validCookie.getValue();
    }

	/*
	#####################################
	# URL parameter specific test cases #
	#####################################
	*/

    /*
     * Sending a valid Code.
     */
    @Test
    void validTestExistingId() throws Exception {
        Cookie validCookie = new Cookie("ysbCaptcha", validCookieId);

        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/verify/" + validCookieId)
                        .cookie(validCookie)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentAsString()).isEqualTo("true");
    }

    /*
     * Sending an invalid Code.
     */
    @Test
    void invalidTestNonExistingId() throws Exception {
        Cookie validCookie = new Cookie("ysbCaptcha", validCookieId);

        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/verify/" + invalidCode)
                        .cookie(validCookie)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentAsString()).isEqualTo("false");
    }

    /*
     * Sending an invalid formatted Code.
     */
    @Test
    void invalidTestNonCorrectFormatId() throws Exception {
        Cookie validCookie = new Cookie("ysbCaptcha", validCookieId);

        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/verify/" + invalidFormattedCode)
                        .cookie(validCookie)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentAsString()).isEqualTo("false");
    }

    /*
     * Sending an invalid Code with wrong characters.
     */
    @Test
    void invalidTestWrongCharacters() throws Exception {
        Cookie validCookie = new Cookie("ysbCaptcha", validCookieId);

        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/verify/" + invalidCharacterCode)
                        .cookie(validCookie)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentAsString()).isEqualTo("false");
    }
}
