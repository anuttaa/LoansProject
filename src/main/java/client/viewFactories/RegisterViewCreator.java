package client.viewFactories;

import client.MainApp;
import client.controllers.RegisterController;

public class RegisterViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/register.fxml"; }

    @Override protected String getTitle() { return "Регистрация"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        RegisterController accController = (RegisterController)controller;
        accController.setMainApp(mainApp);
    }
}
