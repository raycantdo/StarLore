package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class WelcomeController {

    @FXML private StackPane rootStackPane;
    @FXML private Canvas gatewayCanvas;
    @FXML private VBox menuContainer;
    @FXML private Label welcomeLabel;
    @FXML private Label subLabel;
    @FXML private ProgressBar syncProgressBar;
    @FXML private StackPane portalButtonWrapper;
    @FXML private Button journeyButton;

    private Player currentPlayer;
    private AnimationTimer animationTimer;
    private double timeElapsed = 0;
    private Random random = new Random();

    // ─── Heavy UI Telemetry Data ───────────────────────────
    private static final int NUM_NODES = 120;
    private double[] nodeX = new double[NUM_NODES];
    private double[] nodeY = new double[NUM_NODES];
    private double[] nodeVX = new double[NUM_NODES];
    private double[] nodeVY = new double[NUM_NODES];

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();

        // Initialize floating data nodes
        for (int i = 0; i < NUM_NODES; i++) {
            nodeX[i] = random.nextDouble() * 2560;
            nodeY[i] = random.nextDouble() * 1440;
            nodeVX[i] = (random.nextDouble() - 0.5) * 0.8;
            nodeVY[i] = (random.nextDouble() - 0.5) * 0.8;
        }

        if (rootStackPane != null) {
            gatewayCanvas.widthProperty().bind(rootStackPane.widthProperty());
            gatewayCanvas.heightProperty().bind(rootStackPane.heightProperty());
        }

        setupPortalButtonFX();

        if (menuContainer != null) {
            menuContainer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.seconds(1.2), menuContainer);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis(200));
            ft.play();

            ScaleTransition st = new ScaleTransition(Duration.seconds(1.2), menuContainer);
            st.setFromX(0.95);
            st.setFromY(0.95);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setDelay(Duration.millis(200));
            st.play();
        }

        if (syncProgressBar != null) {
            syncProgressBar.setProgress(0);
            Timeline progressAnim = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(syncProgressBar.progressProperty(), 0)),
                    new KeyFrame(Duration.seconds(2.0), new KeyValue(syncProgressBar.progressProperty(), 1.0, Interpolator.EASE_OUT))
            );
            progressAnim.setDelay(Duration.millis(500));
            progressAnim.play();
        }

        startGatewayAnimation();
    }

    private void setupPortalButtonFX() {
        if (journeyButton != null) {
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(journeyButton.scaleXProperty(), 1.0),
                            new KeyValue(journeyButton.scaleYProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(1.4),
                            new KeyValue(journeyButton.scaleXProperty(), 1.03),
                            new KeyValue(journeyButton.scaleYProperty(), 1.03)),
                    new KeyFrame(Duration.seconds(2.8),
                            new KeyValue(journeyButton.scaleXProperty(), 1.0),
                            new KeyValue(journeyButton.scaleYProperty(), 1.0))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();

            journeyButton.setOnMouseEntered(e -> {
                journeyButton.setStyle("-fx-background-color: linear-gradient(to right, rgba(56, 189, 248, 0.55), rgba(168, 85, 247, 0.65));" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Impact', 'Arial Black', sans-serif;" +
                        "-fx-font-size: 26px;" +
                        "-fx-letter-spacing: 4.5;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #7dd3fc;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 3.0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, #38bdf8, 35, 0.7, 0, 0);");
            });

            journeyButton.setOnMouseExited(e -> {
                journeyButton.setStyle("-fx-background-color: linear-gradient(to right, rgba(14, 165, 233, 0.35), rgba(99, 102, 241, 0.5));" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Impact', 'Arial Black', sans-serif;" +
                        "-fx-font-size: 26px;" +
                        "-fx-letter-spacing: 4;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #38bdf8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 2.5;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, #38bdf8, 25, 0.5, 0, 0);");
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

        // 1. Tech Void Background
        gc.setFill(Color.web("#020308"));
        gc.fillRect(0, 0, w, h);

        // 2. Digital Grid
        gc.setStroke(Color.rgb(56, 189, 248, 0.04));
        gc.setLineWidth(1);
        for(int x = 0; x < w; x += 50) gc.strokeLine(x, 0, x, h);
        for(int y = 0; y < h; y += 50) gc.strokeLine(0, y, w, y);

        // 3. Ambient Holographic Core Glow
        RadialGradient coreGlow = new RadialGradient(
                0, 0, w / 2, h / 2, w * 0.6, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 165, 233, 0.15)),
                new Stop(0.5, Color.rgb(99, 102, 241, 0.05)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(coreGlow);
        gc.fillRect(0, 0, w, h);

        // 4. Floating Telemetry Nodes (Constellation Network)
        gc.setLineWidth(1.0);
        for (int i = 0; i < NUM_NODES; i++) {
            nodeX[i] += nodeVX[i];
            nodeY[i] += nodeVY[i];

            if (nodeX[i] < 0 || nodeX[i] > w) nodeVX[i] *= -1;
            if (nodeY[i] < 0 || nodeY[i] > h) nodeVY[i] *= -1;

            // Connect nearby nodes
            for (int j = i + 1; j < NUM_NODES; j++) {
                double dist = Math.hypot(nodeX[i] - nodeX[j], nodeY[i] - nodeY[j]);
                if (dist < 120) {
                    double alpha = (1.0 - (dist / 120.0)) * 0.4;
                    gc.setStroke(Color.rgb(56, 189, 248, alpha));
                    gc.strokeLine(nodeX[i], nodeY[i], nodeX[j], nodeY[j]);
                }
            }

            // Draw Node
            gc.setFill(Color.rgb(186, 230, 253, 0.8));
            gc.fillOval(nodeX[i] - 2, nodeY[i] - 2, 4, 4);
        }

        // 5. Massive Rotating HUD Astrolabe / Radar
        gc.save();
        gc.translate(w / 2, h / 2);

        // Outer Dashed Ring
        gc.rotate(timeElapsed * 8);
        gc.setStroke(Color.rgb(168, 85, 247, 0.15));
        gc.setLineWidth(2);
        gc.setLineDashes(15, 25);
        gc.strokeOval(-450, -450, 900, 900);

        // Mid Solid Ring
        gc.rotate(-timeElapsed * 12);
        gc.setLineDashes(null);
        gc.setStroke(Color.rgb(56, 189, 248, 0.25));
        gc.setLineWidth(1.5);
        gc.strokeOval(-380, -380, 760, 760);

        // Inner Tech Ticks
        for(int i = 0; i < 72; i++) {
            gc.rotate(5);
            gc.setStroke(Color.rgb(255, 255, 255, 0.2));
            gc.setLineWidth(1);
            gc.strokeLine(380, 0, i % 2 == 0 ? 395 : 385, 0);
        }

        // Sweeping Radar Scanner
        gc.rotate(timeElapsed * 40);
        LinearGradient scanner = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(56, 189, 248, 0.0)),
                new Stop(1, Color.rgb(56, 189, 248, 0.3)));
        gc.setFill(scanner);
        gc.fillArc(-380, -380, 760, 760, 0, 45, javafx.scene.shape.ArcType.ROUND);

        gc.restore();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;

        // Null check to prevent returning player crash
        String rank = player.getRankTitle();
        if (rank == null || rank.trim().isEmpty()) {
            rank = "Stargazer";
        }

        if (player.isNewPlayer()) {
            welcomeLabel.setText("COMMANDER " + player.getUsername().toUpperCase());
            subLabel.setText("ASTRAL TELEMETRY SYNCED. YOUR PATH AMONG THE CONSTELLATIONS IS READY.");
            journeyButton.setText("INITIATE LINK");
        } else {
            welcomeLabel.setText("COMMANDER " + player.getUsername().toUpperCase());
            subLabel.setText("CONSTELLATION ARCHIVES RESTORED. READY TO RESUME ASTRAL OPERATIONS.");
            journeyButton.setText("RESUME LINK");
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

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.2), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}