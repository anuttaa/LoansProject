package client.viewFactories;

import client.MainApp;
import client.controllers.AccountController;

public class ClientAccountViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/clientAccount.fxml"; }

    @Override protected String getTitle() { return "Аккаунт клиента"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        AccountController accController = (AccountController)controller;
        accController.setMainApp(mainApp);
    }
}
