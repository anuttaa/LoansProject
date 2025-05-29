package server.DTO;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class PaymentScheduleDTO {
    private Integer paymentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private BigDecimal principalPart;
    private BigDecimal interestPart;

    public IntegerProperty paymentNumberProperty() {
        return new SimpleIntegerProperty(paymentNumber);
    }

    public StringProperty formattedAmountProperty() {
        return new SimpleStringProperty(String.format("%,.2f ₽", amount));
    }

    public StringProperty formattedDueDateProperty() {
        return new SimpleStringProperty(dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
}


