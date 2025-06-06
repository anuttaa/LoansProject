package client.controllers;

import client.MainApp;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.Setter;
import server.Entities.User;

import java.text.SimpleDateFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
@Setter
public class EditUserController {
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
    private JsonObject userData;

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

    public void setUserData(JsonObject userData) {
        this.userData = userData;
        loadUserData();
    }

    public void loadUserData() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getCurrentUser");
            request.addProperty("userId", userData.get("userId").getAsLong());
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
            data.addProperty("userId", userData.get("userId").getAsLong());
            data.addProperty("email", emailField.getText().trim());
            data.addProperty("phone", phoneField.getText().trim());
            data.addProperty("fullName", nameField.getText().trim());
            if (!dateField.getText().trim().isEmpty()) {
                SimpleDateFormat fromUI = new SimpleDateFormat("dd.MM.yyyy");
                SimpleDateFormat toServer = new SimpleDateFormat("yyyy-MM-dd");
                Date date = fromUI.parse(dateField.getText().trim());
                data.addProperty("birthDate", toServer.format(date));
            }
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

        String login = usernameField.getText().trim();
        if (login.isEmpty()) {
            markFieldInvalid(usernameField, "Логин обязателен");
            isValid = false;
        } else {
            if (login.length() < 4) {
                markFieldInvalid(usernameField, "Минимум 4 символа");
                isValid = false;
            }
            if (login.matches(".*[а-яА-ЯёЁ].*")) {
                markFieldInvalid(usernameField, "Только латинские буквы");
                isValid = false;
            }
            if (login.contains(" ")) {
                markFieldInvalid(usernameField, "Пробелы недопустимы");
                isValid = false;
            }
            if (!login.matches("^[a-zA-Z].*")) {
                markFieldInvalid(usernameField, "Должен начинаться с буквы");
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
        mainApp.showDeleteByID();
    }
}
