package client.controllers;

import client.MainApp;
import com.google.gson.*;
import config.LocalDateAdapter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import server.DTO.LoanDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import java.math.BigDecimal;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.LoanType;
import server.Entities.User;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AllClientsLoansController {
    @FXML private TableView<LoanDTO> loansTable;
    @FXML private TableColumn<LoanDTO, String> loanIdColumn;
    @FXML private TableColumn<LoanDTO, String> clientColumn;
    @FXML private TableColumn<LoanDTO, String> bankColumn;
    @FXML private TableColumn<LoanDTO, String> typeColumn;
    @FXML private TableColumn<LoanDTO, BigDecimal> amountColumn;
    @FXML private TableColumn<LoanDTO, String> statusColumn;
    @FXML private TableColumn<LoanDTO, Void> actionsColumn;

    @FXML private VBox loanDetailsPane;
    @FXML private Label detailLoanId;
    @FXML private Label detailClient;
    @FXML private Label detailBank;
    @FXML private Label detailType;
    @FXML private Label detailAmount;
    @FXML private Label detailTerm;
    @FXML private Label detailStatus;

    private MainApp mainApp;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadLoansData();
    }

    @FXML
    private void initialize() {
        initializeTableColumns();
        loanDetailsPane.setVisible(false);

        loansTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showLoanDetails(newSelection));
    }

    private void initializeTableColumns() {
        loanIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLoanId().toString()));

        clientColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("#" + cellData.getValue().getClientName()));

        bankColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("#" + cellData.getValue().getBankName()));

        typeColumn.setCellValueFactory(new PropertyValueFactory<>("loanTypeName"));

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("loanAmount"));
        amountColumn.setCellFactory(tc -> new TableCell<LoanDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("%,.2f ₽", amount));
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        actionsColumn.setCellFactory(param -> new TableCell<LoanDTO, Void>() {
            private final Button editButton = new Button("Изменить");
            private final Button deleteButton = new Button("Удалить");
            private final HBox buttonsContainer = new HBox(5, editButton, deleteButton);

            {
                buttonsContainer.getStyleClass().add("action-buttons-container");
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");

                editButton.setOnAction(event -> {
                    LoanDTO loan = getTableView().getItems().get(getIndex());
                    handleEditLoan(loan);
                });

                deleteButton.setOnAction(event -> {
                    LoanDTO loan = getTableView().getItems().get(getIndex());
                    handleDeleteLoan(loan);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LoanDTO loan = getTableView().getItems().get(getIndex());
                    boolean canEdit = "PENDING".equals(loan.getStatus()) || "ACTIVE".equals(loan.getStatus());
                    editButton.setDisable(!canEdit);
                    deleteButton.setDisable(!"ACTIVE".equals(loan.getStatus()));
                    setGraphic(buttonsContainer);
                }
            }
        });
    }

    private void loadLoansData() {
        if (mainApp == null || mainApp.getClient() == null) {
            showError("Приложение не инициализировано");
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getAllLoans");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response == null) {
                showError("Нет ответа от сервера");
                return;
            }

            if (response.get("status").getAsString().equals("success")) {
                LoanDTO[] loans = gson.fromJson(response.getAsJsonArray("loans"), LoanDTO[].class);
                Platform.runLater(() -> {
                    loansTable.getItems().setAll(loans);
                    System.out.println("Загружено кредитов: " + loans.length);
                });
            } else {
                String errorMsg = response.has("message")
                        ? response.get("message").getAsString()
                        : "Неизвестная ошибка";
                showError("Ошибка загрузки: " + errorMsg);
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке кредитов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showLoanDetails(LoanDTO loan) {
        if (loan == null) {
            loanDetailsPane.setVisible(false);
            return;
        }

        Platform.runLater(() -> {
            detailLoanId.setText(loan.getLoanId().toString());
            detailClient.setText("#" + loan.getClientId());
            detailBank.setText("#" + loan.getBankId());
            detailType.setText(loan.getLoanTypeName());
            detailAmount.setText(String.format("%,.2f ₽", loan.getLoanAmount()));
            detailTerm.setText(loan.getTermMonths() + " мес");
            detailStatus.setText(loan.getStatus());
            loanDetailsPane.setVisible(true);
        });
    }

    private void handleEditLoan(LoanDTO loanDto) {
        try {
            // Загружаем детали кредита для редактирования
            JsonObject detailsRequest = new JsonObject();
            detailsRequest.addProperty("command", "getLoanDetails");
            detailsRequest.addProperty("loanId", loanDto.getLoanId());

            JsonObject detailsResponse = mainApp.getClient().sendRequest(detailsRequest.toString());

            if (detailsResponse == null || !detailsResponse.get("status").getAsString().equals("success")) {
                showError("Не удалось загрузить данные кредита");
                return;
            }

            JsonObject loanJson = detailsResponse.getAsJsonObject("loan");
            Loan loan = gson.fromJson(loanJson, Loan.class);

            // Загружаем доступные типы кредитов
            List<LoanType> availableTypes = loadAvailableLoanTypes();
            if (availableTypes.isEmpty()) {
                showError("Нет доступных типов кредитов");
                return;
            }

            // Создаем диалоговое окно
            Dialog<Loan> dialog = new Dialog<>();
            dialog.setTitle("Редактирование кредита");
            dialog.setHeaderText("Изменение параметров кредита");

            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            ComboBox<LoanType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(availableTypes));
            typeCombo.setConverter(new StringConverter<LoanType>() {
                @Override
                public String toString(LoanType type) {
                    if (type == null) return "";
                    String bankName = (type.getBank() != null) ? type.getBank().getBankName() : "не указан";
                    return type.getLoanTypeName() + " (" + bankName + ")";
                }

                @Override
                public LoanType fromString(String string) {
                    return null;
                }
            });

            // Устанавливаем выбранный тип кредита
            if (loan.getLoanType() != null) {
                typeCombo.getSelectionModel().select(loan.getLoanType());
            } else if (!availableTypes.isEmpty()) {
                typeCombo.getSelectionModel().selectFirst();
            }

            // Остальной код метода остается без изменений
            TextField amountField = new TextField(loan.getLoanAmount().toString());
            TextField termField = new TextField(String.valueOf(loan.getTermMonths()));

            DatePicker startDatePicker = new DatePicker(loan.getStartDate());
            DatePicker endDatePicker = new DatePicker(loan.getEndDate());

            ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("PENDING", "ACTIVE", "CLOSED"));
            statusCombo.getSelectionModel().select(loan.getStatus());

            // Валидация числовых полей
            amountField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    amountField.setText(oldValue);
                }
            });

            termField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    termField.setText(oldValue);
                }
            });

            grid.add(new Label("Тип кредита:"), 0, 0);
            grid.add(typeCombo, 1, 0);
            grid.add(new Label("Сумма кредита:"), 0, 1);
            grid.add(amountField, 1, 1);
            grid.add(new Label("Срок (мес.):"), 0, 2);
            grid.add(termField, 1, 2);
            grid.add(new Label("Дата начала:"), 0, 3);
            grid.add(startDatePicker, 1, 3);
            grid.add(new Label("Дата окончания:"), 0, 4);
            grid.add(endDatePicker, 1, 4);
            grid.add(new Label("Статус:"), 0, 5);
            grid.add(statusCombo, 1, 5);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        Loan updatedLoan = new Loan();
                        updatedLoan.setLoanId(loan.getLoanId());
                        updatedLoan.setLoanType(typeCombo.getValue());
                        updatedLoan.setLoanAmount(new BigDecimal(amountField.getText()));
                        updatedLoan.setTermMonths(Integer.parseInt(termField.getText()));
                        updatedLoan.setStartDate(startDatePicker.getValue());
                        updatedLoan.setEndDate(endDatePicker.getValue());
                        updatedLoan.setStatus(statusCombo.getValue());
                        updatedLoan.setClient(loan.getClient());

                        return updatedLoan;
                    } catch (Exception e) {
                        showError("Некорректные данные: " + e.getMessage());
                        return null;
                    }
                }
                return null;
            });

            Optional<Loan> result = dialog.showAndWait();
            result.ifPresent(updatedLoan -> {
                try {
                    JsonObject updateRequest = new JsonObject();
                    JsonObject data = new JsonObject();
                    data.addProperty("loanId", updatedLoan.getLoanId());
                    data.addProperty("loanTypeId", updatedLoan.getLoanType().getLoanTypeId());
                    data.addProperty("amount", updatedLoan.getLoanAmount().toString());
                    data.addProperty("termMonths", updatedLoan.getTermMonths());
                    data.addProperty("startDate", updatedLoan.getStartDate().toString());
                    updateRequest.add("data", data);
                    updateRequest.addProperty("command", "updateLoan");
                    if (updatedLoan.getEndDate() != null) {
                        updateRequest.addProperty("endDate", updatedLoan.getEndDate().toString());
                    }

                    updateRequest.addProperty("status", updatedLoan.getStatus());
                    updateRequest.addProperty("userId", mainApp.getCurrentUserId());

                    JsonObject updateResponse = mainApp.getClient().sendRequest(updateRequest.toString());

                    if (updateResponse != null && updateResponse.get("status").getAsString().equals("success")) {
                        showSuccess("Кредит успешно обновлен");
                        loadLoansData(); // Перезагружаем данные
                    } else {
                        String errorMsg = updateResponse != null
                                ? updateResponse.get("message").getAsString()
                                : "Неизвестная ошибка";
                        showError("Не удалось обновить кредит: " + errorMsg);
                    }
                } catch (Exception e) {
                    showError("Ошибка при обновлении: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            showError("Ошибка при редактировании: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<LoanType> loadAvailableLoanTypes() {
        List<LoanType> types = new ArrayList<>();
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getLoanTypes");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                JsonArray typesArray = response.getAsJsonArray("loanTypes");
                types = gson.fromJson(typesArray, new TypeToken<List<LoanType>>(){}.getType());
            }
        } catch (Exception e) {
            showError("Ошибка загрузки типов кредитов: " + e.getMessage());
        }
        return types;
    }

    private void handleDeleteLoan(LoanDTO loan) {
        if (loan == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Вы уверены, что хотите удалить этот кредит?");
        confirmation.setContentText(String.format(
                "ID: %d\nТип: %s\nСумма: %,.2f ₽\nКлиент: #%d",
                loan.getLoanId(),
                loan.getLoanTypeName(),
                loan.getLoanAmount(),
                loan.getClientId()
        ));

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteLoanFromServer(loan.getLoanId());
        }
    }

    private void deleteLoanFromServer(Long loanId) {
        new Thread(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "deleteLoan");
                request.addProperty("loanId", loanId);
                request.addProperty("userId", mainApp.getCurrentUserId());

                JsonObject response = mainApp.getClient().sendRequest(request.toString());

                Platform.runLater(() -> {
                    if (response != null && response.get("status").getAsString().equals("success")) {
                        showSuccess("Кредит успешно удален");
                        loadLoansData();
                        loanDetailsPane.setVisible(false);
                    } else {
                        String errorMsg = response != null
                                ? response.get("message").getAsString()
                                : "Неизвестная ошибка";
                        showError("Ошибка удаления: " + errorMsg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Ошибка соединения: " + e.getMessage()));
            }
        }).start();
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

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успешно");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
