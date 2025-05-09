package server.DTO;

import lombok.Getter;
import lombok.Setter;
import java.sql.Date;

@Getter
@Setter
public class LoanDTO {
    private String loanTypeName;
    private Double interestRate;
    private Double loanAmount;
    private Integer termMonths;
    private Date startDate;
    private Date endDate;
    private String status;
}
