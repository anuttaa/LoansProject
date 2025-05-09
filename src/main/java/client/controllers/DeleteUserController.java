package client.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import client.MainApp;
import com.google.gson.JsonObject;


public class DeleteUserController {
    @FXML private TextField userIdField;
    @FXML private TextArea statusLabel;
    @FXML private TextArea usersArea;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        System.out.println("Setting mainApp: " + mainApp);
        loadUsers();
    }

    private void loadUsers() {
        try {
            if (mainApp == null) {
                throw new IllegalStateException("MainApp не инициализирован.");
            }
            if (mainApp.getClient() == null) {
                throw new IllegalStateException("Client не инициализирован.");
            }
            JsonObject getAllRequest = new JsonObject();
            getAllRequest.addProperty("command", "findAllUsers");
            JsonObject allUsersResponse = mainApp.getClient().sendRequest(getAllRequest.toString());

            if (allUsersResponse != null && allUsersResponse.get("status").getAsString().equals("success")) {
                JsonArray users = allUsersResponse.getAsJsonArray("users");

                StringBuilder sb = new StringBuilder("Текущие пользователи:\n");
                for (JsonElement userElem : users) {
                    JsonObject user = userElem.getAsJsonObject();
                    sb.append("ID: ").append(user.get("userId").getAsLong()).append(", Логин: ")
                            .append(user.get("username").getAsString()).append("\n");
                }
                usersArea.setText(sb.toString());
            } else {
                usersArea.setText("Не удалось получить список пользователей.");
            }
        } catch (Exception e) {
            usersArea.setText("Ошибка при загрузке пользователей: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        try {
            long userId = Long.parseLong(userIdField.getText());

            JsonObject deleteRequest = new JsonObject();
            deleteRequest.addProperty("command", "deleteUser");

            JsonObject data = new JsonObject();
            data.addProperty("userId", userId);
            deleteRequest.add("data", data);

            JsonObject deleteResponse = mainApp.getClient().sendRequest(deleteRequest.toString());

            if (deleteResponse != null && deleteResponse.get("status").getAsString().equals("success")) {
                statusLabel.setText("Пользователь успешно удален");
                statusLabel.setStyle("-fx-text-fill: green;");
                loadUsers(); // Обновляем список после удаления
            } else {
                String error = deleteResponse != null ? deleteResponse.get("message").getAsString() : "Нет ответа от сервера";
                statusLabel.setText("Ошибка: " + error);
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Ошибка: ID должен быть числом");
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showAccountView();
    }
}

