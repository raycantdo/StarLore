package com.starlore.starlore;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {

    @FXML
    private Label titleLabel;

    @FXML
    public void initialize() {
        titleLabel.setOpacity(0);

        titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {

                FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), titleLabel);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                PauseTransition pause = new PauseTransition(Duration.seconds(1));

                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), newScene.getRoot());
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
                        root.setOpacity(0);
                        Stage stage = (Stage) titleLabel.getScene().getWindow();
                        stage.setScene(new Scene(root));

                        FadeTransition fadeInNew = new FadeTransition(Duration.seconds(2), root);
                        fadeInNew.setFromValue(0);
                        fadeInNew.setToValue(1);
                        fadeInNew.play();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                fadeIn.setOnFinished(e -> pause.play());
                pause.setOnFinished(e -> fadeOut.play());
                fadeIn.play();
            }
        });
    }
}