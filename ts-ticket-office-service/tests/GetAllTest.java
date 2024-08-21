/*
 * This endpoint returns all entries in the systems database (MongoDB).
 * It just reads the data from the database and returns it.
 *
 * The databased needs to be mocked for this test.
 *
 * REMARK: Testscases are not fully implemented, because this is not a java service.
 */
public class getAllTest
{

	/*
	####################################
	# Method (GET) specific test cases #
	####################################
	*/

    /*
     * Test if the endpoint returns the correct status code and a valid list (JSON) of all objects.
     */
    @Test
    void validTestGetAllObjects() {
        // Prepare the mock database with some objects
        ...
    }

    /*
     * Test if the endpoint returns the correct status code and a valid empty list (JSON).
     */
    @Test
    void validTestGetZeroObjects() {
        // Prepare the mock database with no objects
        ...
    }
}