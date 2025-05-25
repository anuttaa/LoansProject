package client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import client.viewFactories.ViewFactory;

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


