package client.controllers;

import client.MainApp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import config.LocalDateAdapter;
import enums.PaymentType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import server.DTO.PaymentDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoanPaymentsController {
    @FXML private Label titleLabel;
    @FXML private Label remainingAmountLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<PaymentDTO> paymentsTable;
    @FXML private TableColumn<PaymentDTO, LocalDate> paymentDateColumn;
    @FXML private TableColumn<PaymentDTO, BigDecimal> paymentAmountColumn;
    @FXML private TableColumn<PaymentDTO, String> paymentTypeColumn;

    @FXML private TableView<PaymentScheduleDTO> scheduleTable;
    @FXML private TableColumn<PaymentScheduleDTO, LocalDate> scheduleDateColumn;
    @FXML private TableColumn<PaymentScheduleDTO, BigDecimal> scheduleAmountColumn;
    @FXML private TableColumn<PaymentScheduleDTO, BigDecimal> principalColumn;
    @FXML private TableColumn<PaymentScheduleDTO, BigDecimal> interestColumn;

    private Loan currentLoan;
    private MainApp mainApp;
    private ObservableList<PaymentDTO> paymentsData = FXCollections.observableArrayList();
    private ObservableList<PaymentScheduleDTO> scheduleData = FXCollections.observableArrayList();
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    private PaymentScheduleDTO nextPayment;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setLoan(Loan loan) {
        this.currentLoan = loan;
        updateLoanInfo();
        loadPaymentHistory();
        loadPaymentSchedule();
    }

    @FXML
    private void initialize() {
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        paymentsTable.setItems(paymentsData);

        scheduleDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        scheduleAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        principalColumn.setCellValueFactory(new PropertyValueFactory<>("principalPart"));
        interestColumn.setCellValueFactory(new PropertyValueFactory<>("interestPart"));
        scheduleTable.setItems(scheduleData);
    }

    private void updateLoanInfo() {
        titleLabel.setText("Кредит #" + currentLoan.getLoanId());
        remainingAmountLabel.setText(String.format("%,.2f ₽", calculateRemainingDebt(currentLoan)));
        statusLabel.setText(currentLoan.getStatus());
    }

    @FXML
    private void handleNewPayment() {
        if (currentLoan == null) return;

        Dialog<PaymentType> dialog = new Dialog<>();
        dialog.setTitle("Тип платежа");
        dialog.setHeaderText("Выберите тип платежа для кредита #" + currentLoan.getLoanId());

        ComboBox<PaymentType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(PaymentType.values());
        typeCombo.setConverter(new StringConverter<PaymentType>() {
            @Override public String toString(PaymentType type) {
                return type.getDescription();
            }
            @Override public PaymentType fromString(String string) {
                return Arrays.stream(PaymentType.values())
                        .filter(t -> t.getDescription().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        typeCombo.getSelectionModel().selectFirst();

        dialog.getDialogPane().setContent(typeCombo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK ? typeCombo.getValue() : null);

        dialog.showAndWait().ifPresent(this::showPaymentAmountDialog);
    }

    private void showPaymentAmountDialog(PaymentType type) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Сумма платежа");
        dialog.setHeaderText(String.format("Введите сумму для %s", type.getDescription()));

        BigDecimal remainingAmount = calculateRemainingDebt(currentLoan);

        BigDecimal minPartialPayment = calculateMinPartialPrepayment();
        BigDecimal minPayment = getNextMinPaymentAmount();

        TextField amountField = new TextField();
        Label infoLabel = new Label();
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        switch (type) {
            case FULL_PREPAYMENT:
                amountField.setText(remainingAmount.toString());
                amountField.setDisable(true);
                infoLabel.setText(String.format("Полное погашение: %,.2f ₽", remainingAmount));
                break;
            case PARTIAL_PREPAYMENT:
                amountField.setText(minPartialPayment.toString());
                infoLabel.setText(String.format("Минимальная сумма: %,.2f ₽ (1.5x от платежа или 15%% от долга)\nОстаток: %,.2f ₽",
                        minPartialPayment, remainingAmount));
                break;
            case REGULAR:
                amountField.setText(minPayment.toString());
                amountField.setDisable(true);
                infoLabel.setText(String.format("Платеж по графику: %,.2f ₽", minPayment));
                break;
        }

        VBox content = new VBox(10, infoLabel, amountField, errorLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldVal);
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    validatePayment(type, amount, remainingAmount, minPayment);
                    return amount;
                } catch (Exception e) {
                    errorLabel.setText(e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(amount -> {
            if (showConfirmation("Подтверждение",
                    String.format("Вы уверены, что хотите внести %,.2f ₽?", amount))) {
                processPayment(type, amount);
            }
        });
    }

    private BigDecimal getNextMinPaymentAmount() {
        if (scheduleData.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return scheduleData.stream()
                .filter(p -> p.getDueDate().isAfter(LocalDate.now().minusDays(1)))
                .min(Comparator.comparing(PaymentScheduleDTO::getDueDate))
                .map(PaymentScheduleDTO::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateMinPartialPrepayment() {
        if (currentLoan == null || scheduleData.isEmpty()) {
            return BigDecimal.ZERO;
        }

        PaymentScheduleDTO nextPayment = scheduleData.stream()
                .filter(p -> p.getDueDate().isAfter(LocalDate.now()) ||
                        p.getDueDate().isEqual(LocalDate.now()))
                .min(Comparator.comparing(PaymentScheduleDTO::getDueDate))
                .orElse(null);

        if (nextPayment == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal remainingDebt = calculateRemainingDebt(currentLoan);

        BigDecimal minPrepayment = nextPayment.getAmount().multiply(new BigDecimal("1.5"));
        BigDecimal minPercent = remainingDebt.multiply(new BigDecimal("0.15"));

        return minPrepayment.max(minPercent);
    }

    private void validatePayment(PaymentType type, BigDecimal amount,
                                 BigDecimal remainingAmount, BigDecimal minPayment) throws Exception {
        switch (type) {
            case REGULAR:
                if (amount.compareTo(minPayment) != 0) {
                    throw new Exception("Платеж должен точно соответствовать графику");
                }
                break;
            case PARTIAL_PREPAYMENT:
                if (amount.compareTo(minPayment) < 0) {
                    throw new Exception(String.format("Сумма должна быть не менее %,.2f ₽", minPayment));
                }
                if (amount.compareTo(remainingAmount) > 0) {
                    throw new Exception("Сумма превышает остаток долга");
                }
                break;
            case FULL_PREPAYMENT:
                if (amount.compareTo(remainingAmount) != 0) {
                    throw new Exception("Сумма должна точно соответствовать остатку долга");
                }
                break;
        }
    }

    private CompletableFuture<Void> loadPaymentSchedule() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Task<List<PaymentScheduleDTO>> task = new Task<>() {
            @Override
            protected List<PaymentScheduleDTO> call() throws Exception {
                JsonObject response = mainApp.getClient().sendRequest(
                        new JsonObjectBuilder()
                                .add("command", "getPaymentSchedule")
                                .add("loanId", currentLoan.getLoanId())
                                .build().toString());

                if (response != null && response.get("status").getAsString().equals("success")) {
                    return Arrays.asList(gson.fromJson(
                            response.getAsJsonArray("schedule"),
                            PaymentScheduleDTO[].class));
                }
                throw new Exception("Не удалось загрузить график платежей");
            }
        };

        task.setOnSucceeded(e -> {
            scheduleData.setAll(task.getValue());
            future.complete(null);
        });

        task.setOnFailed(e -> {
            showError("Ошибка загрузки графика: " + task.getException().getMessage());
            future.completeExceptionally(task.getException());
        });

        new Thread(task).start();
        return future;
    }

    private void loadPaymentHistory() {
        Task<List<PaymentDTO>> task = new Task<>() {
            @Override
            protected List<PaymentDTO> call() throws Exception {
                JsonObject response = mainApp.getClient().sendRequest(
                        new JsonObjectBuilder()
                                .add("command", "getPaymentHistory")
                                .add("loanId", currentLoan.getLoanId())
                                .build().toString());

                if (response != null && response.get("status").getAsString().equals("success")) {
                    return Arrays.asList(gson.fromJson(
                            response.getAsJsonArray("payments"),
                            PaymentDTO[].class));
                }
                throw new Exception("Не удалось загрузить историю платежей");
            }
        };

        task.setOnSucceeded(e -> paymentsData.setAll(task.getValue()));
        task.setOnFailed(e -> showError("Ошибка загрузки истории: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    private void processPayment(PaymentType type, BigDecimal amount) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                JsonObject paymentData = new JsonObject();
                paymentData.addProperty("loanId", currentLoan.getLoanId());
                paymentData.addProperty("amount", amount);
                paymentData.addProperty("type", type.name());
                paymentData.addProperty("date", LocalDate.now().toString());

                JsonObject response = mainApp.getClient().sendRequest(
                        new JsonObjectBuilder()
                                .add("command", "processPayment")
                                .add("paymentData", paymentData)
                                .build().toString());

                return response != null && response.get("status").getAsString().equals("success");
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                showSuccess("Платеж успешно проведен");
                updateLoanInfo();
                loadPaymentHistory();
                loadPaymentSchedule();
            } else {
                showError("Ошибка при проведении платежа");
            }
        });

        task.setOnFailed(e -> {
            showError("Ошибка: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleClose() {
        titleLabel.getScene().getWindow().hide();
    }

    private BigDecimal calculateRemainingDebt(Loan loan) {
        try {
            JsonObject response = mainApp.getClient().sendRequest(
                    new LoanPaymentsController.JsonObjectBuilder()
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

        public LoanPaymentsController.JsonObjectBuilder add(String property, String value) {
            json.addProperty(property, value);
            return this;
        }

        public LoanPaymentsController.JsonObjectBuilder add(String property, Number value) {
            json.addProperty(property, value);
            return this;
        }

        public LoanPaymentsController.JsonObjectBuilder add(String property, Boolean value) {
            json.addProperty(property, value);
            return this;
        }

        public LoanPaymentsController.JsonObjectBuilder add(String property, Character value) {
            json.addProperty(property, value);
            return this;
        }

        public LoanPaymentsController.JsonObjectBuilder add(String property, JsonElement value) {
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


