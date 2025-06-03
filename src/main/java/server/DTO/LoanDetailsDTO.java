package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
public class LoanDetailsDTO {
    private Long loanId;
    private BigDecimal loanAmount;
    private Integer termMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LoanTypeDTO loanType;
    private ClientDTO client;
    private BankDTO bank;
    private List<PaymentScheduleDTO> schedule;
    private BigDecimal remainingDebt;
    private PaymentScheduleDTO nextPayment;

    // Геттеры и сеттеры
}
