package plan.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import plan.entity.RoutePlanInfo;
import plan.entity.RoutePlanResultUnit;

import java.util.ArrayList;
import java.util.Date;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * This endpoint POSTS a RoutePlanInfo object to retrieve the trips with the fewest stops on that route. To do that, it communicates
 * with the ts-station-service, ts-route-service, ts-travel-service and ts-travel2-service to get all train trips from the given start station to end station on the date
 * and searches the (max) 5 quickest ones.
 * Services in the chain: ts-travel-service, ts-travel2-service, ts-ticketinfo-service, ts-basic-service, ts-station-service, ts-route-service, ts-trains-service,
 *                        ts-price-service, ts-order-service, ts-order-other-service, ts-seats-service, ts-config-service
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostMinStopStationsTest extends BaseIntegrationTest{

    private final String url = "/api/v1/routeplanservice/routePlan/minStopStations";

    /*
     * The equivalence based test is designed to verify that the endpoint for retrieving the trips with the fewest stops on that route works correctly, with existing station names.
     * It ensures that the endpoint returns a successful response with the appropriate message and correct data.
     */
    @Test
    @Order(1)
    void validTestCorrectObject() throws Exception {
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<ArrayList<RoutePlanResultUnit>> response = JSONObject.parseObject(result, new TypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {});
        assertEquals(1, response.getStatus());
        assertEquals("Success.", response.getMsg());
        assertEquals(4, response.getData().size());
    }

    /*
     * The equivalence based test is designed to verify that the endpoint for retrieving the trips with the fewest stops works correctly, when no trips are found with
     * the given station names. It ensures that the endpoint returns a response with the appropriate message and an empty list.
     * The test fails because the ts-route-service returns null as data when no route is found and the function is attempting to call data.size() which leads to a NullPointerException.
     */
    @Test
    @Order(2)
    void validTestNoTrips() throws Exception {
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Station A", "Station B", new Date("Mon May 04 09:00:00 GMT+0800 2025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArrayList<RoutePlanResultUnit> units = new ArrayList<>();
        Response<ArrayList<RoutePlanResultUnit>> response = JSONObject.parseObject(result, new TypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {});
        assertEquals(1, response.getStatus());
        assertEquals("Success", response.getMsg());
        assertEquals(units, response.getData());
    }


    /*
     * This  defect-based test ensures that the application handles scenarios where the
     * ts-station-service is unavailable. If a dependent service is unavailable, the application should
     * handle this gracefully without crashing or providing misleading information.
     * The test fails because the implementation returns a 200 status with null values when the service is unavailable.
     */
    @Test
    @Order(3)
    void testServiceUnavailable() throws Exception {
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Stop the station service container to simulate service unavailability
        stationContainer.stop();

        String result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();
        Response<ArrayList<RoutePlanResultUnit>> response = JSONObject.parseObject(result, new TypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {});

        //Just example response, how case could be handled in the implementation.
        assertEquals(0, response.getStatus());
        assertEquals("Station service unavailable. Please try again later.", response.getMsg());
    }
}
