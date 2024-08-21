package foodsearch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import edu.fudan.common.util.Response;
import foodsearch.entity.FoodOrder;
import foodsearch.service.FoodService;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/foodservice")
public class FoodController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FoodController.class);

    @Autowired
    FoodService foodService;

    @GetMapping(path = "/welcome")
    public String home()
    {
        return "Welcome to [ Food Service ] !";
    }

    @GetMapping(path = "/orders")
    public HttpEntity findAllFoodOrder(@RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Try to Find all FoodOrder!");
        return ok(foodService.findAllFoodOrder(headers));
    }

    @PostMapping(path = "/orders")
    public HttpEntity createFoodOrder(@RequestBody FoodOrder addFoodOrder, @RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Try to Create a FoodOrder!");
        return ok(foodService.createFoodOrder(addFoodOrder, headers));
    }

    @PutMapping(path = "/orders")
    public HttpEntity updateFoodOrder(@RequestBody FoodOrder updateFoodOrder, @RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Try to Update a FoodOrder!");
        return ok(foodService.updateFoodOrder(updateFoodOrder, headers));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(path = "/orders/{orderId}")
    public HttpEntity deleteFoodOrder(@PathVariable String orderId, @RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Try to Cancel a FoodOrder!");
        return ok(foodService.deleteFoodOrder(orderId, headers));
    }

    @GetMapping(path = "/orders/{orderId}")
    public HttpEntity findFoodOrderByOrderId(@PathVariable String orderId, @RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Try to Find all FoodOrder!");
        return ok(foodService.findByOrderId(orderId, headers));
    }

    // This relies on a lot of other services, not completely modified
    @GetMapping(path = "/foods/{date}/{startStation}/{endStation}/{tripId}")
    @HystrixCommand(fallbackMethod = "getAllFoodFallback", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "50000")
    })
    public HttpEntity getAllFood(@PathVariable String date, @PathVariable String startStation,
        @PathVariable String endStation, @PathVariable String tripId,
        @RequestHeader HttpHeaders headers)
    {
        FoodController.LOGGER.info("[Food Service]Get the Get Food Request!");
        return ok(foodService.getAllFood(date, startStation, endStation, tripId, headers));
    }

    private HttpEntity getAllFoodFallback(@PathVariable String date, @PathVariable String startStation,
        @PathVariable String endStation, @PathVariable String tripId,
        @RequestHeader HttpHeaders headers)
    {
        return ok(new Response<>());
    }
}
