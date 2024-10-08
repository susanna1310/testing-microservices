package auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import auth.dto.BasicAuthDto;
import auth.entity.User;
import auth.service.TokenService;
import auth.service.UserService;
import edu.fudan.common.util.Response;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController
{
    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/hello")
    public Object getHello()
    {
        return "Hello";
    }

    @PostMapping("/login")
    @HystrixCommand(fallbackMethod = "getTokenFallback", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    })
    public ResponseEntity<Response> getToken(@RequestBody BasicAuthDto dao, @RequestHeader HttpHeaders headers)
    {
        return ResponseEntity.ok(tokenService.getToken(dao, headers));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUser(@RequestHeader HttpHeaders headers)
    {
        return ResponseEntity.ok().body(userService.getAllUser(headers));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Response> deleteUserById(@PathVariable String userId, @RequestHeader HttpHeaders headers)
    {
        return ResponseEntity.ok(userService.deleteByUserId(UUID.fromString(userId), headers));
    }

    private ResponseEntity<Response> getTokenFallback(@RequestBody BasicAuthDto dao, @RequestHeader HttpHeaders headers)
    {
        return new ResponseEntity<>(new Response<>(0, "Verification failed.", null), HttpStatus.OK);
    }
}
