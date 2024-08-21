/*
 * This endpoint deletes an office in the database.
 * The offfice is provided in the body of the request (province, city, region, office).
 *
 * The databased needs to be mocked for this test.
 *
 * REMARK: Testscases are not fully implemented, because this is not a java service.
 */
public class deleteOfficeTest
{

	/*
	#####################################
	# Method (POST) specific test cases #
	#####################################
	*/

    /*
     * This test checks if the endpoint returns the correct status code and removes the office.
     */
    @Test
    void validTestCorrectObject() {
		...
    }

    /*
     * This test checks if the endpoint handles the request with multiple offices.
     */
    @Test
    void invalidTestMultipleObjects() {
		...
    }

    /*
     * This test checks if the endpoint handels the request is malformed.
     */
    @Test
    void invalidTestMalformedObject() {
		...
    }

    /*
     * This test checks if the endpoint handels the request is missing the body.
     */
    @Test
    void invalidTestMissingBody() {
		...
    }
	/*
	#####################################
	# Body variable specific test cases #
	#####################################
	*/

    /*
     * Check if the body variable province is valid (length, characters).
     */
    @Test
    void bodyVarProvinceValidTestCorrectLengthAndCharacters() {
		...
    }

    /*
     * Check handling if the body variable province is too long.
     */
    @Test
    void bodyVarProvinceInvalidTestStringTooLong() {
		...
    }

    /*
     * Check handling if the body variable province is too short.
     */
    @Test
    void bodyVarProvinceInvalidTestStringTooShort() {
		...
    }

    /*
     * Check handling if the body variable province contains wrong characters.
     */
    @Test
    void bodyVarProvinceInvalidTestStringContainsWrongCharacters() {
		...
    }

    /*
     * Check handling if the body variable province is null.
     */
    @Test
    void bodyVarProvinceInvalidTestStringIsNull() {
		...
    }

    /*
     * Check if the body variable city is valid (length, characters).
     */
    @Test
    void bodyVarCityValidTestCorrectLengthAndCharacters() {
		...
    }

    /*
     * Check handling if the body variable city is too long.
     */
    @Test
    void bodyVarCityInvalidTestStringTooLong() {
		...
    }

    /*
     * Check handling if the body variable city is too short.
     */
    @Test
    void bodyVarCityInvalidTestStringTooShort() {
		...
    }

    /*
     * Check handling if the body variable city contains wrong characters.
     */
    @Test
    void bodyVarCityInvalidTestStringContainsWrongCharacters() {
		...
    }

    /*
     * Check handling if the body variable city is null.
     */
    @Test
    void bodyVarCityInvalidTestStringIsNull() {
		...
    }

    /*
     * Check if the body variable region is valid (length, characters).
     */
    @Test
    void bodyVarRegionValidTestCorrectLengthAndCharacters() {
		...
    }

    /*
     * Check handling if the body variable region is too long.
     */
    @Test
    void bodyVarRegionInvalidTestStringTooLong() {
		...
    }

    /*
     * Check handling if the body variable region is too short.
     */
    @Test
    void bodyVarRegionInvalidTestStringTooShort() {
		...
    }

    /*
     * Check handling if the body variable region contains wrong characters.
     */
    @Test
    void bodyVarRegionInvalidTestStringContainsWrongCharacters() {
		...
    }

    /*
     * Check handling if the body variable region is null.
     */
    @Test
    void bodyVarRegionInvalidTestStringIsNull() {
		...
    }

    /*
     * Check if the body variable office is valid (length, characters).
     */
    @Test
    void bodyVarOfficeValidTestCorrectLengthAndCharacters() {
		...
    }

    /*
     * Check handling if the body variable office is too long.
     */
    @Test
    void bodyVarOfficeInvalidTestStringTooLong() {
		...
    }

    /*
     * Check handling if the body variable office is too short.
     */
    @Test
    void bodyVarOfficeInvalidTestStringTooShort() {
		...
    }

    /*
     * Check handling if the body variable office contains wrong characters.
     */
    @Test
    void bodyVarOfficeInvalidTestStringContainsWrongCharacters() {
		...
    }

    /*
     * Check handling if the body variable office is null.
     */
    @Test
    void bodyVarOfficeInvalidTestStringIsNull() {
		...
    }

}