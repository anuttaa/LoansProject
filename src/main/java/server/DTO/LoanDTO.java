package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Getter
@Setter
public class LoanDTO {
    private Long loanId;
    private Long clientId;
    private Long loanTypeId;
    private Long bankId;
    private String loanTypeName;
    private BigDecimal interestRate;
    private BigDecimal loanAmount;
    private Integer termMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public void setLoanAmount(BigDecimal loanAmount) {
        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма кредита должна быть положительной");
        }
        this.loanAmount = loanAmount;
    }

    public void setTermMonths(Integer termMonths) {
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Срок кредита должен быть положительным");
        }
        this.termMonths = termMonths;
    }
}
