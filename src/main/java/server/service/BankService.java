package server.service;

import server.DAO.BankDAO;
import server.DAO.LoanDAO;
import server.DTO.BankDTO;
import server.Entities.Bank;
import server.Entities.Loan;

import java.util.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BankService {

    private static final Logger LOGGER = Logger.getLogger(BankService.class.getName());

    private final BankDAO bankRepository = new BankDAO();
    private final LoanDAO loanRepository = new LoanDAO();

    public Bank createBank(BankDTO bankDTO) {
        LOGGER.info("Создание нового банка: " + bankDTO.getBankName());

        Bank bank = new Bank();
        bank.setBankName(bankDTO.getBankName());
        bank.setAddress(bankDTO.getAddress());
        bank.setPhone(bankDTO.getPhone());
        bank.setEmail(bankDTO.getEmail());

        Bank saved = bankRepository.save(bank);
        LOGGER.info("Банк успешно создан с ID: " + saved.getBankId());
        return saved;
    }

    public List<Loan> getBankLoans(Long bankId) {
        LOGGER.info("Получение кредитных продуктов для банка с ID: " + bankId);
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning("Банк с ID " + bankId + " не найден");
                    return new NoSuchElementException("Банк с id " + bankId + " не найден");
                });

        return new ArrayList<>(bank.getLoans());
    }

    public double calculateEffectiveRate(Long bankId, double amount, int term) {
        LOGGER.info("Расчёт ЭПС для банка ID " + bankId + ", сумма: " + amount + ", срок: " + term);
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning("Банк с ID " + bankId + " не найден");
                    return new NoSuchElementException("Банк с id " + bankId + " не найден");
                });

        Set<Loan> loans = bank.getLoans();
        if (loans.isEmpty()) {
            LOGGER.warning("У банка ID " + bankId + " нет кредитных продуктов");
            throw new IllegalStateException("У банка нет кредитных продуктов");
        }

        Loan bestLoan = loans.stream()
                .min(Comparator.comparingDouble(Loan::getInterestRate))
                .orElseThrow(() -> {
                    LOGGER.warning("Нет доступных кредитов у банка ID " + bankId);
                    return new IllegalStateException("Нет доступных кредитов");
                });

        double baseRate = bestLoan.getInterestRate();
        double commission = 1.0;

        double result = baseRate + commission;
        LOGGER.info("ЭПС рассчитана: " + result);
        return result;
    }

    public BankDTO findById(Long bankId) {
        LOGGER.info("Поиск банка по ID: " + bankId);
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning("Банк с ID " + bankId + " не найден");
                    return new NoSuchElementException("Банк с id " + bankId + " не найден");
                });
        return convertToDTO(bank);
    }


    public List<BankDTO> findAll() {
        LOGGER.info("Получение списка всех банков");
        return bankRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BankDTO updateBank(Long bankId, BankDTO bankDTO) {
        LOGGER.info("Обновление банка с ID: " + bankId);
        Bank existing = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning("Банк с ID " + bankId + " не найден для обновления");
                    return new NoSuchElementException("Банк с id " + bankId + " не найден");
                });

        existing.setBankName(bankDTO.getBankName());
        existing.setAddress(bankDTO.getAddress());
        existing.setPhone(bankDTO.getPhone());
        existing.setEmail(bankDTO.getEmail());

        Bank updated = bankRepository.update(existing);
        LOGGER.info("Банк обновлён: " + updated.getBankId());
        return convertToDTO(updated);
    }


    public void deleteBank(Long bankId) {
        LOGGER.info("Удаление банка с ID: " + bankId);
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> {
                    LOGGER.warning("Банк с ID " + bankId + " не найден для удаления");
                    return new NoSuchElementException("Банк с id " + bankId + " не найден");
                });
        bankRepository.delete(bank);
        LOGGER.info("Банк с ID " + bankId + " успешно удалён");
    }


    public BankDTO convertToDTO(Bank bank) {
        BankDTO dto = new BankDTO();
        dto.setBankId(bank.getBankId());
        dto.setBankName(bank.getBankName());
        dto.setAddress(bank.getAddress());
        dto.setPhone(bank.getPhone());
        dto.setEmail(bank.getEmail());
        return dto;
    }
}



