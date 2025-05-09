package server.service;

import server.DAO.RoleDAO;
import server.DAO.UserDAO;
import server.Entities.Role;
import server.Entities.User;

import java.util.List;
import java.util.NoSuchElementException;

public class RoleService {
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    public void assignRole(Long currentUserId, Long targetUserId, String roleName) {
        User currentUser = userDAO.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("Текущий пользователь не найден"));

        if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
            throw new SecurityException("У вас нет прав для назначения ролей");
        }

        User targetUser = userDAO.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Целевой пользователь не найден"));

        Role newRole = roleDAO.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Роль не найдена: " + roleName));

        targetUser.setRole(newRole);
        userDAO.update(targetUser);
    }

    public Role createRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return roleDAO.save(role);
    }

    public boolean isAdmin(Long userId) {
        return userDAO.findById(userId)
                .map(user -> "ADMIN".equals(user.getRole().getRoleName()))
                .orElse(false);
    }

    public String getUserRoleName(Long userId) {
        return userDAO.findById(userId)
                .map(user -> user.getRole().getRoleName())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
    }

    public List<Role> getAllRoles() {
        return roleDAO.findAll();
    }


}

