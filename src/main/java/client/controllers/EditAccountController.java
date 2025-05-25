package client.controllers;

import client.MainApp;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.List;

public class EditAccountController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button showPasswordBtn;
    private TextField visiblePasswordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField nameField;
    @FXML private TextField dateField;
    @FXML private TextField addressField;
    @FXML private Label statusLabel;

    private MainApp mainApp;

    @FXML
    public void initialize() {
        visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setStyle(passwordField.getStyle());
        visiblePasswordField.setPrefSize(passwordField.getPrefWidth(), passwordField.getPrefHeight());

        ((HBox)passwordField.getParent()).getChildren().add(0, visiblePasswordField);

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordBtn.setOnAction(e -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
        } else {
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        }
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void loadUserData() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getCurrentUser");
            request.addProperty("userId", mainApp.getCurrentUserId());
            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                JsonObject user = response.getAsJsonObject("user");

                usernameField.setText(user.get("username").getAsString());
                emailField.setText(user.get("email").getAsString());

                if (user.has("fullName")) {
                    nameField.setText(user.get("fullName").getAsString());
                } else {
                    System.out.println("Warning: fullName is missing");
                }

                phoneField.setText(user.has("phone") ? user.get("phone").getAsString() : "");
                addressField.setText(user.has("address") ? user.get("address").getAsString() : "");

                if (user.has("birthDate")) {
                    dateField.setText(formatDate(user.get("birthDate").getAsString()));
                }
            } else {
                statusLabel.setText("Ошибка загрузки данных пользователя");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private String formatDate(String dbDate) {
        try {
            SimpleDateFormat fromDB = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat toUI = new SimpleDateFormat("dd.MM.yyyy");
            return toUI.format(fromDB.parse(dbDate));
        } catch (Exception e) {
            return dbDate;
        }
    }

    @FXML
    private void handleSave() {
        if (!validateAllFields()) {
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "updateUser");

            JsonObject data = new JsonObject();
            data.addProperty("username", usernameField.getText().trim());
            if (!passwordField.getText().isEmpty()) {
                data.addProperty("password", passwordField.getText());
            }
            data.addProperty("userId", mainApp.getCurrentUserId());
            data.addProperty("email", emailField.getText().trim());
            data.addProperty("phone", phoneField.getText().trim());
            data.addProperty("fullName", nameField.getText().trim());
            data.addProperty("birthDate", dateField.getText().trim());
            data.addProperty("address", addressField.getText().trim());

            request.add("data", data);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                statusLabel.setText("Данные успешно обновлены");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                String error = response != null ? response.get("message").getAsString() : "Нет ответа от сервера";
                statusLabel.setText("Ошибка: " + error);
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        resetFieldStyles();

        if (usernameField.getText().trim().isEmpty()) {
            markFieldInvalid(usernameField, "Логин обязателен");
            isValid = false;
        } else if (usernameField.getText().trim().length() < 4) {
            markFieldInvalid(usernameField, "Минимум 4 символа");
            isValid = false;
        }

        // Пароль может быть пустым (не изменяется)
        if (!passwordField.getText().isEmpty() && passwordField.getText().length() < 6) {
            markFieldInvalid(passwordField, "Минимум 6 символов");
            isValid = false;
        }

        if (emailField.getText().trim().isEmpty()) {
            markFieldInvalid(emailField, "Email обязателен");
            isValid = false;
        } else if (!isValidEmail(emailField.getText().trim())) {
            markFieldInvalid(emailField, "Некорректный email");
            isValid = false;
        }

        if (nameField.getText().trim().isEmpty()) {
            markFieldInvalid(nameField, "ФИО обязательно");
            isValid = false;
        }

        if (dateField.getText().trim().isEmpty()) {
            markFieldInvalid(dateField, "Дата рождения обязательна");
            isValid = false;
        } else if (!isValidDate(dateField.getText().trim())) {
            markFieldInvalid(dateField, "Формат даты: ДД.ММ.ГГГГ");
            isValid = false;
        }

        if (phoneField.getText().trim().isEmpty()) {
            markFieldInvalid(phoneField, "Телефон обязателен");
            isValid = false;
        } else if (!isValidPhone(phoneField.getText().trim())) {
            markFieldInvalid(phoneField, "Формат: +375XXXXXXXXX");
            isValid = false;
        }

        if (addressField.getText().trim().isEmpty()) {
            markFieldInvalid(addressField, "Адрес обязателен");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private boolean isValidDate(String date) {
        return date.matches("^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.\\d{4}$");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("^\\+375\\d{9}$");
    }

    private void markFieldInvalid(Control field, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: #ff4444;");
        Tooltip.install(field, tooltip);
    }

    private void resetFieldStyles() {
        List<Control> fields = Arrays.asList(
                usernameField, passwordField, emailField,
                phoneField, nameField, dateField, addressField
        );
        fields.forEach(field -> {
            field.setStyle("");
            Tooltip.uninstall(field, field.getTooltip());
        });
    }

    @FXML
    private void handleBack() {
        if(mainApp.getCurrentUserRoleId()==1){
            mainApp.showAdminAccountView();
            System.out.println("admin role");
        }
        else if(mainApp.getCurrentUserRoleId()==2) {
            mainApp.showAccountView();
            System.out.println("user role");
        }
        else {
            System.out.println("Invalid role");
            mainApp.clearCurrentUser();
        }
    }
}
