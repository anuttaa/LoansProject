package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
@Getter
@Setter
public class UserDTO {
    private Long userId;
    private String username;
    private String password;
    private String fullName;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;
    private Long roleId;
}

