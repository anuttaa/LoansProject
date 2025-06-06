package client.controllers;

import client.MainApp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoanTypesViewController {
    @FXML private ComboBox<String> bankCombo;
    @FXML private TextField typeSearchField;
    @FXML private TextField minRateField;
    @FXML private TextField maxRateField;
    @FXML private TableView<JsonObject> loanTypesTable;
    @FXML private TableColumn<JsonObject, Number> typeIdColumn;
    @FXML private TableColumn<JsonObject, String> typeNameColumn;
    @FXML private TableColumn<JsonObject, String> bankNameColumn;
    @FXML private TableColumn<JsonObject, Number> rateColumn;
    @FXML private TableColumn<JsonObject, Void> actionsColumn;
    @FXML private TableColumn<JsonObject, Number> effectiveRateColumn;

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
        setupTableColumns();
        setupFiltering();
    }

    private void setupTableColumns() {
        typeIdColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().get("loan_type_id").getAsInt()));

        typeNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("loan_type_name").getAsString()));

        bankNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAsJsonObject("bank").get("bank_name").getAsString()));

        rateColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().get("interest_rate").getAsDouble()));

        effectiveRateColumn.setCellValueFactory(data -> {
            JsonObject loanType = data.getValue();
            double baseRate = loanType.get("interest_rate").getAsDouble();

            BigDecimal amount = new BigDecimal("10000");
            int termMonths = 12;
            BigDecimal oneTimeCommission = new BigDecimal("50");
            double monthlyCommission = 0.001;
            double insurance = 0.005;

            double effectiveRate = calculateEffectiveRateRB(
                    baseRate,
                    amount,
                    termMonths,
                    oneTimeCommission,
                    monthlyCommission,
                    insurance
            );

            return new SimpleDoubleProperty(effectiveRate);
        });

        effectiveRateColumn.setCellFactory(column -> new TableCell<JsonObject, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f%%", item.doubleValue()));
                }
            }
        });

        typeIdColumn.setComparator(Comparator.comparingInt(Number::intValue));
        typeNameColumn.setComparator(String::compareToIgnoreCase);
        bankNameColumn.setComparator(String::compareToIgnoreCase);
        rateColumn.setComparator(Comparator.comparingDouble(Number::doubleValue));
        effectiveRateColumn.setComparator(Comparator.comparingDouble(Number::doubleValue));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final HBox buttonsContainer = new HBox(5);
            private final Button selectButton = new Button("Выбрать");
            private final Button editButton = new Button("Изменить");

            {
                selectButton.setOnAction(event -> {
                    JsonObject loanType = getTableView().getItems().get(getIndex());
                    handleTakeLoanForType(loanType);
                });

                editButton.setOnAction(event -> {
                    JsonObject loanType = getTableView().getItems().get(getIndex());
                    handleEditLoanType(loanType);
                });

                buttonsContainer.getChildren().addAll(selectButton, editButton);
                buttonsContainer.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonsContainer);
            }
        });
    }

    private void handleEditLoanType(JsonObject loanType) {
        if(mainApp.getCurrentUserRoleId()==1) {
            Dialog<JsonObject> dialog = new Dialog<>();
            dialog.setTitle("Редактирование типа кредита");
            dialog.setHeaderText("Измените данные типа кредита");

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextField nameField = new TextField(loanType.get("loan_type_name").getAsString());
            TextField rateField = new TextField(String.valueOf(loanType.get("interest_rate").getAsDouble()));

            ComboBox<String> bankCombo = new ComboBox<>();
            bankCombo.setItems(FXCollections.observableArrayList(getBankNames()));
            bankCombo.getSelectionModel().select(loanType.getAsJsonObject("bank").get("bank_name").getAsString());

            rateField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d*)?")) {
                    rateField.setText(oldVal);
                }
            });

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            grid.add(new Label("Название:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Процентная ставка:"), 0, 1);
            grid.add(rateField, 1, 1);
            grid.add(new Label("Банк:"), 0, 2);
            grid.add(bankCombo, 1, 2);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    if (nameField.getText().isEmpty() || rateField.getText().isEmpty() || bankCombo.getValue() == null) {
                        showError("Заполните все поля");
                        return null;
                    }

                    try {
                        JsonObject updatedData = new JsonObject();
                        updatedData.addProperty("loanTypeId", loanType.get("loan_type_id").getAsString());
                        updatedData.addProperty("loanTypeName", nameField.getText());
                        updatedData.addProperty("interestRate", Double.parseDouble(rateField.getText()));
                        updatedData.addProperty("bankName", bankCombo.getValue());

                        return updatedData;
                    } catch (NumberFormatException e) {
                        showError("Некорректное значение процентной ставки");
                        return null;
                    }
                }
                return null;
            });

            Optional<JsonObject> result = dialog.showAndWait();
            result.ifPresent(updatedData -> {
                try {
                    JsonObject request = new JsonObject();
                    request.addProperty("command", "updateLoanType");
                    request.add("loanTypeData", updatedData);

                    JsonObject response = mainApp.getClient().sendRequest(request.toString());

                    if (response != null && response.get("status").getAsString().equals("success")) {
                        showSuccess("Тип кредита успешно обновлен");
                        loadLoanTypes();
                    } else {
                        String error = response != null ? response.get("message").getAsString() : "Нет ответа от сервера";
                        showError("Не удалось обновить тип кредита: " + error);
                    }
                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                }
            });
        }else {
            showError("Вам недоступна эта функция!");
        }
    }

    private List<String> getBankNames() {
        List<String> bankNames = new ArrayList<>();
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getBanks");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                response.getAsJsonArray("banks").forEach(bank -> {
                    bankNames.add(bank.getAsJsonObject().get("bankName").getAsString());
                });
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке банков: " + e.getMessage());
        }
        return bankNames;
    }

    private void setupFiltering() {
        typeSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        minRateField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        maxRateField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        bankCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        SortedList<JsonObject> sortedData = new SortedList<>(filteredLoanTypes);
        sortedData.comparatorProperty().bind(loanTypesTable.comparatorProperty());
        loanTypesTable.setItems(sortedData);
    }

    private void applyFilters() {
        filteredLoanTypes.setPredicate(loanType -> {
            if (!typeSearchField.getText().isEmpty()) {
                String searchText = typeSearchField.getText().toLowerCase();
                String loanTypeName = loanType.get("loan_type_name").getAsString().toLowerCase();
                if (!loanTypeName.contains(searchText)) {
                    return false;
                }
            }

            if (bankCombo.getSelectionModel().getSelectedItem() != null) {
                String selectedBank = bankCombo.getSelectionModel().getSelectedItem();
                String loanBank = loanType.getAsJsonObject("bank").get("bank_name").getAsString();
                if (!loanBank.equals(selectedBank)) {
                    return false;
                }
            }

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
                    loanTypesTable.getSortOrder().clear();
                    loanTypesTable.getSortOrder().add(rateColumn);
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
        applyFilters();
    }

    @FXML
    private void handleResetFilters() {
        bankCombo.getSelectionModel().clearSelection();
        typeSearchField.clear();
        minRateField.clear();
        maxRateField.clear();
        applyFilters();
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

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField amountField = new TextField();
        amountField.setPromptText("Сумма кредита");
        TextField termField = new TextField();
        termField.setPromptText("Срок (месяцы)");

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

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Сумма:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Срок (мес):"), 0, 1);
        grid.add(termField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (amountField.getText().isEmpty() || termField.getText().isEmpty()) {
                    showError("Заполните все поля");
                    return null;
                }

                JsonObject loanData = new JsonObject();
                loanData.addProperty("userId", mainApp.getCurrentUserId());
                loanData.addProperty("loanTypeId", loanType.get("loan_type_id").getAsString());
                loanData.addProperty("amount", amountField.getText());
                loanData.addProperty("termMonths", termField.getText());
                return loanData;
            }
            return null;
        });

        Optional<JsonObject> result = dialog.showAndWait();
        result.ifPresent(loanData -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "takeLoan");
                request.add("loanData", loanData);

                JsonObject response = mainApp.getClient().sendRequest(request.toString());

                if (response != null && response.get("status").getAsString().equals("success")) {
                    showSuccess("Кредит успешно оформлен");
                    loadLoanTypes();
                } else {
                    String error = response != null ? response.get("message").getAsString() : "Нет ответа от сервера";
                    showError("Не удалось оформить кредит: " + error);
                }
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });
    }

    private double calculateEffectiveRateRB(
            double baseRate,
            BigDecimal amount,
            int termMonths,
            BigDecimal oneTimeCommission,
            double monthlyCommissionPercent,
            double insurancePercent) {

        BigDecimal receivedAmount = amount.subtract(oneTimeCommission)
                .subtract(amount.multiply(BigDecimal.valueOf(insurancePercent)));

        double monthlyInterestRate = baseRate / 100 / 12;
        BigDecimal monthlyPayment = calculateAnnuityPayment(amount, monthlyInterestRate, termMonths);
        BigDecimal monthlyCommission = amount.multiply(BigDecimal.valueOf(monthlyCommissionPercent / 100));
        BigDecimal totalMonthlyPayment = monthlyPayment.add(monthlyCommission);

        double low = 0.0;
        double high = 100.0;
        double eps = 0.001;

        for (int i = 0; i < 100; i++) {
            double mid = (low + high) / 2;
            double monthlyEffectiveRate = mid / 100 / 12;

            BigDecimal npv = BigDecimal.ZERO;
            for (int month = 1; month <= termMonths; month++) {
                BigDecimal discountedPayment = totalMonthlyPayment.divide(
                        BigDecimal.valueOf(Math.pow(1 + monthlyEffectiveRate, month)),
                        10, RoundingMode.HALF_UP
                );
                npv = npv.add(discountedPayment);
            }

            if (npv.compareTo(receivedAmount) > 0) {
                low = mid;
            } else {
                high = mid;
            }

            if (high - low < eps) break;
        }

        return (low + high) / 2;
    }

    private BigDecimal calculateAnnuityPayment(BigDecimal amount, double monthlyRate, int termMonths) {
        if (monthlyRate == 0) {
            return amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }

        double annuityCoeff = (monthlyRate * Math.pow(1 + monthlyRate, termMonths)) /
                (Math.pow(1 + monthlyRate, termMonths) - 1);

        return amount.multiply(BigDecimal.valueOf(annuityCoeff))
                .setScale(2, RoundingMode.HALF_UP);
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
