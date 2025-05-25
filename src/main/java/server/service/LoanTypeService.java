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

    public List<LoanTypeDTO> getAllLoanTypes() {
        return loanTypeDAO.findAll().stream()
                .map(this::convertToDto)
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

    public List<Bank> getAllBanks() {
        return bankDAO.findAll();
    }

    private LoanTypeDTO convertToDto(LoanType type) {
        return convertToDto(type, type.getBank());
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
}
