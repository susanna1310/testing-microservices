package code.component;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/*
 * This endpoint creates a CAPTCHA image and text, manages cookies to associate the CAPTCHA with the session,
 * and returns the image. The CAPTCHA is beeing used to prevent automated systems from accessing
 * certain functionalities by requiring human verification.
 * By sending a GET request to /api/v1/verifycode/generate, the server will return a verification image.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
public class getGenerateCodeTest
{

    @Autowired
    private MockMvc mockMvc;

    /*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * This test case sends a GET request to /api/v1/verifycode/generate and
     * expects the verification image to be returned.
     *
     * The test will check if the response status is 200 (OK) and if the content type is image/jpeg.
     * It will also check if the byte array returned is not empty and if the first two bytes are the JPEG magic numbers.
     * Finally, it will check if the Set-Cookie header is present and if the cookie is replaced.
     */
    @Test
    void validTestGetAllObjects() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        Cookie cookie = new Cookie("ysbCaptcha", "needsToBeReplaced");

        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/generate")
                        .headers(headers)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        // Check if the byte array starts with JPEG magic numbers
        assertThat(content[0]).isEqualTo((byte) 0xFF);
        assertThat(content[1]).isEqualTo((byte) 0xD8);

        // Check if the Set-Cookie header is present and if the cookie is replaced
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("ysbCaptcha=");
        assertThat(setCookieHeader).doesNotContain("needsToBeReplaced");
    }
}
