package client.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.LocalDateAdapter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import com.google.gson.JsonObject;
import javafx.scene.control.PasswordField;
import client.MainApp;
import server.DTO.UserDTO;

import java.time.LocalDate;


public class MainController {
    @FXML public Button register;
    @FXML public Button exit;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleExit() {
        try {
            if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                showError("Пожалуйста, введите логин и пароль");
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("command", "login");

            JsonObject authData = new JsonObject();
            authData.addProperty("username", loginField.getText().trim());
            authData.addProperty("password", passwordField.getText().trim());
            request.add("data", authData);

            System.out.println("Отправка запроса на вход: " + request);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());
            System.out.println("Получен ответ: " + response);

            if (response == null) {
                showError("Нет ответа от сервера");
                return;
            }

            if (response.has("error")) {
                showError(response.get("error").getAsString());
                return;
            }

            if (response.has("status") && "success".equals(response.get("status").getAsString())) {
                String username = loginField.getText().trim();
                UserDTO userDTO = gson.fromJson(response.get("user"), UserDTO.class);
                mainApp.setCurrentUserId(userDTO.getUserId());
                mainApp.setCurrentUserRoleId(userDTO.getRoleId());

                if(userDTO.getRoleId()==1){
                    mainApp.showAdminAccountView();
                    System.out.println("user role");
                }
                else if(userDTO.getRoleId()==2) {
                    mainApp.showAccountView();
                    System.out.println("admin role");
                }
                else {
                    System.out.println("Invalid role");
                    mainApp.clearCurrentUser();
                }
            } else {
                showError("Ошибка аутентификации");
            }

        } catch (Exception e) {
            showError("Ошибка при входе: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() throws Exception {
        mainApp.showRegisterView();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

