package client.controllers;

import client.MainApp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import config.LocalDateAdapter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import server.DTO.LoanDTO;

import java.time.LocalDate;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import java.math.BigDecimal;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import server.Entities.Loan;

public class AllClientsLoansController {
    @FXML private TableView<LoanDTO> loansTable;
    @FXML private TableColumn<LoanDTO, String> loanIdColumn;
    @FXML private TableColumn<LoanDTO, String> clientColumn;
    @FXML private TableColumn<LoanDTO, String> bankColumn;
    @FXML private TableColumn<LoanDTO, String> typeColumn;
    @FXML private TableColumn<LoanDTO, BigDecimal> amountColumn;
    @FXML private TableColumn<LoanDTO, String> statusColumn;
    @FXML private TableColumn<LoanDTO, LoanDTO> actionsColumn;

    @FXML private VBox loanDetailsPane;
    @FXML private Label detailLoanId;
    @FXML private Label detailClient;
    @FXML private Label detailBank;
    @FXML private Label detailType;
    @FXML private Label detailAmount;
    @FXML private Label detailTerm;
    @FXML private Label detailStatus;

    private MainApp mainApp;
    Gson gson = new GsonBuilder()
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
                new SimpleStringProperty("#" + cellData.getValue().getClientId()));

        bankColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("#" + cellData.getValue().getBankId()));

        typeColumn.setCellValueFactory(new PropertyValueFactory<>("loanTypeName"));

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("loanAmount"));
        amountColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("%,.2f ₽", amount));
            }
        });

        actionsColumn.setCellFactory(column -> new TableCell<LoanDTO, LoanDTO>() {
            private final Button deleteButton = new Button("Удалить");

            {
                // Настройка кнопки
                deleteButton.getStyleClass().add("danger-button");
                deleteButton.setPrefWidth(100);
                deleteButton.setOnAction(event -> {
                    LoanDTO loan = getTableView().getItems().get(getIndex());
                    handleDeleteLoan(loan);
                });
            }

            @Override
            protected void updateItem(LoanDTO loan, boolean empty) {
                super.updateItem(loan, empty);

                if (empty || getTableView().getItems().size() <= getIndex()) {
                    setGraphic(null);
                } else {
                    //deleteButton.setDisable(loan == null || !"ACTIVE".equals(loan.getStatus()));
                    setGraphic(deleteButton);
                }
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadLoansData() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "getAllLoans");

            JsonObject response = mainApp.getClient().sendRequest(request.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                LoanDTO[] loans = gson.fromJson(response.getAsJsonArray("loans"), LoanDTO[].class);
                loansTable.getItems().setAll(loans);
            } else {
                showError("Ошибка загрузки данных: " +
                        (response != null ? response.get("message").getAsString() : "Нет ответа от сервера"));
            }
        } catch (Exception e) {
            showError("Ошибка при загрузке кредитов: " + e.getMessage());
        }
    }

    private void showLoanDetails(LoanDTO loan) {
        if (loan == null) {
            loanDetailsPane.setVisible(false);
            return;
        }

        detailLoanId.setText(loan.getLoanId().toString());
        detailClient.setText("#" + loan.getClientId());
        detailBank.setText("#" + loan.getBankId());
        detailType.setText(loan.getLoanTypeName());
        detailAmount.setText(String.format("%,.2f ₽", loan.getLoanAmount()));
        detailTerm.setText(loan.getTermMonths() + " мес");
        detailStatus.setText(loan.getStatus());

        loanDetailsPane.setVisible(true);
    }

    private void handleDeleteLoan(LoanDTO loan) {
        if (loan == null) return;

        // Создаем более информативное подтверждение
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Вы уверены, что хотите удалить этот кредит?");

        String contentText = String.format(
                "ID кредита: %d\nТип: %s\nСумма: %,.2f ₽\nКлиент: #%d\nБанк: #%d",
                loan.getLoanId(),
                loan.getLoanTypeName(),
                loan.getLoanAmount(),
                loan.getClientId(),
                loan.getBankId()
        );
        confirmation.setContentText(contentText);

        // Добавляем кастомные кнопки
        confirmation.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            deleteLoanFromServer(loan.getLoanId());
        }
    }

    // 3. Улучшаем метод deleteLoanFromServer()
    private void deleteLoanFromServer(Long loanId) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("command", "deleteLoan");
            request.addProperty("loanId", loanId);
            request.addProperty("userId", mainApp.getCurrentUserId());

            new Thread(() -> {
                try {
                    JsonObject response = mainApp.getClient().sendRequest(request.toString());

                    Platform.runLater(() -> {

                        if (response != null && response.get("status").getAsString().equals("success")) {
                            showAlert(Alert.AlertType.INFORMATION, "Успешно",
                                    "Кредит успешно удален");

                            // Обновляем данные
                            loadLoansData();
                            loanDetailsPane.setVisible(false);
                        } else {
                            String errorMsg = response != null ?
                                    response.get("message").getAsString() : "Неизвестная ошибка";
                            showError("Не удалось удалить кредит: " + errorMsg);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showError("Ошибка соединения: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            showError("Ошибка при формировании запроса: " + e.getMessage());
        }
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Ошибка", message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
