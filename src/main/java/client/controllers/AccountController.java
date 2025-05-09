package client.controllers;

import javafx.fxml.FXML;
import client.MainApp;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AccountController {
    @FXML public Button backButton;
    @FXML public Button deleteByID;
    @FXML private Button banks;
    @FXML private Button loans;
    @FXML private Button payments;
    @FXML private Button effectiveRate;
    @FXML private Button account;
    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleBack() {
        try {
            mainApp.showMainView();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText("Не удалось вернуться на главный экран");
            alert.showAndWait();
        }
    }

    // Методы для открытия окон
    @FXML
    private void handleBanks() {
        mainApp.showBanksView();
    }
    
    @FXML
    private void handleDeleteByID() {
        mainApp.showDeleteByID();
    }

    @FXML
    private void handleLoans() {
        openWindow("/loans.fxml", "Мои кредиты");
    }

    @FXML
    private void handlePayments() {
        openWindow("/payments.fxml", "График платежей");
    }

    @FXML
    private void handleEffectiveRate() {
        openWindow("/rate_calculator.fxml", "Расчет ЭПС");
    }

    @FXML
    private void handleAccount() {
        openWindow("/edit_account.fxml", "Редактирование аккаунта");
    }

    // Общий метод для открытия окон
    private void openWindow(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
