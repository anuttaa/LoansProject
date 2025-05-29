package client.viewFactories;

import client.MainApp;
import client.controllers.LoansViewController;

public class LoansViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/myLoansView.fxml"; }

    @Override protected String getTitle() { return "Мои кредиты"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        LoansViewController accController = (LoansViewController)controller;
        accController.setMainApp(mainApp);
    }
}
