package server.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "loan_type")
public class LoanType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_type_id")
    private Long loanTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(name = "loan_type_name", nullable = false)
    private String loanTypeName;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @OneToMany(mappedBy = "loanType")
    private List<Loan> loans;
}

