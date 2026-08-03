package com.starlore.starlore;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WelcomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label subLabel;
    @FXML private Button journeyButton;

    private Player currentPlayer;

    public void setPlayer(Player player) {
        this.currentPlayer = player;

        if (player.isNewPlayer()) {
            welcomeLabel.setText("✨ Welcome, " + player.getUsername() + ".");
            subLabel.setText("The heavens recognize a new astronomer.\nYour journey among the stars begins now.");
            journeyButton.setText("Begin Journey");
        } else {
            welcomeLabel.setText("✨ Welcome back, " + player.getUsername() + ".");
            subLabel.setText("The constellations remember your path.\nContinue your journey among the stars.");
            journeyButton.setText("Continue Journey");
        }
    }

    @FXML
    private void startJourney() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();

            GameHubController controller = loader.getController();
            controller.setPlayer(currentPlayer);

            root.setOpacity(0);
            Stage stage = (Stage) journeyButton.getScene().getWindow();
            stage.setScene(new Scene(root));

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}