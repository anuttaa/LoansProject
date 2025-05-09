package client.controllers;

import client.MainApp;
import server.DTO.BankDTO;
import com.google.gson.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;


public class BanksViewController {
    @FXML private TableView<BankDTO> banksTable;
    @FXML private TableColumn<BankDTO, Long> idColumn;
    @FXML private TableColumn<BankDTO, String> nameColumn;
    @FXML private TableColumn<BankDTO, String> addressColumn;
    @FXML private TableColumn<BankDTO, String> phoneColumn;
    @FXML private TableColumn<BankDTO, String> emailColumn;

    private MainApp mainApp;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("bankId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadBanks();
    }

    private void loadBanks() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "GET_BANKS");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response == null) {
                showError("Ошибка", "Нет ответа от сервера");
                return;
            }

            if (!response.has("banks")) {
                showError("Ошибка", "Некорректный формат ответа от сервера");
                return;
            }

            JsonArray banksArray = response.getAsJsonArray("banks");
            ObservableList<BankDTO> banks = FXCollections.observableArrayList();

            for (JsonElement element : banksArray) {
                JsonObject bankJson = element.getAsJsonObject();
                BankDTO bank = new BankDTO();

                bank.setBankId(bankJson.has("id") ? bankJson.get("id").getAsLong() : 0);
                bank.setBankName(bankJson.has("name") ? bankJson.get("name").getAsString() : "");
                bank.setAddress(bankJson.has("address") ? bankJson.get("address").getAsString() : "");
                bank.setPhone(bankJson.has("phone") ? bankJson.get("phone").getAsString() : "");
                bank.setEmail(bankJson.has("email") ? bankJson.get("email").getAsString() : "");

                banks.add(bank);
            }

            banksTable.setItems(banks);

        } catch (Exception e) {
            showError("Ошибка загрузки", "Не удалось загрузить список банков: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showAccountView();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}