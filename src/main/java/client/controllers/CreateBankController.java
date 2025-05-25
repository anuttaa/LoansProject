package client.controllers;
import client.MainApp;
import com.google.gson.*;
import config.LocalDateAdapter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;
import server.Entities.Bank;
import server.Entities.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Setter
@Getter
public class CreateBankController {
    @FXML private TextField bankNameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label errorLabel;

    @FXML private TableView<Bank> banksTable;
    @FXML private TableColumn<Bank, Long> idColumn;
    @FXML private TableColumn<Bank, String> nameColumn;
    @FXML private TableColumn<Bank, String> addressColumn;
    @FXML private TableColumn<Bank, String> phoneColumn;
    @FXML private TableColumn<Bank, String> emailColumn;
    @FXML private TableColumn<Bank, Void> actionsColumn;

    private ObservableList<Bank> banksData = FXCollections.observableArrayList();
    private MainApp mainApp;
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    @FXML
    public void initialize() {
        // Настройка столбцов таблицы
        idColumn.setCellValueFactory(new PropertyValueFactory<>("bankId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Настройка столбца с действиями
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button addLoanButton = new Button("Добавить кредит");
            private final Button viewLoansButton = new Button("Просмотр кредитов");
            private final HBox buttons = new HBox(5, addLoanButton, viewLoansButton);

            {
                addLoanButton.setOnAction(event -> {
                    Bank bank = getTableView().getItems().get(getIndex());
                    showAddLoanDialog(bank);
                });

                viewLoansButton.setOnAction(event -> {
                    Bank bank = getTableView().getItems().get(getIndex());
                    showBankLoans(bank);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });

        banksTable.setItems(banksData);
        bankNameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadBanks();
    }

    private void loadBanks() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getBanks");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                banksData.clear();
                JsonArray banksArray = response.getAsJsonArray("banks");
                for (JsonElement bankElement : banksArray) {
                    Bank bank = gson.fromJson(bankElement, Bank.class);
                    banksData.add(bank);
                }
            } else {
                String errorMsg = response != null && response.has("message")
                        ? response.get("message").getAsString()
                        : "Не удалось загрузить список банков";
                showAlert(Alert.AlertType.ERROR, "Ошибка", errorMsg);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Ошибка при загрузке списка банков: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateBank() {
        if (bankNameField.getText().trim().isEmpty()) {
            errorLabel.setText("Название банка обязательно для заполнения");
            return;
        }

        try {
            JsonObject requestData = new JsonObject();
            requestData.addProperty("command", "addBank");

            JsonObject bankData = new JsonObject();
            bankData.addProperty("bankName", bankNameField.getText().trim());
            bankData.addProperty("address", addressField.getText().trim());
            bankData.addProperty("phone", phoneField.getText().trim());
            bankData.addProperty("email", emailField.getText().trim());

            requestData.add("data", bankData);

            JsonObject response = mainApp.getClient().sendRequest(requestData.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Банк успешно создан");
                clearFields();
                loadBanks(); // Обновляем таблицу
            } else {
                String errorMessage = response != null && response.has("message")
                        ? response.get("message").getAsString()
                        : "Неизвестная ошибка сервера";
                showAlert(Alert.AlertType.ERROR, "Ошибка", errorMessage);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Не удалось создать банк: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAddLoanDialog(Bank bank) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Добавление кредита");
        dialog.setHeaderText("Добавить кредит для банка: " + bank.getBankName());

        // Кнопки диалога
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Поля формы
        TextField loanNameField = new TextField();
        loanNameField.setPromptText("Название кредита");

        TextField interestRateField = new TextField();
        interestRateField.setPromptText("Процентная ставка (например, 5.5)");

        // Валидация числового поля
        interestRateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                interestRateField.setText(oldVal);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Название кредита:"), 0, 0);
        grid.add(loanNameField, 1, 0);
        grid.add(new Label("Процентная ставка:"), 0, 1);
        grid.add(interestRateField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Преобразование результата
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (loanNameField.getText().isEmpty() || interestRateField.getText().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Заполните все поля");
                    return null;
                }

                JsonObject loanData = new JsonObject();
                loanData.addProperty("bankId", bank.getBankId());
                loanData.addProperty("loanTypeName", loanNameField.getText());
                loanData.addProperty("interestRate", interestRateField.getText());
                return loanData;
            }
            return null;
        });

        Optional<JsonObject> result = dialog.showAndWait();
        result.ifPresent(loanData -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "createLoanType");
                request.add("data", loanData);

                JsonObject response = mainApp.getClient().sendRequest(request.toString());

                if (response != null && response.get("status").getAsString().equals("success")) {
                    showAlert(Alert.AlertType.INFORMATION, "Успех", "Кредит успешно добавлен");
                } else {
                    String error = response != null ? response.get("message").getAsString() : "Неизвестная ошибка";
                    showAlert(Alert.AlertType.ERROR, "Ошибка", error);
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", e.getMessage());
            }
        });
    }

    private void showBankLoans(Bank bank) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getLoanTypesByBank");
            request.addProperty("bankId", bank.getBankId());

            // Получаем строковый ответ от сервера
            JsonObject response = mainApp.getClient().sendRequest(request.toString());


            if (response.get("status").getAsString().equals("success")) {
                ObservableList<LoanType> loans = FXCollections.observableArrayList();
                JsonArray loansArray = response.getAsJsonArray("loanTypes");

                for (JsonElement loanElement : loansArray) {
                    loans.add(gson.fromJson(loanElement, LoanType.class));
                }

                // Создаем диалоговое окно
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("Кредиты банка");
                dialog.setHeaderText("Кредиты банка: " + bank.getBankName());

                TableView<LoanType> tableView = new TableView<>();
                tableView.setItems(loans);

                TableColumn<LoanType, String> nameCol = new TableColumn<>("Название");
                nameCol.setCellValueFactory(new PropertyValueFactory<>("loanTypeName"));

                TableColumn<LoanType, BigDecimal> rateCol = new TableColumn<>("Ставка");
                rateCol.setCellValueFactory(new PropertyValueFactory<>("interestRate"));

                tableView.getColumns().addAll(nameCol, rateCol);
                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                dialog.getDialogPane().setContent(tableView);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.showAndWait();
            } else {
                String errorMsg = response.has("message")
                        ? response.get("message").getAsString()
                        : "Неизвестная ошибка сервера";
                showAlert(Alert.AlertType.ERROR, "Ошибка", errorMsg);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Не удалось загрузить список кредитов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateFields() {
        errorLabel.setText(bankNameField.getText().trim().isEmpty()
                ? "Название банка обязательно для заполнения"
                : "");
    }

    private void clearFields() {
        bankNameField.clear();
        addressField.clear();
        phoneField.clear();
        emailField.clear();
        errorLabel.setText("");
    }

    @FXML
    private void handleBack() {
        if(mainApp.getCurrentUserRoleId()==1){
            mainApp.showAdminAccountView();
            System.out.println("admin role");
        }
        else if(mainApp.getCurrentUserRoleId()==2) {
            mainApp.showAccountView();
            System.out.println("user role");
        }
        else {
            System.out.println("Invalid role");
            mainApp.clearCurrentUser();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
