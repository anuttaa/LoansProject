package client.viewFactories;

import client.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
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

            // Создаем сцену и настраиваем привязки размеров
            Scene scene = new Scene(root);

            // Принудительно устанавливаем размеры root-элемента
            if (root instanceof Region) {
                Region region = (Region) root;
                region.setPrefSize(primaryStage.getWidth(), primaryStage.getHeight());
            }

            // Настройка окна
            primaryStage.setTitle(getTitle());
            primaryStage.setScene(scene);

            // Всегда открываем на весь экран
            primaryStage.setMaximized(true); // Развернуть с рамкой

            // ИЛИ для настоящего полноэкранного режима:
            // primaryStage.setFullScreen(true);

            // Привязываем размеры содержимого к размерам окна
            bindRootToStage(root, primaryStage);

            primaryStage.show();

        } catch (IOException e) {
            mainApp.showError("Ошибка", "Не удалось загрузить сцену " + getTitle() +
                    "\nПричина: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void bindRootToStage(Parent root, Stage stage) {
        // Для AnchorPane
        if (root instanceof AnchorPane) {
            AnchorPane pane = (AnchorPane) root;
            AnchorPane.setTopAnchor(pane, 0.0);
            AnchorPane.setBottomAnchor(pane, 0.0);
            AnchorPane.setLeftAnchor(pane, 0.0);
            AnchorPane.setRightAnchor(pane, 0.0);
        }
        // Для других контейнеров
        else if (root instanceof Region) {
            Region region = (Region) root;
            region.prefWidthProperty().bind(stage.widthProperty());
            region.prefHeightProperty().bind(stage.heightProperty());
        }
    }
}

