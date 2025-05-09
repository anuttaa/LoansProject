package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
@Getter
@Setter
public class PaymentScheduleDTO {
    private int paymentNumber;
    private double amount;
    private Date dueDate;
}
