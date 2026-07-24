package com.starlore.starlore;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
public class HelloController {
    @FXML
    private TextField nameField;
    private PlayerDAO playerDAO = new PlayerDAO();
    @FXML
    private Label welcomeLabel;

    @FXML
    private Button beginButton;

    @FXML
    private void checkPlayer() {

        String name = nameField.getText().trim();

        if(name.isEmpty())
            return;

        // Later we'll check if player exists

        try {
            Player player = playerDAO.checkOrCreatePlayer(name);

            // Load welcome screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("WelcomeView.fxml"));
            Parent root = loader.load();

            // Pass player data to WelcomeController
            WelcomeController controller = loader.getController();
            controller.setPlayer(player);

            root.setOpacity(0);
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));

            // Fade in welcome screen
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}