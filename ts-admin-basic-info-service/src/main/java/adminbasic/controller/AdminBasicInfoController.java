package adminbasic.controller;

import adminbasic.entity.*;
import adminbasic.service.AdminBasicInfoService;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import edu.fudan.common.util.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/adminbasicservice")
@DefaultProperties(defaultFallback = "fallback", commandProperties = {
    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
})
public class AdminBasicInfoController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBasicInfoController.class);

    @Autowired
    AdminBasicInfoService adminBasicInfoService;

    @GetMapping(path = "/welcome")
    public String home(@RequestHeader HttpHeaders headers)
    {
        return "Welcome to [ AdminBasicInfo Service ] !";
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminbasic/contacts")
    @HystrixCommand
    public HttpEntity getAllContacts(@RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Find All Contacts by admin ");
        return ok(adminBasicInfoService.getAllContacts(headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/adminbasic/contacts/{contactsId}")
    @HystrixCommand
    public HttpEntity deleteContacts(@PathVariable String contactsId, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Delete Contacts by admin ");
        return ok(adminBasicInfoService.deleteContact(contactsId, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(path = "/adminbasic/contacts")
    @HystrixCommand
    public HttpEntity modifyContacts(@RequestBody Contacts mci, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Contacts by admin: ");
        return ok(adminBasicInfoService.modifyContact(mci, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/adminbasic/contacts")
    @HystrixCommand
    public HttpEntity addContacts(@RequestBody Contacts c, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Contacts by admin  ");
        return ok(adminBasicInfoService.addContact(c, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminbasic/stations")
    @HystrixCommand
    public HttpEntity getAllStations(@RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Find All Station by admin  ");
        return ok(adminBasicInfoService.getAllStations(headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/adminbasic/stations")
    @HystrixCommand
    public HttpEntity deleteStation(@RequestBody Station s, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Delete Station by admin ");
        return ok(adminBasicInfoService.deleteStation(s, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(path = "/adminbasic/stations")
    @HystrixCommand
    public HttpEntity modifyStation(@RequestBody Station s, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Station by admin ");
        return ok(adminBasicInfoService.modifyStation(s, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/adminbasic/stations")
    @HystrixCommand
    public HttpEntity addStation(@RequestBody Station s, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Station by admin");
        return ok(adminBasicInfoService.addStation(s, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminbasic/trains")
    @HystrixCommand
    public HttpEntity getAllTrains(@RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Find All Train by admin: ");
        return ok(adminBasicInfoService.getAllTrains(headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/adminbasic/trains/{id}")
    @HystrixCommand
    public HttpEntity deleteTrain(@PathVariable String id, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Delete Train by admin");
        return ok(adminBasicInfoService.deleteTrain(id, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(path = "/adminbasic/trains")
    @HystrixCommand
    public HttpEntity modifyTrain(@RequestBody TrainType t, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Train by admin  ");
        return ok(adminBasicInfoService.modifyTrain(t, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/adminbasic/trains")
    @HystrixCommand
    public HttpEntity addTrain(@RequestBody TrainType t, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Train by admin ");
        return ok(adminBasicInfoService.addTrain(t, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminbasic/configs")
    @HystrixCommand
    public HttpEntity getAllConfigs(@RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Find All Config by admin  ");
        return ok(adminBasicInfoService.getAllConfigs(headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/adminbasic/configs/{name}")
    @HystrixCommand
    public HttpEntity deleteConfig(@PathVariable String name, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Delete Config by admin ");
        return ok(adminBasicInfoService.deleteConfig(name, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(path = "/adminbasic/configs")
    @HystrixCommand
    public HttpEntity modifyConfig(@RequestBody Config c, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Config by admin ");
        return ok(adminBasicInfoService.modifyConfig(c, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/adminbasic/configs")
    @HystrixCommand
    public HttpEntity addConfig(@RequestBody Config c, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Config by admin  ");
        return ok(adminBasicInfoService.addConfig(c, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminbasic/prices")
    @HystrixCommand
    public HttpEntity getAllPrices(@RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Find All Price by admin ");
        return ok(adminBasicInfoService.getAllPrices(headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/adminbasic/prices")
    @HystrixCommand
    public HttpEntity deletePrice(@RequestBody PriceInfo pi, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Delete Price by admin  ");
        return ok(adminBasicInfoService.deletePrice(pi, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(path = "/adminbasic/prices")
    @HystrixCommand
    public HttpEntity modifyPrice(@RequestBody PriceInfo pi, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Modify Price by admin  ");
        return ok(adminBasicInfoService.modifyPrice(pi, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/adminbasic/prices")
    @HystrixCommand
    public HttpEntity addPrice(@RequestBody PriceInfo pi, @RequestHeader HttpHeaders headers)
    {
        AdminBasicInfoController.LOGGER.info("[Admin Basic Info Service][Add Price by admin");
        return ok(adminBasicInfoService.addPrice(pi, headers));
    }

    private HttpEntity fallback()
    {
        return ok(new Response<>());
    }
}
