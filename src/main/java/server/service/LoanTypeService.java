package server.service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.BankDAO;
import server.DAO.LoanTypeDAO;
import server.DTO.LoanTypeDTO;
import server.Entities.Bank;
import server.Entities.LoanType;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import java.math.BigDecimal;

public class LoanTypeService {
    private final BankDAO bankDAO = new BankDAO();
    private final LoanTypeDAO loanTypeDAO = new LoanTypeDAO();

    public LoanType createLoanType(LoanTypeDTO dto) {
        Bank bank = bankDAO.findById(dto.getBankId())
                .orElseThrow(() -> new EntityNotFoundException("Bank not found"));

        LoanType type = new LoanType();
        type.setLoanTypeName(dto.getLoanTypeName());
        type.setInterestRate(dto.getInterestRate());
        type.setBank(bank);

        return loanTypeDAO.save(type);
    }

    public List<LoanTypeDTO> getLoanTypesByBankId(Long bankId) {
        Bank bank = bankDAO.findById(bankId)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found"));

        return loanTypeDAO.findByBankId(bankId).stream()
                .map(type -> convertToDto(type, bank))
                .collect(Collectors.toList());
    }

    public List<LoanTypeDTO> getLoanTypesWithFilters(Long bankId, String namePart,
                                                     BigDecimal minRate, BigDecimal maxRate) {
        return loanTypeDAO.findWithFilters(bankId, namePart, minRate, maxRate).stream()
                .map(loanType -> LoanTypeDTO.builder()
                        .loanTypeId(loanType.getLoanTypeId())
                        .loanTypeName(loanType.getLoanTypeName())
                        .interestRate(loanType.getInterestRate())
                        .bankId(loanType.getBank().getBankId())
                        .bankName(loanType.getBank().getBankName())
                        .build())
                .collect(Collectors.toList());
    }

    public LoanTypeDTO updateLoanType(LoanTypeDTO loanTypeDTO) {
        LoanType existingLoanType = loanTypeDAO.findById(loanTypeDTO.getLoanTypeId())
                .orElseThrow(() -> new NoSuchElementException("Тип кредита не найден"));

        Bank bank = bankDAO.findByBankName(loanTypeDTO.getBankName())
                .orElseThrow(() -> new NoSuchElementException("Банк '" + loanTypeDTO.getBankName() + "' не найден"));

        existingLoanType.setLoanTypeName(loanTypeDTO.getLoanTypeName());
        existingLoanType.setInterestRate(loanTypeDTO.getInterestRate());
        existingLoanType.setBank(bank);

        LoanType updatedLoanType = loanTypeDAO.save(existingLoanType);

        return convertToDTO(updatedLoanType);
    }

    private LoanTypeDTO convertToDTO(LoanType loanType) {
        LoanTypeDTO dto = new LoanTypeDTO();
        dto.setLoanTypeId(loanType.getLoanTypeId());
        dto.setLoanTypeName(loanType.getLoanTypeName());
        dto.setInterestRate(loanType.getInterestRate());
        dto.setBankId(loanType.getBank().getBankId());
        dto.setBankName(loanType.getBank().getBankName());
        return dto;
    }

    private LoanTypeDTO convertToDto(LoanType type, Bank bank) {
        return LoanTypeDTO.builder()
                .loanTypeId(type.getLoanTypeId())
                .loanTypeName(type.getLoanTypeName())
                .interestRate(type.getInterestRate())
                .bankId(bank.getBankId())
                .bankName(bank.getBankName())
                .build();
    }

    public List<LoanTypeDTO> getAllLoanTypesWithBankInfo() {
        return loanTypeDAO.getAllLoanTypesWithBankInfo();
    }

    public void deleteLoanType(long loanTypeId) {
        LoanType loanType = loanTypeDAO.findById(loanTypeId)
                .orElseThrow(() -> new EntityNotFoundException("LoanType not found with id: " + loanTypeId));

        loanTypeDAO.delete(loanType);
    }
}
