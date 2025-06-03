package server.DTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanTypeDTO {
    private Long loanTypeId;
    private String loanTypeName;
    private BigDecimal interestRate;
    private Long bankId;
    private String bankName;

    public static class Builder {
        private Long loanTypeId;
        private String loanTypeName;
        private BigDecimal interestRate;
        private Long bankId;
        private String bankName;

        public Builder loanTypeId(Long loanTypeId) {
            this.loanTypeId = loanTypeId;
            return this;
        }

        public Builder loanTypeName(String loanTypeName) {
            this.loanTypeName = loanTypeName;
            return this;
        }

        public Builder interestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
            return this;
        }

        public Builder bankId(Long bankId) {
            this.bankId = bankId;
            return this;
        }

        public Builder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public LoanTypeDTO build() {
            return new LoanTypeDTO(loanTypeId, loanTypeName, interestRate, bankId, bankName);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

