package client.viewFactories;

import client.MainApp;
import client.controllers.EditAccountController;
import client.controllers.LoanPaymentsController;

public class PaymentsCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/loan-payments-view.fxml"; }

    @Override protected String getTitle() { return "Просмотр платежей"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        LoanPaymentsController accController = (LoanPaymentsController)controller;
        accController.setMainApp(mainApp);
    }
}
