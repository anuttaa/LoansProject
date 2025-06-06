package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentScheduleDTO {
    private Integer paymentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private BigDecimal principalPart;
    private BigDecimal interestPart;
}


