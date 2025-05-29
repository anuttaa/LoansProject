package client.viewFactories;

import client.MainApp;
import client.controllers.EditBankController;

public class EditBankCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/EditBank.fxml"; }

    @Override protected String getTitle() { return "Редактирование банков"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        EditBankController accController = (EditBankController)controller;
        accController.setMainApp(mainApp);
    }
}
