package server.service;

import server.DAO.LoanDAO;
import server.DAO.LoanTypeDAO;
import server.DTO.LoanStatisticsDTO;

public class LoanStatisticsService {
    private final LoanDAO loanRepository = new LoanDAO();
    private final LoanTypeDAO loanTypeRepository = new LoanTypeDAO();

    public LoanStatisticsDTO getLoanStatistics() {
        LoanStatisticsDTO stats = new LoanStatisticsDTO();

        stats.setAverageRate(loanRepository.getAverageInterestRate());
        stats.setMostPopularBank(loanRepository.getMostPopularBank());
        stats.setTotalLoans(loanRepository.getTotalLoanCount());
        stats.setTotalAmount(loanRepository.getTotalLoanAmount());

        stats.setLoansByBank(loanRepository.getLoanCountByBank());

        stats.setRatesDistribution(loanTypeRepository.getRateDistribution());

        stats.setRatesTrend(loanRepository.getMonthlyRateTrend());

        return stats;
    }
}
