package client.controllers;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import client.MainApp;


import java.util.Arrays;
import java.util.List;

public class RegisterController {
    @FXML public Button register;
    @FXML public TextField nameField;
    @FXML public TextField dateField;
    @FXML public TextField phoneField;
    @FXML public TextField addressField;
    @FXML public Button backButton;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleRegister() {
        try {
            if (!validateAllFields()) {
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("command", "register");

            JsonObject userData = new JsonObject();
            userData.addProperty("username", loginField.getText().trim());
            userData.addProperty("password", passwordField.getText().trim());
            userData.addProperty("email", emailField.getText().trim());
            userData.addProperty("fullName", nameField.getText().trim());
            userData.addProperty("birthDate", dateField.getText().trim());
            userData.addProperty("phone", phoneField.getText().trim());
            userData.addProperty("address", addressField.getText().trim());
            userData.addProperty("roleId", 2);

            request.add("user", userData);

            System.out.println("Отправка запроса: " + request);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response == null) {
                showError("Нет ответа от сервера");
            } else if (response.has("error")) {
                showError(response.get("error").getAsString());
            } else {
                showSuccess("Регистрация прошла успешно!");
                closeWindow();
                mainApp.showMainView();
            }
        } catch (Exception e) {
            showError("Ошибка регистрации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        resetFieldStyles();

        if (loginField.getText().trim().isEmpty()) {
            markFieldInvalid(loginField, "Логин обязателен");
            isValid = false;
        } else if (loginField.getText().trim().length() < 4) {
            markFieldInvalid(loginField, "Минимум 4 символа");
            isValid = false;
        }

        if (passwordField.getText().trim().isEmpty()) {
            markFieldInvalid(passwordField, "Пароль обязателен");
            isValid = false;
        } else if (passwordField.getText().length() < 6) {
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
        List<Control> fields = Arrays.asList(loginField, passwordField, emailField,
                nameField, dateField, phoneField, addressField);
        fields.forEach(field -> {
            field.setStyle("");
            Tooltip.uninstall(field, field.getTooltip());
        });
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успешно");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        loginField.getScene().getWindow().hide();
    }

    @FXML
    private void handleBack() {
        mainApp.showMainView();
    }
}
