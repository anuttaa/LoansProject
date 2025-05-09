package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
@Getter
@Setter
public class PaymentDTO {
    private Date paymentDate;
    private Double amount;
    private String paymentType;
}
