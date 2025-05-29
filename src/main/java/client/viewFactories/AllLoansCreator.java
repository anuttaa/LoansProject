package client.viewFactories;

import client.MainApp;
import client.controllers.AllClientsLoansController;

public class AllLoansCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/AllClientsLoansView.fxml"; }

    @Override protected String getTitle() { return "Все кредиты"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        AllClientsLoansController accController = (AllClientsLoansController)controller;
        accController.setMainApp(mainApp);
    }
}
