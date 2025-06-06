package server.DAO;

import org.hibernate.Session;
import server.DTO.LoanTypeDTO;
import server.DTO.RateRangeCountDTO;
import server.Entities.LoanType;

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LoanTypeDAO extends AbstractDAO<LoanType, Long> {

    public LoanTypeDAO() {
        super(LoanType.class);
    }

    public List<LoanType> findByBankId(Long bankId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM LoanType WHERE bank.bankId = :bankId", LoanType.class)
                    .setParameter("bankId", bankId)
                    .list();
        }
    }

    public List<LoanType> findWithFilters(Long bankId, String namePart,
                                          BigDecimal minRate, BigDecimal maxRate) {
        try (Session session = getCurrentSession()) {
            StringBuilder hql = new StringBuilder("FROM LoanType lt WHERE 1=1");

            if (bankId != null) {
                hql.append(" AND lt.bank.bankId = :bankId");
            }
            if (namePart != null && !namePart.isEmpty()) {
                hql.append(" AND LOWER(lt.loanTypeName) LIKE LOWER(:namePart)");
            }
            if (minRate != null) {
                hql.append(" AND lt.interestRate >= :minRate");
            }
            if (maxRate != null) {
                hql.append(" AND lt.interestRate <= :maxRate");
            }

            var query = session.createQuery(hql.toString(), LoanType.class);

            if (bankId != null) {
                query.setParameter("bankId", bankId);
            }
            if (namePart != null && !namePart.isEmpty()) {
                query.setParameter("namePart", "%" + namePart + "%");
            }
            if (minRate != null) {
                query.setParameter("minRate", minRate);
            }
            if (maxRate != null) {
                query.setParameter("maxRate", maxRate);
            }

            return query.list();
        }
    }

    public List<LoanTypeDTO> getAllLoanTypesWithBankInfo() {
        try (Session session = getCurrentSession()) {
            String hql = "SELECT lt.loanTypeId, lt.loanTypeName, lt.interestRate, " +
                    "b.bankId, b.bankName " +
                    "FROM LoanType lt JOIN lt.bank b";

            List<Object[]> results = session.createQuery(hql, Object[].class).list();

            return results.stream()
                    .map(o -> LoanTypeDTO.builder()
                            .loanTypeId((Long) o[0])
                            .loanTypeName((String) o[1])
                            .interestRate((BigDecimal) o[2])
                            .bankId((Long) o[3])
                            .bankName((String) o[4])
                            .build())
                    .collect(Collectors.toList());
        }
    }

    public Optional<LoanType> findByIdWithAllRelations(Long loanTypeId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT lt FROM LoanType lt " +
                                    "LEFT JOIN FETCH lt.bank " +
                                    "WHERE lt.loanTypeId = :loanTypeId", LoanType.class)
                    .setParameter("loanTypeId", loanTypeId)
                    .uniqueResultOptional();
        }
    }

    public List<RateRangeCountDTO> getRateDistribution() {
        try (Session session = getCurrentSession()) {
            List<Object[]> results = session.createQuery(
                            "SELECT " +
                                    "CASE " +
                                    "  WHEN lt.interestRate < 5 THEN '0-5%' " +
                                    "  WHEN lt.interestRate BETWEEN 5 AND 10 THEN '5-10%' " +
                                    "  WHEN lt.interestRate BETWEEN 10 AND 15 THEN '10-15%' " +
                                    "  WHEN lt.interestRate > 15 THEN '15%+' " +
                                    "END as rateRange, " +
                                    "COUNT(lt) " +
                                    "FROM LoanType lt " +
                                    "GROUP BY rateRange", Object[].class)
                    .list();

            return results.stream()
                    .map(o -> new RateRangeCountDTO((String) o[0], ((Long) o[1]).intValue()))
                    .collect(Collectors.toList());
        }
    }

    public Optional<LoanType> findById(Long loanTypeId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "FROM LoanType WHERE loanTypeId = :loanTypeId", LoanType.class)
                    .setParameter("loanTypeId", loanTypeId)
                    .uniqueResultOptional();
        }
    }

    public boolean hasActiveLoans(Long loanTypeId) {
        try (Session session = getCurrentSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(l) FROM Loan l WHERE l.loanType.loanTypeId = :loanTypeId", Long.class)
                    .setParameter("loanTypeId", loanTypeId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    @Override
    public void deleteById(Long loanTypeId) {
        try (Session session = getCurrentSession()) {
            LoanType loanType = session.get(LoanType.class, loanTypeId);
            if (loanType != null) {
                session.remove(loanType);
            }
        }
    }
}
