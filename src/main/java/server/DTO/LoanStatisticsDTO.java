package server.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoanStatisticsDTO {
    private double averageRate;
    private String mostPopularBank;
    private int totalLoans;
    private double totalAmount;
    private List<BankLoanCountDTO> loansByBank;
    private List<RateRangeCountDTO> ratesDistribution;
    private List<MonthlyRateDTO> ratesTrend;
}
