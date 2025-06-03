package server.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyRateDTO {
    private String month; // Например "Янв 2023"
    private double averageRate;

    public MonthlyRateDTO(String month, double averageRate) {
        this.month = month;
        this.averageRate = averageRate;
    }
}
