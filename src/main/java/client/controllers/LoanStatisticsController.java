package client.controllers;

import client.MainApp;
import com.google.gson.JsonElement;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class LoanStatisticsController {
    @FXML private Label avgRateLabel;
    @FXML private Label popularBankLabel;
    @FXML private Label totalLoansLabel;
    @FXML private Label totalAmountLabel;

    @FXML private BarChart<String, Number> loansByBankChart;
    @FXML private PieChart ratesPieChart;
    @FXML private LineChart<String, Number> ratesTrendChart;

    private MainApp mainApp;
    private Stage dialogStage;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadStatistics();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void initialize() {
    }

    @FXML
    private void handleRefresh() {
        loadStatistics();
    }

    @FXML
    private void handleClose() {
        if (mainApp.getCurrentUserRoleId() == 1) {
            mainApp.showAdminAccountView();
        } else if (mainApp.getCurrentUserRoleId() == 2) {
            mainApp.showAccountView();
        } else {
            mainApp.clearCurrentUser();
        }
    }

    private void loadStatistics() {
        if (mainApp == null || mainApp.getClient() == null) {
            showError("Не удалось загрузить статистику: нет подключения");
            return;
        }
        try {
            JsonObject typesRequest = new JsonObject();
            typesRequest.addProperty("command", "getLoanStatistics");
            JsonObject response = mainApp.getClient().sendRequest(typesRequest.toString());

            if (response != null && response.get("status").getAsString().equals("success")) {
                // Основная статистика
                JsonObject stats = response.getAsJsonObject("statistics");
                avgRateLabel.setText(String.format("%.2f%%", stats.get("avgRate").getAsDouble()));
                popularBankLabel.setText(stats.get("popularBank").getAsString());
                totalLoansLabel.setText(stats.get("totalLoans").getAsString());
                totalAmountLabel.setText(String.format("%,.2f BYN", stats.get("totalAmount").getAsDouble()));

                // График по банкам
                loansByBankChart.getData().clear();
                XYChart.Series<String, Number> bankSeries = new XYChart.Series<>();
                JsonArray banksData = stats.getAsJsonArray("loansByBank");
                for (JsonElement item : banksData) {
                    JsonObject bank = item.getAsJsonObject();
                    bankSeries.getData().add(new XYChart.Data<>(
                            bank.get("bankName").getAsString(),
                            bank.get("count").getAsInt()
                    ));
                }
                loansByBankChart.getData().add(bankSeries);

                // Круговая диаграмма ставок
                ratesPieChart.getData().clear();
                JsonArray ratesData = stats.getAsJsonArray("ratesDistribution");
                for (JsonElement item : ratesData) {
                    JsonObject rate = item.getAsJsonObject();
                    ratesPieChart.getData().add(new PieChart.Data(
                            rate.get("range").getAsString(),
                            rate.get("count").getAsDouble()
                    ));
                }

                // График динамики ставок
                ratesTrendChart.getData().clear();
                XYChart.Series<String, Number> trendSeries = new XYChart.Series<>();
                trendSeries.setName("Средняя ставка");
                JsonArray trendData = stats.getAsJsonArray("ratesTrend");
                for (JsonElement item : trendData) {
                    JsonObject month = item.getAsJsonObject();
                    trendSeries.getData().add(new XYChart.Data<>(
                            month.get("month").getAsString(),
                            month.get("avgRate").getAsDouble()
                    ));
                }
                ratesTrendChart.getData().add(trendSeries);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
