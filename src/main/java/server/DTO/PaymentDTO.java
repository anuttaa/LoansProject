package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentDTO {
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentType;
}
