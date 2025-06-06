package server.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyRateDTO {
    private String month;
    private double averageRate;

    public MonthlyRateDTO(String month, double averageRate) {
        this.month = month;
        this.averageRate = averageRate;
    }
}
