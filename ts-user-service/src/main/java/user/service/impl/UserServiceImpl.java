package user.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.fudan.common.util.Response;
import lombok.extern.slf4j.Slf4j;
import user.dto.AuthDto;
import user.dto.UserDto;
import user.entity.User;
import user.repository.UserRepository;
import user.service.UserService;

/**
 * @author fdse
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService
{
    private static final String AUHT_SERVICE_URI = "http://ts-auth-service:12340/api/v1";

    @Autowired
    private UserRepository userRepository;

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${ts.auth.service.url:ts-auth-service}")
    private String tsAuthServiceUrl;

    @Value("${ts.auth.service.port:12340}")
    private String tsAuthServicePort;


    @Override
    public Response saveUser(UserDto userDto, HttpHeaders headers)
    {
        log.info("Save User Name id：" + userDto.getUserName());
        UUID userId = userDto.getUserId();
        if (userDto.getUserId() == null) {
            userId = UUID.randomUUID();
        }

        User user = User.builder()
            .userId(userId)
            .userName(userDto.getUserName())
            .password(userDto.getPassword())
            .gender(userDto.getGender())
            .documentType(userDto.getDocumentType())
            .documentNum(userDto.getDocumentNum())
            .email(userDto.getEmail()).build();

        // avoid same user name
        User user1 = userRepository.findByUserName(userDto.getUserName());
        if (user1 == null) {

            createDefaultAuthUser(AuthDto.builder().userId(userId + "")
                .userName(user.getUserName())
                .password(user.getPassword()).build());

            User userSaveResult = userRepository.save(user);
            log.info("Send authorization message to ts-auth-service....");

            return new Response<>(1, "REGISTER USER SUCCESS", userSaveResult);
        } else {
            return new Response(0, "USER HAS ALREADY EXISTS", null);
        }
    }

    @Override
    public Response getAllUsers(HttpHeaders headers)
    {
        List<User> users = userRepository.findAll();
        if (users != null && !users.isEmpty()) {
            return new Response<>(1, "Success", users);
        }
        return new Response<>(0, "NO User", null);
    }

    @Override
    public Response findByUserName(String userName, HttpHeaders headers)
    {
        User user = userRepository.findByUserName(userName);
        if (user != null) {
            return new Response<>(1, "Find User Success", user);
        }
        return new Response<>(0, "No User", null);
    }

    @Override
    public Response findByUserId(String userId, HttpHeaders headers)
    {
        User user = userRepository.findByUserId(UUID.fromString(userId));
        if (user != null) {
            return new Response<>(1, "Find User Success", user);
        }
        return new Response<>(0, "No User", null);
    }

    @Override
    public Response deleteUser(UUID userId, HttpHeaders headers)
    {
        log.info("DELETE USER BY ID :" + userId);
        User user = userRepository.findByUserId(userId);
        if (user != null) {
            // first  only admin token can delete success
            deleteUserAuth(userId, headers);
            // second
            userRepository.deleteByUserId(userId);
            log.info("DELETE SUCCESS");
            return new Response<>(1, "DELETE SUCCESS", null);
        } else {
            return new Response<>(0, "USER NOT EXISTS", null);
        }
    }

    @Override
    public Response updateUser(UserDto userDto, HttpHeaders headers)
    {
        log.info("UPDATE USER :" + userDto.toString());
        User oldUser = userRepository.findByUserName(userDto.getUserName());
        if (oldUser != null) {
            User newUser = User.builder().email(userDto.getEmail())
                .password(userDto.getPassword())
                .userId(oldUser.getUserId())
                .userName(userDto.getUserName())
                .gender(userDto.getGender())
                .documentNum(userDto.getDocumentNum())
                .documentType(userDto.getDocumentType()).build();
            userRepository.deleteByUserId(oldUser.getUserId());
            userRepository.save(newUser);
            return new Response<>(1, "SAVE USER SUCCESS", newUser);
        } else {
            return new Response(0, "USER NOT EXISTS", null);
        }
    }

    public void deleteUserAuth(UUID userId, HttpHeaders headers)
    {
        log.info("DELETE USER BY ID :" + userId);

        HttpEntity<Response> httpEntity = new HttpEntity<>(headers);
        restTemplate.exchange("http://" + tsAuthServiceUrl + ":" + tsAuthServicePort + "/api/v1" + "/users/" + userId,
            HttpMethod.DELETE,
            httpEntity,
            Response.class);
        log.info("DELETE USER AUTH SUCCESS");
    }

    private Response createDefaultAuthUser(AuthDto dto)
    {
        log.info("CALL TO AUTH");
        log.info("AuthDto : " + dto.toString());
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<AuthDto> entity = new HttpEntity<>(dto, headers);
        ResponseEntity<Response<AuthDto>> res = restTemplate.exchange("http://" + tsAuthServiceUrl + ":" + tsAuthServicePort + "/api/v1/auth",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Response<AuthDto>>()
            {
            });
        return res.getBody();
    }
}
