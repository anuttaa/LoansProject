package server.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class LoanTypeDTO {
    private Long loanTypeId;
    private String loanTypeName;
    private BigDecimal interestRate;
    private Long bankId;
    private String bankName;
}

