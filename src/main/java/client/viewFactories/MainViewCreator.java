package client.viewFactories;

import client.MainApp;
import client.controllers.MainController;

public class MainViewCreator extends ViewCreator {
    @Override protected String getFxmlPath() { return "/main.fxml"; }

    @Override protected String getTitle() { return "Вход в систему"; }

    @Override protected void setupController(Object controller, MainApp mainApp) {
        MainController accController = (MainController)controller;
        accController.setMainApp(mainApp);
    }
}
