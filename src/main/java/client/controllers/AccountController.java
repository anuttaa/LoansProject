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

    @FXML
    private void handleMyLoans() {
       mainApp.showLoansView();
    }

    @FXML
    private void handleShowLoans() { mainApp.showLoanTypesView();}

    @FXML
    private void handleAccount() { mainApp.showEditAccountView(); }

    @FXML
    public void handleShowStatistics(){
        mainApp.showLoanStatistics();
    }
}
