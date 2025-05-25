package server.DAO;

import org.hibernate.Session;
import server.DTO.LoanTypeDTO;
import server.Entities.LoanType;

import java.util.List;
import java.math.BigDecimal;
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

    public Optional<LoanType> findByTypeName(String typeName) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM LoanType WHERE loanTypeName = :typeName", LoanType.class)
                    .setParameter("typeName", typeName)
                    .uniqueResultOptional();
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

    public List<LoanType> findByInterestRateBetween(BigDecimal minRate, BigDecimal maxRate) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "FROM LoanType WHERE interestRate BETWEEN :minRate AND :maxRate",
                            LoanType.class)
                    .setParameter("minRate", minRate)
                    .setParameter("maxRate", maxRate)
                    .list();
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
}
