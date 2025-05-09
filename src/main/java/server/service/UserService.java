package server.service;

import org.hibernate.Hibernate;
import server.DAO.UserDAO;
import server.DAO.RoleDAO;
import server.DAO.BankDAO;
import server.DTO.UserDTO;
import server.Entities.Bank;
import server.Entities.Role;
import server.Entities.User;
import exeption.AuthExeption;
import server.Security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class UserService implements Service<UserDTO, Long> {
    private final UserDAO userDAO = new UserDAO();
    private final PasswordHasher passwordHasher = new PasswordHasher();
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Override
    public UserDTO save(UserDTO userDTO) {
        LOG.debug("Сохранение пользователя: {}", userDTO.getUsername());
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPasswordHash(userDTO.getPassword());
        user.setFullName(userDTO.getFullName());
        user.setBirthDate(userDTO.getBirthDate());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setAddress(userDTO.getAddress());

        user = userDAO.save(user);
        LOG.info("Пользователь сохранён с ID: {}", user.getUserId());

        userDTO.setUserId(user.getUserId());
        return userDTO;
    }

    @Override
    public Optional<UserDTO> findById(Long id) {
        LOG.debug("Поиск пользователя по ID: {}", id);
        Optional<User> user = userDAO.findById(id);
        if (user.isPresent()) {
            UserDTO dto = new UserDTO();
            dto.setUserId(user.get().getUserId());
            dto.setUsername(user.get().getUsername());
            dto.setFullName(user.get().getFullName());
            dto.setEmail(user.get().getEmail());
            dto.setBankIds(user.get().getBanks().stream().map(Bank::getBankId).collect(Collectors.toSet()));
            LOG.info("Пользователь найден: {}", dto.getUsername());
            return Optional.of(dto);
        }
        LOG.warn("Пользователь с ID {} не найден", id);
        return Optional.empty();
    }

    @Override
    public List<UserDTO> findAll() {
        LOG.debug("Получение списка всех пользователей");
        List<User> users = userDAO.findAll();
        LOG.info("Найдено пользователей: {}", users.size());
        return users.stream().map(user -> {
            UserDTO dto = new UserDTO();
            dto.setUserId(user.getUserId());
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getFullName());
            dto.setPassword(user.getPasswordHash());
            dto.setBirthDate(user.getBirthDate());
            dto.setPhone(user.getPhone());
            dto.setAddress(user.getAddress());
            dto.setRoleId(user.getRole().getRoleId());
            dto.setEmail(user.getEmail());
            dto.setBankIds(user.getBanks().stream().map(Bank::getBankId).collect(Collectors.toSet()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public UserDTO update(UserDTO userDTO) {
        LOG.debug("Обновление пользователя с ID: {}", userDTO.getUserId());

        User user = userDAO.findById(userDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDTO.getUsername() != null) user.setUsername(userDTO.getUsername());
        if (userDTO.getPassword() != null) user.setPasswordHash(userDTO.getPassword());
        if (userDTO.getFullName() != null) user.setFullName(userDTO.getFullName());
        if (userDTO.getBirthDate() != null) user.setBirthDate(userDTO.getBirthDate());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if (userDTO.getAddress() != null) user.setAddress(userDTO.getAddress());

        userDAO.update(user);
        LOG.info("Пользователь обновлён: {}", userDTO.getUserId());

        return mapToDTO(user);
    }

    @Override
    public void delete(UserDTO userDTO) {
        LOG.debug("Удаление пользователя с ID: {}", userDTO.getUserId());
        User user = userDAO.findById(userDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        userDAO.delete(user);
        LOG.info("Пользователь с ID {} удалён", userDTO.getUserId());
    }

    public User register(UserDTO userDTO) throws AuthExeption {
        LOG.debug("Регистрация пользователя: {}", userDTO.getUsername());
        if (userDTO.getUsername() == null || userDTO.getPassword() == null || userDTO.getFullName() == null || userDTO.getRoleId() == null) {
            LOG.warn("Не все обязательные поля заполнены для пользователя: {}", userDTO);
            throw new AuthExeption("Не все обязательные поля заполнены");
        }
        LOG.info("=== REGISTRATION DEBUG ===");
        LOG.info("Raw password: [{}]", userDTO.getPassword());


        User user = new User();
        user.setUsername(userDTO.getUsername());
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashedPassword = passwordHasher.hash(userDTO.getPassword());
        LOG.info("Generated hash: [{}]", hashedPassword);
        user.setPasswordHash(hashedPassword);
        user.setFullName(userDTO.getFullName());
        user.setBirthDate(userDTO.getBirthDate());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setAddress(userDTO.getAddress());

        RoleDAO roleDAO = new RoleDAO();
        BankDAO bankDAO = new BankDAO();

        Role role = roleDAO.findById(userDTO.getRoleId())
                .orElseThrow(() -> new AuthExeption("Роль с id " + userDTO.getRoleId() + " не найдена"));
        user.setRole(role);

        Set<Bank> banks = new HashSet<>();
        if (userDTO.getBankIds() != null) {
            for (Long bankId : userDTO.getBankIds()) {
                Bank bank = bankDAO.findById(bankId)
                        .orElseThrow(() -> new AuthExeption("Банк с id " + bankId + " не найден"));
                banks.add(bank);
            }
        }
        user.setBanks(banks);

        User savedUser = userDAO.save(user);
        LOG.info("Пользователь успешно зарегистрирован с ID: {}", savedUser.getUserId());
        return savedUser;
    }

    public UserDTO login(String username, String password) throws AuthExeption {
        LOG.debug("Attempting login for: {}", username);

        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthExeption("User not found"));

        LOG.debug("Input password: [{}]", password);
        LOG.debug("Stored hash: [{}]", user.getPasswordHash());
        LOG.debug("Hash length: {}", user.getPasswordHash().length());
        debugPasswordCheck(username, password);
        try {
            boolean matches = passwordHasher.verify(password, user.getPasswordHash());
            LOG.debug("Password verification result: {}", matches);

            if (!matches) {
                String testHash = passwordHasher.hash(password);
                LOG.debug("Newly generated hash: [{}]", testHash);
                LOG.debug("New hash length: {}", testHash.length());

                throw new AuthExeption("Invalid password");
            }

            return mapToDTO(user);
        } catch (Exception e) {
            LOG.error("Password verification failed", e);
            throw new AuthExeption("Authentication error");
        }
    }
    public void debugPasswordCheck(String username, String password) {
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LOG.info("=== PASSWORD DEBUG ===");
        LOG.info("Input password: [{}]", password);
        LOG.info("Stored hash: [{}]", user.getPasswordHash());

        String newHash = passwordHasher.hash(password);
        LOG.info("Newly generated hash: [{}]", newHash);

        LOG.info("Verification result: {}", passwordHasher.verify(password, user.getPasswordHash()));
        LOG.info("Compare hashes directly: {}", user.getPasswordHash().equals(newHash));
    }
    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setBirthDate(user.getBirthDate());
        dto.setRoleId(user.getRole() != null ? user.getRole().getRoleId() : null);

        if (user.getBanks() != null) {
            Set<Long> bankIds = user.getBanks()
                    .stream()
                    .map(Bank::getBankId)
                    .collect(Collectors.toSet());
            dto.setBankIds(bankIds);
        }

        return dto;
    }

    public void addBankToUser(Long userId, Long bankId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        BankDAO bankDAO = new BankDAO();
        Bank bank = bankDAO.findById(bankId)
                .orElseThrow(() -> new NoSuchElementException("Bank not found"));

        Set<Bank> userBanks = user.getBanks();
        if (userBanks == null) {
            userBanks = new HashSet<>();
            user.setBanks(userBanks);
        }

        if (!userBanks.contains(bank)) {
            userBanks.add(bank);
        }
        userDAO.update(user);
    }
}

