package travel2.controller;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import edu.fudan.common.util.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import travel2.entity.*;
import travel2.service.Travel2Service;

import java.util.ArrayList;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/travel2service")
@DefaultProperties(defaultFallback = "fallback", commandProperties = {
    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "300000")
})
public class Travel2Controller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Travel2Controller.class);

    @Autowired
    private Travel2Service service;

    @GetMapping(path = "/welcome")
    public String home(@RequestHeader HttpHeaders headers)
    {
        return "Welcome to [ Travle2 Service ] !";
    }

    @GetMapping(value = "/train_types/{tripId}")
    @HystrixCommand
    public HttpEntity getTrainTypeByTripId(@PathVariable String tripId,
        @RequestHeader HttpHeaders headers)
    {
        // TrainType
        return ok(service.getTrainTypeByTripId(tripId, headers));
    }

    @GetMapping(value = "/routes/{tripId}")
    @HystrixCommand
    public HttpEntity getRouteByTripId(@PathVariable String tripId,
        @RequestHeader HttpHeaders headers)
    {
        Travel2Controller.LOGGER.info("[Get Route By Trip ID] TripId: {}", tripId);
        //Route
        return ok(service.getRouteByTripId(tripId, headers));
    }

    @PostMapping(value = "/trips/routes")
    public HttpEntity getTripsByRouteId(@RequestBody ArrayList<String> routeIds,
        @RequestHeader HttpHeaders headers)
    {
        // ArrayList<ArrayList<Trip>>
        return ok(service.getTripByRoute(routeIds, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trips")
    public HttpEntity<?> createTrip(@RequestBody TravelInfo routeIds, @RequestHeader HttpHeaders headers)
    {
        // null
        return new ResponseEntity<>(service.create(routeIds, headers), HttpStatus.CREATED);
    }

    /**
     * Return Trip only, no left ticket information
     *
     * @param tripId trip id
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/trips/{tripId}")
    public HttpEntity retrieve(@PathVariable String tripId, @RequestHeader HttpHeaders headers)
    {
        // Trip
        return ok(service.retrieve(tripId, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(value = "/trips")
    public HttpEntity updateTrip(@RequestBody TravelInfo info, @RequestHeader HttpHeaders headers)
    {
        // Trip
        return ok(service.update(info, headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(value = "/trips/{tripId}")
    public HttpEntity deleteTrip(@PathVariable String tripId, @RequestHeader HttpHeaders headers)
    {
        // string
        return ok(service.delete(tripId, headers));
    }

    /**
     * Return Trip and the remaining tickets
     *
     * @param info trip info
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trips/left")
    @HystrixCommand
    public HttpEntity queryInfo(@RequestBody TripInfo info, @RequestHeader HttpHeaders headers)
    {
        if (info.getStartingPlace() == null || info.getStartingPlace().length() == 0 ||
            info.getEndPlace() == null || info.getEndPlace().length() == 0 ||
            info.getDepartureTime() == null)
        {
            Travel2Controller.LOGGER.info("[Travel Service][Travel Query] Fail.Something null.");
            ArrayList<TripResponse> errorList = new ArrayList<>();
            return ok(errorList);
        }
        Travel2Controller.LOGGER.info("[Travel Service] Query TripResponse");
        return ok(service.query(info, headers));
    }

    /**
     * Return a Trip and the remaining tickets
     *
     * @param gtdi trip all datail info
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trip_detail")
    @HystrixCommand
    public HttpEntity getTripAllDetailInfo(@RequestBody TripAllDetailInfo gtdi, @RequestHeader HttpHeaders headers)
    {
        return ok(service.getTripAllDetailInfo(gtdi, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/trips")
    public HttpEntity queryAll(@RequestHeader HttpHeaders headers)
    {
        // List<Trip>
        return ok(service.queryAll(headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/admin_trip")
    @HystrixCommand
    public HttpEntity adminQueryAll(@RequestHeader HttpHeaders headers)
    {
        // ArrayList<AdminTrip>
        return ok(service.adminQueryAll(headers));
    }

    private HttpEntity fallback()
    {
        return ok(new Response<>());
    }
}
