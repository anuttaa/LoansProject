package client.controllers;

import client.MainApp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import server.Entities.Bank;
import server.Entities.LoanType;

@Setter
@Getter
public class LoanTypesViewController {
    @FXML private ComboBox<String> bankCombo;
    @FXML private TextField typeSearchField;
    @FXML private TextField minRateField;
    @FXML private TextField maxRateField;
    @FXML private TableView<JsonObject> loanTypesTable;
    @FXML private TableColumn<JsonObject, String> typeIdColumn;
    @FXML private TableColumn<JsonObject, String> typeNameColumn;
    @FXML private TableColumn<JsonObject, String> bankNameColumn;
    @FXML private TableColumn<JsonObject, String> rateColumn;
    @FXML private TableColumn<JsonObject, Void> actionsColumn;

    private MainApp mainApp;
    private ObservableList<JsonObject> loanTypes = FXCollections.observableArrayList();
    private FilteredList<JsonObject> filteredLoanTypes = new FilteredList<>(loanTypes);

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        System.out.println("Setting mainApp: " + mainApp);
        loadBanks();
        loadLoanTypes();
    }

    @FXML
    public void initialize() {
        rateColumn.setComparator(RATE_COMPARATOR);
        setupTableColumns();
        setupFiltering();
    }

    public static final Comparator<String> RATE_COMPARATOR = (s1, s2) -> {
        try {
            double rate1 = Double.parseDouble(s1);
            double rate2 = Double.parseDouble(s2);
            return Double.compare(rate2, rate1);
        } catch (NumberFormatException e) {
            return s2.compareTo(s1);
        }
    };

    private void setupTableColumns() {
        // Настройка столбцов таблицы
        typeIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("loan_type_id").getAsString()));

        typeNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("loan_type_name").getAsString()));

        bankNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAsJsonObject("bank").get("bank_name").getAsString()));

        rateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f", data.getValue().get("interest_rate").getAsDouble())));

        // Настройка столбца с действиями
        actionsColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<JsonObject, Void> call(TableColumn<JsonObject, Void> param) {
                return new TableCell<>() {
                    private final Button selectButton = new Button("Выбрать");

                    {
                        selectButton.setOnAction(event -> {
                            JsonObject loanType = getTableView().getItems().get(getIndex());
                            handleTakeLoanForType(loanType);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(selectButton);
                        }
                    }
                };
            }
        });

        // Сортировка по процентной ставке по умолчанию (по убыванию)
        rateColumn.setSortType(TableColumn.SortType.DESCENDING);
        loanTypesTable.getSortOrder().add(rateColumn);
    }

    private void setupFiltering() {
        // Настройка фильтрации данных в таблице
        typeSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        minRateField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        maxRateField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        bankCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Установка фильтрованного и отсортированного списка в таблицу
        SortedList<JsonObject> sortedData = new SortedList<>(filteredLoanTypes);
        sortedData.comparatorProperty().bind(loanTypesTable.comparatorProperty());
        loanTypesTable.setItems(sortedData);
    }

    private void applyFilters() {
        filteredLoanTypes.setPredicate(loanType -> {
            // Фильтр по названию типа кредита
            if (!typeSearchField.getText().isEmpty()) {
                String searchText = typeSearchField.getText().toLowerCase();
                String loanTypeName = loanType.get("loan_type_name").getAsString().toLowerCase();
                if (!loanTypeName.contains(searchText)) {
                    return false;
                }
            }

            // Фильтр по банку
            if (bankCombo.getSelectionModel().getSelectedItem() != null) {
                String selectedBank = bankCombo.getSelectionModel().getSelectedItem();
                String loanBank = loanType.getAsJsonObject("bank").get("bank_name").getAsString();
                if (!loanBank.equals(selectedBank)) {
                    return false;
                }
            }

            // Фильтр по минимальной ставке
            if (!minRateField.getText().isEmpty()) {
                try {
                    double minRate = Double.parseDouble(minRateField.getText());
                    double loanRate = loanType.get("interest_rate").getAsDouble();
                    if (loanRate < minRate) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            // Фильтр по максимальной ставке
            if (!maxRateField.getText().isEmpty()) {
                try {
                    double maxRate = Double.parseDouble(maxRateField.getText());
                    double loanRate = loanType.get("interest_rate").getAsDouble();
                    if (loanRate > maxRate) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            return true;
        });
    }

    private void loadBanks() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getBanks");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                bankCombo.getItems().clear();
                bankCombo.getItems().add("Все банки"); // Добавляем опцию "Все банки"
                response.getAsJsonArray("banks").forEach(bank -> {
                    bankCombo.getItems().add(bank.getAsJsonObject().get("bankName").getAsString());
                });
            } else {
                showError("Не удалось загрузить список банков");
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке банков: " + e.getMessage());
        }
    }

    private void loadLoanTypes() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getLoanTypes");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                loanTypes.clear();

                JsonArray loansArray = response.getAsJsonArray("loanTypes");
                if (loansArray != null) {
                    for (JsonElement element : loansArray) {
                        if (element == null || !element.isJsonObject()) continue;

                        JsonObject loanJson = element.getAsJsonObject();

                        // Проверяем наличие всех обязательных полей
                        if (!loanJson.has("loanTypeId") || !loanJson.has("loanTypeName") ||
                                !loanJson.has("interestRate") || !loanJson.has("bankId") ||
                                !loanJson.has("bankName")) {
                            System.err.println("Пропущен неполный объект кредита: " + loanJson);
                            continue;
                        }

                        try {
                            JsonObject tableItem = new JsonObject();
                            tableItem.addProperty("loan_type_id", loanJson.get("loanTypeId").getAsString());
                            tableItem.addProperty("loan_type_name", loanJson.get("loanTypeName").getAsString());
                            tableItem.addProperty("interest_rate", loanJson.get("interestRate").getAsDouble());

                            JsonObject bankJson = new JsonObject();
                            bankJson.addProperty("bank_id", loanJson.get("bankId").getAsString());
                            bankJson.addProperty("bank_name", loanJson.get("bankName").getAsString());
                            tableItem.add("bank", bankJson);

                            loanTypes.add(tableItem);
                        } catch (Exception e) {
                            System.err.println("Ошибка обработки объекта кредита: " + loanJson);
                            e.printStackTrace();
                        }
                    }
                }

                if (loanTypesTable != null) {
                    loanTypesTable.sort();
                }
            } else {
                String errorMsg = response != null && response.has("message")
                        ? response.get("message").getAsString()
                        : "Не удалось загрузить типы кредитов";
                showError(errorMsg);
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке кредитов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleApplyFilters() {
        applyFilters(); // Применяем фильтры
    }

    @FXML
    private void handleResetFilters() {
        bankCombo.getSelectionModel().clearSelection();
        typeSearchField.clear();
        minRateField.clear();
        maxRateField.clear();
        applyFilters(); // Сбрасываем фильтры
    }

    @FXML
    private void handleTakeLoan() {
        JsonObject selectedLoan = loanTypesTable.getSelectionModel().getSelectedItem();
        if (selectedLoan != null) {
            handleTakeLoanForType(selectedLoan);
        } else {
            showError("Выберите тип кредита из таблицы");
        }
    }

    private void handleTakeLoanForType(JsonObject loanType) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Оформление кредита");
        dialog.setHeaderText("Введите данные для кредита: " + loanType.get("loan_type_name").getAsString());

        // Добавляем кнопки OK и Cancel
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Создаем поля для ввода
        TextField amountField = new TextField();
        amountField.setPromptText("Сумма кредита");
        TextField termField = new TextField();
        termField.setPromptText("Срок (месяцы)");

        // Валидация числовых полей
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
        });

        termField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                termField.setText(oldVal);
            }
        });

        // Создаем и настраиваем GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Сумма:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Срок (мес):"), 0, 1);
        grid.add(termField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        // Преобразуем результат диалога
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (amountField.getText().isEmpty() || termField.getText().isEmpty()) {
                    showError("Заполните все поля");
                    return null;
                }

                JsonObject loanData = new JsonObject();
                loanData.addProperty("loanTypeId", loanType.get("loan_type_id").getAsString());
                loanData.addProperty("amount", amountField.getText());
                loanData.addProperty("termMonths", termField.getText());
                return loanData;
            }
            return null;
        });

        // Показываем диалог и обрабатываем результат
        Optional<JsonObject> result = dialog.showAndWait();
        result.ifPresent(loanData -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "takeLoan");
                request.add("loanData", loanData);

                JsonObject response = mainApp.getClient().sendRequest(request.toString());

                if (response != null && response.get("status").getAsString().equals("success")) {
                    showSuccess("Кредит успешно оформлен");
                    loadLoanTypes(); // Обновляем список кредитов
                } else {
                    String error = response != null ? response.get("message").getAsString() : "Нет ответа от сервера";
                    showError("Не удалось оформить кредит: " + error);
                }
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });
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

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успешно");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
