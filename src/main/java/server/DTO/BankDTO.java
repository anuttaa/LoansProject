package server.DTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankDTO {
    private Long bankId;
    private String bankName;
    private String address;
    private String phone;
    private String email;
}

