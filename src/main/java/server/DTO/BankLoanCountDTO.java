package server.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankLoanCountDTO {
    private String bankName;
    private int loanCount;

    public BankLoanCountDTO(String bankName, int loanCount) {
        this.bankName = bankName;
        this.loanCount = loanCount;
    }
    // Геттеры и сеттеры
    // ...
}
