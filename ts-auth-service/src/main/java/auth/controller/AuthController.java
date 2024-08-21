package auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import auth.dto.AuthDto;
import auth.service.UserService;
import edu.fudan.common.util.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fdse
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
public class AuthController
{
    @Autowired
    private UserService userService;

    /**
     * only while  user register, this method will be called by ts-user-service to create a default role use
     *
     * @return
     */
    @GetMapping("/hello")
    public String getHello()
    {
        return "hello";
    }

    @PostMapping
    public HttpEntity<Response> createDefaultUser(@RequestBody AuthDto authDto)
    {
        userService.createDefaultAuthUser(authDto);
        Response response = new Response(1, "SUCCESS", authDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

