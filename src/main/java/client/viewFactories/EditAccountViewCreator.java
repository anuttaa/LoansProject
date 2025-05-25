package client.viewFactories;

import client.MainApp;
import client.controllers.EditAccountController;

public class EditAccountViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/editAccount.fxml"; }

    @Override protected String getTitle() { return "Редактирование аккаунта"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        EditAccountController accController = (EditAccountController)controller;
        accController.setMainApp(mainApp);
        accController.loadUserData();
    }
}
