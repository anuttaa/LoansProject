package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentScheduleDTO {
    private int paymentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
}
