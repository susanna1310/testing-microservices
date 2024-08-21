package user.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author fdse
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDto
{
    private UUID userId;

    private String userName;

    private String password;

    private int gender;

    private int documentType;

    private String documentNum;

    private String email;
}
