package server.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateRangeCountDTO {
    private String rateRange; // Например "5-10%"
    private int count;

    public RateRangeCountDTO(String rateRange, int count) {
        this.rateRange = rateRange;
        this.count = count;
    }

}
