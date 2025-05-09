package server.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.sql.Date;

@Getter
@Setter
@Entity
@Table(name = "loan")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @ManyToMany
    @JoinTable(
            name = "loan_client",
            joinColumns = @JoinColumn(name = "loan_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private Set<User> clients = new HashSet<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(name = "loan_type_name", nullable = false)
    private String loanTypeName;

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "status")
    private String status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return loanId == loan.loanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

}