package client.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import client.MainApp;
import com.google.gson.JsonObject;
import javafx.scene.layout.HBox;


public class DeleteUserController {
    @FXML private TableView<JsonObject> usersTable;
    @FXML private TableColumn<JsonObject, String> idColumn;
    @FXML private TableColumn<JsonObject, String> usernameColumn;
    @FXML private TableColumn<JsonObject, String> roleColumn;
    @FXML private TableColumn<JsonObject, String> emailColumn;
    @FXML private TableColumn<JsonObject, Void> actionsColumn;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private ObservableList<JsonObject> users = FXCollections.observableArrayList();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        initializeTable();
        loadUsers();
    }

    private void initializeTable() {
        // Настройка столбцов
        idColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("userId").getAsString()));
        usernameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("username").getAsString()));
        roleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(getRoleName(data.getValue().get("roleId").getAsInt())));
        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("email").getAsString()));

        // Настройка столбца с действиями
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");
            private final Button editButton = new Button("Редактировать");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                buttons.setAlignment(Pos.CENTER);

                deleteButton.getStyleClass().add("danger-button");
                editButton.getStyleClass().add("primary-button");
                buttons.getStyleClass().add("buttons-container");

                deleteButton.setOnAction(event -> {
                    JsonObject user = getTableView().getItems().get(getIndex());
                    handleDelete(user.get("userId").getAsLong());
                });

                editButton.setOnAction(event -> {
                    JsonObject user = getTableView().getItems().get(getIndex());
                    handleEdit(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private String getRoleName(int roleId) {
        switch (roleId) {
            case 1: return "Администратор";
            case 2: return "Пользователь";
            default: return "Неизвестно";
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    private void loadUsers() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "findAllUsers");
            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                users.clear();
                JsonArray usersArray = response.getAsJsonArray("users");

                for (JsonElement userElem : usersArray) {
                    JsonObject user = userElem.getAsJsonObject();
                    users.add(user);
                }

                usersTable.setItems(users);
                statusLabel.setText("Загружено пользователей: " + users.size());
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                String error = response != null ? response.get("message").getAsString() : "Неизвестная ошибка";
                statusLabel.setText("Ошибка загрузки: " + error);
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    private void handleDelete(long userId) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "deleteUser");

            JsonObject data = new JsonObject();
            data.addProperty("userId", userId);
            request.add("data", data);

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                statusLabel.setText("Пользователь успешно удален");
                statusLabel.setStyle("-fx-text-fill: green;");
                loadUsers(); // Обновляем список
            } else {
                String error = response != null ? response.get("message").getAsString() : "Неизвестная ошибка";
                statusLabel.setText("Ошибка удаления: " + error);
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void handleEdit(JsonObject user) {
        try {
            mainApp.showEditUserView(user);
        } catch (Exception e) {
            statusLabel.setText("Ошибка перехода к редактированию: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleBack() {
        if (mainApp.getCurrentUserRoleId() == 1) {
            mainApp.showAdminAccountView();
        } else if (mainApp.getCurrentUserRoleId() == 2) {
            mainApp.showAccountView();
        } else {
            mainApp.clearCurrentUser();
        }
    }
}

