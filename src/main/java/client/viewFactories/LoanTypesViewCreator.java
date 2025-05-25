package client.viewFactories;

import client.MainApp;
import client.controllers.EditAccountController;
import client.controllers.LoanTypesViewController;

public class LoanTypesViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/loansView.fxml"; }

    @Override protected String getTitle() { return "Просмотр кредитов"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        LoanTypesViewController accController = (LoanTypesViewController)controller;
        accController.setMainApp(mainApp);
    }
}
