package com.starlore.starlore;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("SplashView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),950,700);
        stage.setTitle("StarLore");
        stage.setScene(scene);
        stage.show();
    }
}
