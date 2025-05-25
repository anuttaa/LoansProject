package client.viewFactories;

import client.MainApp;
import client.controllers.AdminAccountController;

public class AdminAccountViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/adminAccount.fxml"; }

    @Override protected String getTitle() { return "Аккаунт администратора"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        AdminAccountController accController = (AdminAccountController)controller;
        accController.setMainApp(mainApp);
    }
}
