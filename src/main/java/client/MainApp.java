package client;

import client.controllers.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import server.Entities.User;

import java.io.IOException;


public class MainApp extends Application {
    private Stage primaryStage;
    private BankClient client;
    private String currentUsername;
    private Scene currentRegisterScene;
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.client = new BankClient("localhost", 5555);

        showMainView();
    }

    public void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setTitle("Вход в систему");
            primaryStage.setScene(new Scene(root, 600, 400));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка загрузки", "Не удалось загрузить главное окно: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showRegisterView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
        Parent root = loader.load();

        RegisterController controller = loader.getController();
        controller.setMainApp(this);

        primaryStage.setTitle("Регистрация");
        primaryStage.setScene(new Scene(root, 600, 400));
        this.currentRegisterScene = primaryStage.getScene();
    }

    public void showAccountView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/account.fxml"));
            Parent root = loader.load();

            AccountController controller = loader.getController();
            controller.setMainApp(this);
            primaryStage.setScene(new Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось загрузить окно аккаунта");
        }
    }

    public void showBanksView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/banksView.fxml"));
            Parent root = loader.load();

            BanksViewController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setTitle("Список банков");
            primaryStage.setScene(new Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось загрузить окно банков");
        }
    }

    public void showDeleteByID(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/deleteByID.fxml"));
            Parent root = loader.load();

            DeleteUserController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setTitle("Удаление пользователя");
            primaryStage.setScene(new Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось загрузить окно удаления");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    public BankClient getClient() {
        return client;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

}


