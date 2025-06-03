package client.viewFactories;

import client.MainApp;
import client.controllers.LoanStatisticsController;

public class LoanStatisticsCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/loan-statistics.fxml"; }

    @Override protected String getTitle() { return "Статистика"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        LoanStatisticsController accController = (LoanStatisticsController)controller;
        accController.setMainApp(mainApp);
    }
}
