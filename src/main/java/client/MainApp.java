package client;

import client.controllers.EditBankController;
import client.controllers.LoanPaymentsController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import client.viewFactories.ViewFactory;
import server.Entities.Bank;
import server.Entities.Loan;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
@Setter
public class MainApp extends Application {
    private Stage primaryStage;
    private BankClient client;
    private String currentUsername;
    private Scene currentRegisterScene;
    private Long currentUserId;
    private Long currentUserRoleId;
    private ViewFactory viewFactory;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.client = new BankClient("localhost", 5555);
        this.viewFactory = new ViewFactory(this, primaryStage);

        showMainView();
    }

    public void clearCurrentUser() {
        this.currentUserId = null;
        this.currentUserRoleId = null;
    }

    private void showView(ViewFactory.ViewType viewType) {
        viewFactory.showView(viewType);
    }

    public void showMainView() {
        showView(ViewFactory.ViewType.MAIN);
    }

    public void showRegisterView() {
        showView(ViewFactory.ViewType.REGISTER);
        this.currentRegisterScene = primaryStage.getScene();
    }

    public void showAccountView() {
        showView(ViewFactory.ViewType.CLIENT_ACCOUNT);
    }

    public void showAdminAccountView() {
        showView(ViewFactory.ViewType.ADMIN_ACCOUNT);
    }

    public void showBanksView() {
        showView(ViewFactory.ViewType.BANKS);
    }

    public void showLoansView() {
        showView(ViewFactory.ViewType.LOANS);
    }

    public void showLoanTypesView() {
        showView(ViewFactory.ViewType.LOAN_TYPES);
    }

    public void showDeleteByID() {
        showView(ViewFactory.ViewType.DELETE_BY_ID);
    }

    public void showEditAccountView() {
        showView(ViewFactory.ViewType.EDIT_ACCOUNT);
    }

    public void showAllClientsLoansView() {
        showView(ViewFactory.ViewType.ALL_LOANS);
    }

    public void showPaymentsView(Loan selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/loan-payments-view.fxml"));
            Parent root = loader.load();

            LoanPaymentsController controller = loader.getController();
            controller.setLoan(selected);
            controller.setMainApp(this);

            Stage stage = new Stage();
            stage.setTitle("Платежи по кредиту #" + selected.getLoanId());

            // Устанавливаем фиксированный размер окна
            stage.setMinWidth(1440);
            stage.setMinHeight(810);
            stage.setMaxWidth(1440);
            stage.setMaxHeight(810);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Центрируем окно на экране
            stage.centerOnScreen();

            // Модальное окно (блокирует родительское)
            stage.initModality(Modality.APPLICATION_MODAL);

            // Запрещаем изменение размера
            stage.setResizable(false);

            stage.show();

        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Ошибка при открытии окна платежей", e);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть окно платежей");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void editBank(Bank bank) {
        try {
            // Загрузка FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditBank.fxml"));
            Parent root = loader.load();

            // Настройка контроллера
            EditBankController controller = loader.getController();
            controller.setBank(bank);
            controller.setMainApp(this);

            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Редактирование банка #" + bank.getBankId());

        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Ошибка при открытии окна редактирования банка", e);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть окно редактирования");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


