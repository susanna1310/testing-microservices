package consign.controller;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import consign.entity.Consign;
import consign.service.ConsignService;
import edu.fudan.common.util.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/consignservice")
@DefaultProperties(defaultFallback = "fallback", commandProperties = {
    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
})
public class ConsignController
{
    @Autowired
    ConsignService service;

    @GetMapping(path = "/welcome")
    public String home(@RequestHeader HttpHeaders headers)
    {
        return "Welcome to [ Consign Service ] !";
    }

    @PostMapping(value = "/consigns")
    @HystrixCommand
    public HttpEntity insertConsign(@RequestBody Consign request,
        @RequestHeader HttpHeaders headers)
    {
        return ok(service.insertConsignRecord(request, headers));
    }

    @PutMapping(value = "/consigns")
    @HystrixCommand
    public HttpEntity updateConsign(@RequestBody Consign request, @RequestHeader HttpHeaders headers)
    {
        return ok(service.updateConsignRecord(request, headers));
    }

    @GetMapping(value = "/consigns/account/{id}")
    public HttpEntity findByAccountId(@PathVariable String id, @RequestHeader HttpHeaders headers)
    {
        UUID newid = UUID.fromString(id);
        return ok(service.queryByAccountId(newid, headers));
    }

    @GetMapping(value = "/consigns/order/{id}")
    public HttpEntity findByOrderId(@PathVariable String id, @RequestHeader HttpHeaders headers)
    {
        UUID newid = UUID.fromString(id);
        return ok(service.queryByOrderId(newid, headers));
    }

    @GetMapping(value = "/consigns/{consignee}")
    public HttpEntity findByConsignee(@PathVariable String consignee, @RequestHeader HttpHeaders headers)
    {
        return ok(service.queryByConsignee(consignee, headers));
    }

    private HttpEntity fallback()
    {
        return ok(new Response<>());
    }
}
