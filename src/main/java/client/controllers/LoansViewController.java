package client.controllers;

import client.MainApp;
import com.google.gson.*;
import config.LocalDateAdapter;
import enums.PaymentType;
import exeption.PaymentValidationException;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DTO.PaymentScheduleDTO;
import server.Entities.Bank;
import server.Entities.Loan;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import server.Entities.LoanType;
import server.Entities.User;
import server.service.UserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class LoansViewController {
    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, String> typeColumn;
    @FXML private TableColumn<Loan, BigDecimal> amountColumn;
    @FXML private TableColumn<Loan, BigDecimal> rateColumn;
    @FXML private TableColumn<Loan, Integer> termColumn;
    @FXML private TableColumn<Loan, String> statusColumn;
    @FXML private TableColumn<Loan, Void> actionsColumn;

    @FXML private Label detailBank;
    @FXML private Label detailType;
    @FXML private Label detailAmount;
    @FXML private Label detailRate;
    @FXML private Label detailTerm;
    @FXML private Label detailDates;
    @FXML private Label detailStatus;

    @FXML private Button paymentButton;
    @FXML private Button scheduleButton;

    private MainApp mainApp;
    private ObservableList<Loan> loans = FXCollections.observableArrayList();
    private Map<Long, Bank> banksCache = new HashMap<>();
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadBanks();
        loadLoans();
    }

    private void loadBanks() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getBanks");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                JsonArray banksArray = response.getAsJsonArray("banks");
                for (JsonElement element : banksArray) {
                    JsonObject bankJson = element.getAsJsonObject();
                    Bank bank = new Bank();
                    bank.setBankId(bankJson.get("bankId").getAsLong());
                    bank.setBankName(bankJson.get("bankName").getAsString());
                    banksCache.put(bank.getBankId(), bank);
                }
            }
        } catch (Exception e) {
            LOG.info("bankID");
            showError("Ошибка bad: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLoanType().getLoanTypeName()));

        amountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getLoanAmount()));

        rateColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getLoanType().getInterestRate()));

        termColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getTermMonths()));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        amountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%,.2f ₽", item));
            }
        });

        rateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f%%", item));
            }
        });

        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");
            private final HBox buttonsContainer = new HBox(5, deleteButton);

            {
                // Применяем стили
                buttonsContainer.getStyleClass().add("action-buttons-container");
                deleteButton.getStyleClass().add("delete-button");

                deleteButton.setOnAction(event -> {
                    Loan loan = getTableView().getItems().get(getIndex());
                    if (loan != null) {
                        handleDeleteLoan(loan);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Loan loan = getTableView().getItems().get(getIndex());
                    if (loan != null) {
                        deleteButton.setDisable(!"ACTIVE".equals(loan.getStatus()));
                        setGraphic(buttonsContainer);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Обработка выбора строки
        loansTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showLoanDetails(newSelection));
    }

    private void loadLoans() {
        try {
            JsonObject typesRequest = new JsonObject();
            typesRequest.addProperty("command", "getLoanTypes");

            JsonObject typesResponse = mainApp.getClient().sendRequest(typesRequest.toString());

            if (typesResponse == null || !typesResponse.get("status").getAsString().equals("success")) {
                showError("Не удалось загрузить информацию о банках");
                return;
            }

            JsonArray loanTypesArray = typesResponse.getAsJsonArray("loanTypes");
            Map<Long, String> bankNames = new HashMap<>();
            for (JsonElement element : loanTypesArray) {
                JsonObject typeJson = element.getAsJsonObject();
                long bankId = typeJson.get("bankId").getAsLong();
                String bankName = typeJson.get("bankName").getAsString();
                bankNames.put(bankId, bankName);
            }

            JsonObject loansRequest = new JsonObject();
            loansRequest.addProperty("command", "getClientLoans");
            loansRequest.addProperty("userId", mainApp.getCurrentUserId());

            JsonObject loansResponse = mainApp.getClient().sendRequest(loansRequest.toString());

            if (loansResponse != null && loansResponse.get("status").getAsString().equals("success")) {
                loans.clear();
                JsonArray loansArray = loansResponse.getAsJsonArray("loans");

                if (loansArray != null) {
                    for (JsonElement element : loansArray) {
                        if (element == null || !element.isJsonObject()) continue;

                        JsonObject loanJson = element.getAsJsonObject();
                        Loan loan = convertJsonToLoan(loanJson, bankNames);
                        loans.add(loan);
                    }
                }
                loansTable.setItems(loans);
                System.out.println("Total loans in table: " + loansTable.getItems().size());
            } else {
                showError(loansResponse != null ? loansResponse.get("message").getAsString() : "Не удалось загрузить кредиты");
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке кредитов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteLoan(Loan loan) {
        if (loan == null) return;

        if (!showConfirmation("Подтверждение удаления",
                "Вы уверены, что хотите удалить кредит " + loan.getLoanType().getLoanTypeName() + "?")) {
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "deleteLoan");
            request.addProperty("loanId", loan.getLoanId());
            request.addProperty("userId", mainApp.getCurrentUserId());

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                showSuccess("Кредит успешно удален");
                loans.remove(loan);
                clearDetails();
            } else {
                String errorMsg = response != null ? response.get("message").getAsString() : "Неизвестная ошибка";
                showError("Не удалось удалить кредит: " + errorMsg);
            }
        } catch (Exception e) {
            showError("Ошибка при удалении кредита: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Loan convertJsonToLoan(JsonObject loanJson, Map<Long, String> bankNames) {
        Loan loan = new Loan();
        loan.setLoanId(loanJson.get("id").getAsLong());
        loan.setLoanAmount(new BigDecimal(loanJson.get("amount").getAsString()));
        loan.setTermMonths(loanJson.get("termMonths").getAsInt());
        loan.setStatus(loanJson.get("status").getAsString());
        loan.setStartDate(LocalDate.parse(loanJson.get("startDate").getAsString()));

        if (loanJson.has("endDate") && !loanJson.get("endDate").isJsonNull()) {
            loan.setEndDate(LocalDate.parse(loanJson.get("endDate").getAsString()));
        }

        if (loanJson.has("loanType") && loanJson.get("loanType").isJsonObject()) {
            JsonObject typeJson = loanJson.getAsJsonObject("loanType");
            LoanType loanType = new LoanType();
            loanType.setLoanTypeId(typeJson.get("id").getAsLong());
            loanType.setLoanTypeName(typeJson.get("name").getAsString());
            loanType.setInterestRate(new BigDecimal(typeJson.get("rate").getAsString()));

            if (typeJson.has("bankId")) {
                Long bankId = typeJson.get("bankId").getAsLong();
                // Создаем объект Bank с именем из кэша
                Bank bank = new Bank();
                bank.setBankId(bankId);
                bank.setBankName(bankNames.getOrDefault(bankId, "Банк ID: " + bankId));
                loanType.setBank(bank);
            }

            loan.setLoanType(loanType);
        }

        return loan;
    }

    private void showLoanDetails(Loan loan) {
        if (loan == null) {
            clearDetails();
            return;
        }

        if (loan.getLoanType() != null && loan.getLoanType().getBank() != null) {
            detailBank.setText(loan.getLoanType().getBank().getBankName());
            detailType.setText(loan.getLoanType().getLoanTypeName());
            detailRate.setText(String.format("%.2f%%", loan.getLoanType().getInterestRate()));
        } else {
            detailBank.setText("Нет данных");
            detailType.setText(loan.getLoanType() != null ? loan.getLoanType().getLoanTypeName() : "Нет данных");
            detailRate.setText(loan.getLoanType() != null ? String.format("%.2f%%", loan.getLoanType().getInterestRate()) : "Нет данных");
        }

        detailAmount.setText(String.format("%,.2f ₽", loan.getLoanAmount()));
        detailTerm.setText(loan.getTermMonths() + " мес.");
        detailStatus.setText(loan.getStatus());

        if (loan.getStartDate() != null) {
            String datesText = "С " + loan.getStartDate();
            if (loan.getEndDate() != null) {
                datesText += " по " + loan.getEndDate();
            }
            detailDates.setText(datesText);
        } else {
            detailDates.setText("Дата начала не указана");
        }

        boolean isActive = "ACTIVE".equals(loan.getStatus());
        paymentButton.setDisable(!isActive);
        scheduleButton.setDisable(!isActive);
    }

    private void clearDetails() {
        detailBank.setText("");
        detailType.setText("");
        detailRate.setText("");
        detailAmount.setText("");
        detailTerm.setText("");
        detailDates.setText("");
        detailStatus.setText("");
        paymentButton.setDisable(true);
    }

    @FXML
    private void handlePayment() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите кредит");
            return;
        }

        if (!"ACTIVE".equals(selected.getStatus())) {
            showError("Нельзя оплатить закрытый кредит");
            return;
        }
        mainApp.showPaymentsView(selected);
    }

    private BigDecimal calculateRemainingDebt(Loan loan) {
        try {
            JsonObject response = mainApp.getClient().sendRequest(
                    new JsonObjectBuilder()
                            .add("command", "getRemainingDebt")
                            .add("loanId", loan.getLoanId())
                            .build().toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                return new BigDecimal(response.get("amount").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loan.getLoanAmount();
    }

    private void validateRegularPayment(Loan loan, BigDecimal amount) throws Exception {
        JsonObject response = mainApp.getClient().sendRequest(
                new JsonObjectBuilder()
                        .add("command", "getNextPayment")
                        .add("loanId", loan.getLoanId())
                        .build().toString());

        if (response != null && response.get("status").getAsString().equals("success")) {
            PaymentScheduleDTO nextPayment = gson.fromJson(
                    response.get("nextPayment"), PaymentScheduleDTO.class);

            if (amount.compareTo(nextPayment.getAmount()) < 0) {
                throw new Exception(String.format("Минимальный платеж: %,.2f ₽", nextPayment.getAmount()));
            }
        }
    }


    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
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

    private static class JsonObjectBuilder {
        private JsonObject json = new JsonObject();

        public JsonObjectBuilder add(String property, String value) {
            json.addProperty(property, value);
            return this;
        }

        public JsonObjectBuilder add(String property, Number value) {
            json.addProperty(property, value);
            return this;
        }

        public JsonObjectBuilder add(String property, Boolean value) {
            json.addProperty(property, value);
            return this;
        }

        public JsonObjectBuilder add(String property, Character value) {
            json.addProperty(property, value);
            return this;
        }

        public JsonObjectBuilder add(String property, JsonElement value) {
            json.add(property, value);
            return this;
        }

        public JsonObject build() {
            return json;
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
