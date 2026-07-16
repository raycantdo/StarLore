package com.starlore.starlore;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HelloController {
    @FXML
    private TextField nameField;

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

        welcomeLabel.setText(
                "✨ Welcome, " + name +
                        "\n\nThe heavens recognize a new astronomer."
        );

        welcomeLabel.setVisible(true);

        beginButton.setVisible(true);

    }

    @FXML
    private void beginJourney() {

        System.out.println("Go to Game Mode Page");

    }
}
