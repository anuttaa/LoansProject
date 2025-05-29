package client.controllers;

import client.MainApp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import config.LocalDateAdapter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import server.Entities.Bank;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class EditBankController {
    @FXML private TextField bankNameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private Bank currentBank;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setBank(Bank bank) {
        this.currentBank = bank;
        // Заполняем поля данными банка
        bankNameField.setText(bank.getBankName());
        addressField.setText(bank.getAddress());
        phoneField.setText(bank.getPhone());
        emailField.setText(bank.getEmail());
    }

    @FXML
    private void handleSave() {
        if (!validateFields()) {
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "updateBank");

            JsonObject bankData = new JsonObject();
            bankData.addProperty("bankId", currentBank.getBankId());
            bankData.addProperty("bankName", bankNameField.getText().trim());
            bankData.addProperty("address", addressField.getText().trim());
            bankData.addProperty("phone", phoneField.getText().trim());
            bankData.addProperty("email", emailField.getText().trim());

            request.add("data", bankData);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                showSuccess("Данные банка успешно обновлены");
                // Обновляем данные в текущем объекте
                currentBank.setBankName(bankNameField.getText().trim());
                currentBank.setAddress(addressField.getText().trim());
                currentBank.setPhone(phoneField.getText().trim());
                currentBank.setEmail(emailField.getText().trim());

                mainApp.showBanksView();
            } else {
                String error = response != null ? response.get("message").getAsString() : "Нет ответа от сервера";
                showError("Ошибка: " + error);
            }
        } catch (Exception e) {
            showError("Ошибка соединения: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        boolean isValid = true;
        resetFieldStyles();

        if (bankNameField.getText().trim().isEmpty()) {
            markFieldInvalid(bankNameField, "Название банка обязательно");
            isValid = false;
        }

        if (addressField.getText().trim().isEmpty()) {
            markFieldInvalid(addressField, "Адрес обязателен");
            isValid = false;
        }

        if (phoneField.getText().trim().isEmpty()) {
            markFieldInvalid(phoneField, "Телефон обязателен");
            isValid = false;
        } else if (!isValidPhone(phoneField.getText().trim())) {
            markFieldInvalid(phoneField, "Формат: +375XXXXXXXXX или 80XXXXXXXXX");
            isValid = false;
        }

        if (emailField.getText().trim().isEmpty()) {
            markFieldInvalid(emailField, "Email обязателен");
            isValid = false;
        } else if (!isValidEmail(emailField.getText().trim())) {
            markFieldInvalid(emailField, "Некорректный email");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("^(\\+375|80)\\d{9}$");
    }

    private void markFieldInvalid(Control field, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: #ff4444;");
        Tooltip.install(field, tooltip);
    }

    private void resetFieldStyles() {
        List<Control> fields = Arrays.asList(
                bankNameField, addressField, phoneField, emailField
        );
        fields.forEach(field -> {
            field.setStyle("");
            Tooltip.uninstall(field, field.getTooltip());
        });
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    private void handleBack() {
        mainApp.showBanksView();
    }
}