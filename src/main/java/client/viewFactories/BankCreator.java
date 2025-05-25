package client.viewFactories;

import client.MainApp;
import client.controllers.CreateBankController;

public class BankCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/banksView.fxml"; }

    @Override protected String getTitle() { return "Банки"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        CreateBankController accController = (CreateBankController)controller;
        accController.setMainApp(mainApp);
    }
}
