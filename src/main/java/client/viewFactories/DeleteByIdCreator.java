package client.viewFactories;

import client.MainApp;
import client.controllers.DeleteUserController;

public class DeleteByIdCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/deleteByID.fxml"; }

    @Override protected String getTitle() { return "Удаление пользователя"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        DeleteUserController accController = (DeleteUserController)controller;
        accController.setMainApp(mainApp);
    }
}
