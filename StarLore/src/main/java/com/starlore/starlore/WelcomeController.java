package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class WelcomeController {

    @FXML private AnchorPane rootPane;
    @FXML private Canvas gatewayCanvas;
    @FXML private VBox menuContainer;
    @FXML private Label welcomeLabel;
    @FXML private Label subLabel;
    @FXML private StackPane portalButtonWrapper;
    @FXML private Button journeyButton;

    private Player currentPlayer;
    private AnimationTimer animationTimer;
    private double timeElapsed = 0;
    private Random random = new Random();

    // Cosmic Gateway Background Stars
    private static final int NUM_STARS = 150;
    private double[] starX = new double[NUM_STARS];
    private double[] starY = new double[NUM_STARS];
    private double[] starR = new double[NUM_STARS];
    private double[] starAlpha = new double[NUM_STARS];

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();

        // Initialize background stars
        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] = random.nextDouble() * 1400;
            starY[i] = random.nextDouble() * 520; // in the sky above the mountains
            starR[i] = random.nextDouble() * 2.2 + 0.8;
            starAlpha[i] = random.nextDouble() * 0.5 + 0.2;
        }

        // Bind canvas dimensions
        if (rootPane != null) {
            gatewayCanvas.widthProperty().bind(rootPane.prefWidthProperty());
            gatewayCanvas.heightProperty().bind(rootPane.prefHeightProperty());
        }

        // Setup Portal Button Hover & Shimmer Animations
        setupPortalButtonFX();

        // Starlight manifestation animation for menu container
        if (menuContainer != null) {
            menuContainer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.seconds(1.8), menuContainer);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis(300));
            ft.play();

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.8), menuContainer);
            tt.setFromX(-30);
            tt.setToX(0);
            tt.setDelay(Duration.millis(300));
            tt.play();
        }

        // Start Cinematic Canvas Rendering Loop
        startGatewayAnimation();
    }

    private void setupPortalButtonFX() {
        if (journeyButton != null) {
            // Ambient breathing pulse for the portal button
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(journeyButton.scaleXProperty(), 1.0),
                            new KeyValue(journeyButton.scaleYProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(1.4),
                            new KeyValue(journeyButton.scaleXProperty(), 1.04),
                            new KeyValue(journeyButton.scaleYProperty(), 1.04)),
                    new KeyFrame(Duration.seconds(2.8),
                            new KeyValue(journeyButton.scaleXProperty(), 1.0),
                            new KeyValue(journeyButton.scaleYProperty(), 1.0))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();

            // Hover illumination
            journeyButton.setOnMouseEntered(e -> {
                journeyButton.setStyle("-fx-background-color: linear-gradient(to right, rgba(56, 189, 248, 0.45), rgba(168, 85, 247, 0.55));" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Verdana';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-letter-spacing: 2.5;" +
                        "-fx-background-radius: 27;" +
                        "-fx-border-color: #7dd3fc;" +
                        "-fx-border-radius: 27;" +
                        "-fx-border-width: 2.2;" +
                        "-fx-padding: 12 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, #38bdf8, 26, 0.85, 0, 0);");
            });

            journeyButton.setOnMouseExited(e -> {
                journeyButton.setStyle("-fx-background-color: linear-gradient(to right, rgba(14, 165, 233, 0.22), rgba(99, 102, 241, 0.35));" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Verdana';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-letter-spacing: 2;" +
                        "-fx-background-radius: 27;" +
                        "-fx-border-color: #38bdf8;" +
                        "-fx-border-radius: 27;" +
                        "-fx-border-width: 1.8;" +
                        "-fx-padding: 12 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, #38bdf8, 16, 0.65, 0, 0);");
            });
        }
    }

    private void startGatewayAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                timeElapsed += 0.016;
                renderGatewayCanvas();
            }
        };
        animationTimer.start();
    }

    private void renderGatewayCanvas() {
        if (gatewayCanvas == null) return;
        GraphicsContext gc = gatewayCanvas.getGraphicsContext2D();
        double w = gatewayCanvas.getWidth();
        double h = gatewayCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        // 1. Sky Gradient
        gc.setFill(Color.web("#030514"));
        gc.fillRect(0, 0, w, h);

        // 2. Cosmic Nebula Cloud Layer
        RadialGradient nebula1 = new RadialGradient(
                0, 0, w * 0.72, h * 0.35, w * 0.45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(99, 102, 241, 0.20)),
                new Stop(0.5, Color.rgb(56, 189, 248, 0.10)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(nebula1);
        gc.fillOval(w * 0.72 - w * 0.45, h * 0.35 - w * 0.45, w * 0.9, w * 0.9);

        // 3. Twinkling Sky Stars
        for (int i = 0; i < NUM_STARS; i++) {
            double alpha = starAlpha[i] + Math.sin(timeElapsed * 2.0 + i) * 0.15;
            alpha = Math.max(0.1, Math.min(0.85, alpha));
            gc.setFill(Color.rgb(224, 242, 254, alpha));
            gc.fillOval(starX[i], starY[i], starR[i], starR[i]);
        }

        // 4. Visual Anchor: Massive Glowing Fractured Moon / Celestial Core in Upper Right
        double moonX = w * 0.72;
        double moonY = h * 0.34;
        double moonR = 125.0;

        // Corona / Astral Bloom
        RadialGradient moonCorona = new RadialGradient(
                0, 0, moonX, moonY, moonR * 2.4, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(186, 230, 253, 0.45)),
                new Stop(0.35, Color.rgb(56, 189, 248, 0.20)),
                new Stop(0.7, Color.rgb(99, 102, 241, 0.08)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(moonCorona);
        gc.fillOval(moonX - moonR * 2.4, moonY - moonR * 2.4, moonR * 4.8, moonR * 4.8);

        // Planetary Ring System (Tilted Ellipse)
        gc.save();
        gc.translate(moonX, moonY);
        gc.rotate(-26.0);
        gc.setLineWidth(5.5);
        gc.setStroke(Color.rgb(186, 230, 253, 0.45));
        gc.strokeOval(-moonR * 1.85, -moonR * 0.42, moonR * 3.7, moonR * 0.84);
        gc.setLineWidth(2.0);
        gc.setStroke(Color.rgb(255, 255, 255, 0.7));
        gc.strokeOval(-moonR * 1.7, -moonR * 0.38, moonR * 3.4, moonR * 0.76);
        gc.restore();

        // Moon Body
        RadialGradient moonSurface = new RadialGradient(
                0, 0, moonX - moonR * 0.3, moonY - moonR * 0.3, moonR * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(255, 255, 255)),
                new Stop(0.35, Color.rgb(224, 242, 254)),
                new Stop(0.75, Color.rgb(125, 211, 252)),
                new Stop(1.0, Color.rgb(14, 116, 144))
        );
        gc.setFill(moonSurface);
        gc.fillOval(moonX - moonR, moonY - moonR, moonR * 2, moonR * 2);

        // Fractured Celestial Cracks / Craters on Moon
        gc.setStroke(Color.rgb(255, 255, 255, 0.5));
        gc.setLineWidth(1.6);
        gc.strokeLine(moonX - 25, moonY - 40, moonX + 10, moonY - 15);
        gc.strokeLine(moonX + 10, moonY - 15, moonX + 45, moonY - 22);
        gc.strokeLine(moonX + 10, moonY - 15, moonX + 5, moonY + 35);

        // 5. Horizon Silhouettes: Jagged Mountain Ridges & Fantasy Observatory Domes
        double horizonY = h * 0.72;

        // Distant Mountain Ridge (Dark Slate/Indigo)
        gc.setFill(Color.web("#070b1e"));
        gc.beginPath();
        gc.moveTo(0, h);
        gc.lineTo(0, horizonY);
        gc.lineTo(w * 0.12, horizonY - 45);
        gc.lineTo(w * 0.25, horizonY + 15);
        gc.lineTo(w * 0.40, horizonY - 60);
        gc.lineTo(w * 0.52, horizonY - 10);
        gc.lineTo(w * 0.65, horizonY - 75);
        gc.lineTo(w * 0.78, horizonY - 30);
        gc.lineTo(w * 0.90, horizonY - 55);
        gc.lineTo(w, horizonY);
        gc.lineTo(w, h);
        gc.closePath();
        gc.fill();

        // Foreground Mountain Ridge (Pitch Silhouette)
        double fgHorizon = h * 0.78;
        gc.setFill(Color.web("#02030a"));
        gc.beginPath();
        gc.moveTo(0, h);
        gc.lineTo(0, fgHorizon);
        gc.lineTo(w * 0.15, fgHorizon - 30);
        gc.lineTo(w * 0.28, fgHorizon - 65);
        gc.lineTo(w * 0.45, fgHorizon - 15);
        gc.lineTo(w * 0.60, fgHorizon - 40);
        gc.lineTo(w * 0.75, fgHorizon - 20);
        gc.lineTo(w * 0.88, fgHorizon - 50);
        gc.lineTo(w, fgHorizon);
        gc.lineTo(w, h);
        gc.closePath();
        gc.fill();

        // Fantasy Observatory Dome & Astrologer Tower Silhouette on the Right
        double towerX = w * 0.80;
        double towerBaseY = fgHorizon - 35;
        double towerW = 60;
        double towerH = 100;

        // Tower walls
        gc.setFill(Color.web("#010206"));
        gc.fillRect(towerX, towerBaseY - towerH, towerW, towerH);

        // Observatory Dome Top
        gc.fillOval(towerX - 6, towerBaseY - towerH - 32, towerW + 12, 60);

        // Giant Observatory Telescope Slit & Lens Beam
        gc.save();
        gc.translate(towerX + towerW / 2.0, towerBaseY - towerH - 6);
        gc.rotate(-42.0);
        // Telescope barrel
        gc.setFill(Color.web("#010206"));
        gc.fillRect(-6, -55, 12, 55);
        // Faint light beam from telescope aiming at the celestial anchor
        RadialGradient scopeBeam = new RadialGradient(
                0, 0, 0, -55, 160, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(186, 230, 253, 0.45)),
                new Stop(0.5, Color.rgb(56, 189, 248, 0.15)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(scopeBeam);
        gc.fillOval(-35, -195, 70, 150);
        gc.restore();

        // Observatory glowing yellow/warm candle windows (like the reference image!)
        gc.setFill(Color.rgb(253, 224, 71, 0.85));
        gc.fillOval(towerX + 16, towerBaseY - 60, 9, 14);
        gc.fillOval(towerX + 35, towerBaseY - 60, 9, 14);
        gc.fillOval(towerX + 25, towerBaseY - 25, 10, 15);
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;

        if (player.isNewPlayer()) {
            welcomeLabel.setText("✨ Welcome, " + player.getUsername() + ".");
            subLabel.setText("The heavens recognize a new astronomer.\nYour path among the constellations begins now.");
            journeyButton.setText("BEGIN JOURNEY  ➔");
        } else {
            welcomeLabel.setText("✨ Welcome back, " + player.getUsername() + ".");
            subLabel.setText("The constellations remember your path.\nContinue your voyage across the astral realm.");
            journeyButton.setText("CONTINUE JOURNEY  ➔");
        }
    }

    @FXML
    private void startJourney() {
        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();

            GameHubController controller = loader.getController();
            controller.setPlayer(currentPlayer);

            root.setOpacity(0);
            Stage stage = (Stage) journeyButton.getScene().getWindow();
            SceneManager.switchScene(stage, root);

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
