package auth.init;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import auth.entity.User;
import auth.repository.UserRepository;

/**
 * @author fdse
 */
@Component
public class InitUser implements CommandLineRunner
{
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... strings) throws Exception
    {
        User whetherExistUser = userRepository.findByUsername("fdse_microservice").orElse(new User());
        if (whetherExistUser.getUsername() == null) {
            User user = User.builder()
                .userId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"))
                .username("fdse_microservice")
                .password(passwordEncoder.encode("111111"))
                .roles(new HashSet<>(Arrays.asList("ROLE_USER")))
                .build();
            userRepository.save(user);
        }

        User whetherExistAdmin = userRepository.findByUsername("admin").orElse(new User());
        if (whetherExistAdmin.getUsername() == null) {
            User admin = User.builder()
                .userId(UUID.randomUUID())
                .username("admin")
                .password(passwordEncoder.encode("222222"))
                .roles(new HashSet<>(Arrays.asList("ROLE_ADMIN")))
                .build();
            userRepository.save(admin);
        }
    }
}
