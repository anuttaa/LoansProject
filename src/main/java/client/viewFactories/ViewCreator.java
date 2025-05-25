package client.viewFactories;

import client.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public abstract class ViewCreator {
    protected abstract String getFxmlPath();
    protected abstract String getTitle();
    protected abstract void setupController(Object controller, MainApp mainApp);

    public void showView(MainApp mainApp, Stage primaryStage) {
        try {
            URL fxmlUrl = getClass().getResource(getFxmlPath());
            if (fxmlUrl == null) {
                throw new IOException("Файл не найден: " + getFxmlPath());
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            setupController(loader.getController(), mainApp);

            primaryStage.setTitle(getTitle());
            primaryStage.setScene(new Scene(root, 600, 400));
            primaryStage.show();
        } catch (IOException e) {
            mainApp.showError("Ошибка", "Не удалось загрузить сцену " + getTitle() +
                    "\nПричина: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

