package server.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "payment_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_type")
    private String paymentType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return paymentId == payment.paymentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", loan=" + loan +
                ", paymentDate=" + paymentDate +
                ", amount=" + amount +
                ", paymentType='" + paymentType + '\'' +
                '}';
    }
}