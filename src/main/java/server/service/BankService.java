package server.service;

import server.DAO.BankDAO;
import server.DAO.LoanDAO;
import server.DAO.LoanTypeDAO;
import server.DTO.BankDTO;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.LoanType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BankService {
    private static final Logger LOGGER = Logger.getLogger(BankService.class.getName());

    private final BankDAO bankRepository = new BankDAO();
    private final LoanTypeDAO loanTypeRepository = new LoanTypeDAO();

    public Bank createBank(BankDTO bankDTO) {
        Objects.requireNonNull(bankDTO, "BankDTO cannot be null");
        LOGGER.info(() -> "Creating new bank: " + bankDTO.getBankName());

        Bank bank = new Bank();
        bank.setBankName(bankDTO.getBankName());
        bank.setAddress(bankDTO.getAddress());
        bank.setPhone(bankDTO.getPhone());
        bank.setEmail(bankDTO.getEmail());

        Bank savedBank = bankRepository.save(bank);
        LOGGER.info(() -> "Bank created successfully with ID: " + savedBank.getBankId());
        return savedBank;
    }

    public List<BankDTO> findAll() {
        LOGGER.info("Retrieving all banks");
        return bankRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BankDTO updateBank(Long bankId, BankDTO bankDTO) {
        Objects.requireNonNull(bankId, "Bank ID cannot be null");
        Objects.requireNonNull(bankDTO, "BankDTO cannot be null");
        LOGGER.info(() -> "Updating bank with ID: " + bankId);

        Bank existingBank = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning(() -> "Bank not found for update with ID: " + bankId);
                    return new NoSuchElementException("Bank not found with ID: " + bankId);
                });

        existingBank.setBankName(bankDTO.getBankName());
        existingBank.setAddress(bankDTO.getAddress());
        existingBank.setPhone(bankDTO.getPhone());
        existingBank.setEmail(bankDTO.getEmail());

        Bank updatedBank = bankRepository.update(existingBank);
        LOGGER.info(() -> "Bank updated successfully with ID: " + updatedBank.getBankId());
        return convertToDTO(updatedBank);
    }

    private BankDTO convertToDTO(Bank bank) {
        BankDTO dto = new BankDTO();
        dto.setBankId(bank.getBankId());
        dto.setBankName(bank.getBankName());
        dto.setAddress(bank.getAddress());
        dto.setPhone(bank.getPhone());
        dto.setEmail(bank.getEmail());
        return dto;
    }
}



