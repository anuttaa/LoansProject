package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
@Getter
@Setter
public class UserDTO {
    private Long userId;
    private String username;
    private String password;
    private String fullName;
    private String birthDate;
    private String phone;
    private String email;
    private String address;
    private Long roleId;
    private Set<Long> bankIds;
}

