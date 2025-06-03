package server.service;

import config.HibernateConfig;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import server.DAO.UserDAO;
import server.DAO.RoleDAO;
import server.DAO.BankDAO;
import server.DTO.UserDTO;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.Role;
import server.Entities.User;
import server.Security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import exeption.AuthExeption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class UserService implements Service<UserDTO, Long> {
    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final BankDAO bankDAO;
    private final PasswordHasher passwordHasher;
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private final SessionFactory sessionFactory;


    // Метод для получения текущей сессии
    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }


    public UserService() {
        this.userDAO = new UserDAO();
        this.roleDAO = new RoleDAO();
        this.bankDAO = new BankDAO();
        this.passwordHasher = new PasswordHasher();
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public UserDTO save(UserDTO userDTO) {
        validateUserDTO(userDTO);
        User user = convertToEntity(userDTO);
        user = userDAO.save(user);
        return convertToDTO(user);
    }

    @Override
    public Optional<UserDTO> findById(Long id) {
        return userDAO.findById(id).map(this::convertToDTO);
    }

    @Override
    public List<UserDTO> findAll() {
        return userDAO.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO update(UserDTO userDTO) {
        User existingUser = userDAO.findById(userDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        updateUserFromDTO(existingUser, userDTO);
        userDAO.update(existingUser);
        return convertToDTO(existingUser);
    }

    @Override
    public void delete(UserDTO userDTO) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                // 1. Получаем пользователя с блокировкой
                User user = session.get(User.class, userDTO.getUserId(), LockMode.PESSIMISTIC_WRITE);
                if (user == null) {
                    throw new EntityNotFoundException("User not found");
                }

                // 2. Удаляем все связанные платежи и кредиты
                session.createQuery("DELETE FROM Payment p WHERE p.loan.client.userId = :userId")
                        .setParameter("userId", userDTO.getUserId())
                        .executeUpdate();

                session.createQuery("DELETE FROM Loan l WHERE l.client.userId = :userId")
                        .setParameter("userId", userDTO.getUserId())
                        .executeUpdate();

                // 3. Удаляем самого пользователя
                session.createQuery("DELETE FROM User u WHERE u.userId = :userId")
                        .setParameter("userId", userDTO.getUserId())
                        .executeUpdate();

                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                throw new RuntimeException("Ошибка при удалении пользователя: " + e.getMessage(), e);
            }
        }
    }

    public User register(UserDTO userDTO) throws AuthExeption {
        validateRegistration(userDTO);

        User user = convertToEntity(userDTO);
        setUserRole(user, userDTO.getRoleId());

        return userDAO.save(user);
    }

    public UserDTO login(String username, String password) throws AuthExeption {
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthExeption("Invalid credentials"));

        if (!passwordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthExeption("Invalid credentials");
        }

        return convertToDTO(user);
    }

    public void initializeFirstAdmin() {
        if (userDAO.count() == 0) {
            UserDTO adminDTO = createAdminDTO();
            try {
                User admin = register(adminDTO);
                LOG.info("Created first admin with ID: {}", admin.getUserId());
            } catch (AuthExeption e) {
                LOG.error("Failed to create first admin", e);
            }
        }
    }

    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordHasher.hash(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setBirthDate(dto.getBirthDate());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        return user;
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setBirthDate(user.getBirthDate());
        dto.setRoleId(user.getRole().getRoleId());

        return dto;
    }

    private void updateUserFromDTO(User user, UserDTO dto) {
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getPassword() != null) user.setPasswordHash(passwordHasher.hash(dto.getPassword()));
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getBirthDate() != null) user.setBirthDate(dto.getBirthDate());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
    }

    private void validateUserDTO(UserDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private void validateRegistration(UserDTO dto) throws AuthExeption {
        if (dto.getRoleId() == 1 && userDAO.count() > 0) {
            throw new AuthExeption("Admin registration is not allowed");
        }
        if (userDAO.findByUsername(dto.getUsername()).isPresent()) {
            throw new AuthExeption("Логин уже существует!");
        }
    }

    private void setUserRole(User user, Long roleId) throws AuthExeption {
        Role role = roleDAO.findById(roleId)
                .orElseThrow(() -> {
                    LOG.error("Role not found with id: {}", roleId);
                    return new AuthExeption(String.format("Role not found with id: %d", roleId));
                });
        user.setRole(role);
    }

    private User getUserById(Long userId) {
        return userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Bank getBankById(Long bankId) {
        return bankDAO.findById(bankId)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found"));
    }

    private UserDTO createAdminDTO() {
        UserDTO adminDTO = new UserDTO();
        adminDTO.setUsername("admin");
        adminDTO.setPassword("admin");
        adminDTO.setFullName("System Administrator");
        adminDTO.setEmail("admin@bank.com");
        adminDTO.setPhone("+375000000000");
        adminDTO.setAddress("Headquarters");
        adminDTO.setBirthDate(LocalDate.now().minusYears(30));
        adminDTO.setRoleId(1L);
        return adminDTO;
    }
}

