package client.controllers;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import client.MainApp;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
            String formattedDate = convertDateToISO(dateField.getText().trim());
            userData.addProperty("birthDate", formattedDate);
            userData.addProperty("phone", phoneField.getText().trim());
            userData.addProperty("address", addressField.getText().trim());
            userData.addProperty("roleId", 2);

            request.add("user", userData);

            System.out.println("Отправка запроса: " + request);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response == null) {
                showError("Нет ответа от сервера");
            } else if (response.get("status").getAsString().equals("error")) {
                showError(response.get("message").getAsString());
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

    private String convertDateToISO(String dateString) throws IllegalArgumentException {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate date = LocalDate.parse(dateString, inputFormatter);

            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return date.format(outputFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте ДД.ММ.ГГГГ");
        }
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        resetFieldStyles();

        String login = loginField.getText().trim();
        if (login.isEmpty()) {
            markFieldInvalid(loginField, "Логин обязателен");
            isValid = false;
        } else {
            if (login.length() < 4) {
                markFieldInvalid(loginField, "Минимум 4 символа");
                isValid = false;
            }
            if (login.matches(".*[а-яА-ЯёЁ].*")) {
                markFieldInvalid(loginField, "Только латинские буквы");
                isValid = false;
            }
            if (login.contains(" ")) {
                markFieldInvalid(loginField, "Пробелы недопустимы");
                isValid = false;
            }
            if (!login.matches("^[a-zA-Z].*")) {
                markFieldInvalid(loginField, "Должен начинаться с буквы");
                isValid = false;
            }
        }

        String password = passwordField.getText();
        if (password.isEmpty()) {
            markFieldInvalid(passwordField, "Пароль обязателен");
            isValid = false;
        } else if (password.length() < 6) {
            markFieldInvalid(passwordField, "Минимум 6 символов");
            isValid = false;
        } else if (!password.matches(".*[A-Z].*")) {
            markFieldInvalid(passwordField, "Добавьте заглавную букву");
            isValid = false;
        } else if (!password.matches(".*\\d.*")) {
            markFieldInvalid(passwordField, "Добавьте цифру");
            isValid = false;
        } else if (!password.matches(".*[!@#$%^&*].*")) {
            markFieldInvalid(passwordField, "Добавьте спецсимвол (!@#$%^&*)");
            isValid = false;
        }

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            markFieldInvalid(emailField, "Email обязателен");
            isValid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            markFieldInvalid(emailField, "Некорректный email");
            isValid = false;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            markFieldInvalid(nameField, "ФИО обязательно");
            isValid = false;
        } else {
            if (!name.matches("^[А-ЯЁ][а-яё]+(\\s[А-ЯЁ][а-яё]+){1,2}$")) {
                markFieldInvalid(nameField, "Формат: Иванов Иван Иванович");
                isValid = false;
            }
            if (name.matches(".*\\d.*")) {
                markFieldInvalid(nameField, "Цифры недопустимы");
                isValid = false;
            }
        }

        String date = dateField.getText().trim();
        if (date.isEmpty()) {
            markFieldInvalid(dateField, "Дата рождения обязательна");
            isValid = false;
        } else if (!date.matches("^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.(19|20)\\d{2}$")) {
            markFieldInvalid(dateField, "Формат: ДД.ММ.ГГГГ");
            isValid = false;
        } else {
            try {
                LocalDate birthDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                if (birthDate.isAfter(LocalDate.now().minusYears(14))) {
                    markFieldInvalid(dateField, "Минимальный возраст - 14 лет");
                    isValid = false;
                }
                if (birthDate.isBefore(LocalDate.now().minusYears(100))) {
                    markFieldInvalid(dateField, "Проверьте дату рождения");
                    isValid = false;
                }
            } catch (DateTimeParseException e) {
                markFieldInvalid(dateField, "Некорректная дата");
                isValid = false;
            }
        }

        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            markFieldInvalid(phoneField, "Телефон обязателен");
            isValid = false;
        } else if (!phone.matches("^\\+375(17|25|29|33|44)\\d{7}$")) {
            markFieldInvalid(phoneField, "Формат: +375XXXXXXXXX");
            isValid = false;
        }

        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            markFieldInvalid(addressField, "Адрес обязателен");
            isValid = false;
        } else if (address.length() < 10) {
            markFieldInvalid(addressField, "Слишком короткий адрес");
            isValid = false;
        }

        return isValid;
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
