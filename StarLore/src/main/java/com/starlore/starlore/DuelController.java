package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuelController {

    @FXML private Canvas playerCanvas;
    @FXML private Canvas aiCanvas;
    @FXML private Label playerScoreLabel;
    @FXML private Label aiScoreLabel;
    @FXML private Label timerLabel;
    @FXML private Label countdownLabel;
    @FXML private Label constellationNameLabel;
    @FXML private Label constellationRevealLabel;
    @FXML private Label resultLabel;
    @FXML private Label resultScoreLabel;
    @FXML private StackPane countdownOverlay;
    @FXML private StackPane resultOverlay;

    private Player currentPlayer;
    private Random random = new Random();

    // Game State
    private int playerScore = 0;
    private int aiScore = 0;
    private int timeLeft = 30;
    private boolean gameActive = false;

    // Constellation Data
    private List<double[]> stars = new ArrayList<>();
    private List<int[]> connections = new ArrayList<>();
    private List<Integer> playerConnected = new ArrayList<>();
    private List<Integer> aiConnected = new ArrayList<>();
    private int lastPlayerStar = -1;
    private int lastAiStar = -1;
    private String currentConstellationName = "";

    // Timelines
    private Timeline gameTimer;
    private Timeline aiTimeline;

    // Constellations Database
    private String[][] constellationData = {
            {"ORION"},
            {"CASSIOPEIA"},
            {"URSA MAJOR"},
            {"SCORPIUS"},
            {"LEO"}
    };

    // ─── Initialization ───────────────────────────────────────

    @FXML
    public void initialize() {
        playerCanvas.setOnMouseClicked(this::handlePlayerClick);
        startCountdown();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
    }

    // ─── Countdown ────────────────────────────────────────────

    private void startCountdown() {
        generateConstellation();
        constellationRevealLabel.setText("Connect: " + currentConstellationName);

        int[] count = {3};
        countdownLabel.setText(String.valueOf(count[0]));

        // Animate countdown number
        animateCountdownNumber();

        Timeline countdown = new Timeline();
        countdown.setCycleCount(3);
        KeyFrame kf = new KeyFrame(Duration.seconds(1), e -> {
            count[0]--;
            if (count[0] > 0) {
                countdownLabel.setText(String.valueOf(count[0]));
                animateCountdownNumber();
            } else {
                countdownLabel.setText("GO!");
                countdownLabel.setStyle(
                        "-fx-font-size: 100; -fx-font-weight: bold; -fx-text-fill: #00ff88;");
            }
        });
        countdown.getKeyFrames().add(kf);
        countdown.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.millis(600));
            pause.setOnFinished(ev -> {
                countdownOverlay.setVisible(false);
                constellationNameLabel.setText("🌟 " + currentConstellationName);
                startGame();
            });
            pause.play();
        });
        countdown.play();
    }

    private void animateCountdownNumber() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(800), countdownLabel);
        scale.setFromX(1.5);
        scale.setFromY(1.5);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    // ─── Constellation Generation ─────────────────────────────

    private void generateConstellation() {
        stars.clear();
        connections.clear();
        playerConnected.clear();
        aiConnected.clear();
        lastPlayerStar = -1;
        lastAiStar = -1;

        // Pick random constellation
        int index = random.nextInt(constellationData.length);
        currentConstellationName = constellationData[index][0];

        // Generate stars based on constellation
        switch (currentConstellationName) {
            case "ORION":
                // Orion — 7 main stars
                stars.add(new double[]{245, 80});   // 0 — Betelgeuse
                stars.add(new double[]{245, 160});  // 1 — Bellatrix area
                stars.add(new double[]{200, 220});  // 2 — Belt left
                stars.add(new double[]{245, 230});  // 3 — Belt center
                stars.add(new double[]{290, 220});  // 4 — Belt right
                stars.add(new double[]{200, 320});  // 5 — Rigel area
                stars.add(new double[]{290, 320});  // 6 — Saiph area
                connections.add(new int[]{0, 1});
                connections.add(new int[]{1, 2});
                connections.add(new int[]{2, 3});
                connections.add(new int[]{3, 4});
                connections.add(new int[]{4, 1});
                connections.add(new int[]{2, 5});
                connections.add(new int[]{4, 6});
                break;

            case "CASSIOPEIA":
                // W shape — 5 stars
                stars.add(new double[]{160, 200}); // 0
                stars.add(new double[]{205, 150}); // 1
                stars.add(new double[]{245, 200}); // 2
                stars.add(new double[]{285, 150}); // 3
                stars.add(new double[]{330, 200}); // 4
                connections.add(new int[]{0, 1});
                connections.add(new int[]{1, 2});
                connections.add(new int[]{2, 3});
                connections.add(new int[]{3, 4});
                break;

            case "URSA MAJOR":
                // Big Dipper — 7 stars
                stars.add(new double[]{150, 180}); // 0
                stars.add(new double[]{200, 160}); // 1
                stars.add(new double[]{250, 170}); // 2
                stars.add(new double[]{300, 180}); // 3
                stars.add(new double[]{320, 240}); // 4
                stars.add(new double[]{270, 260}); // 5
                stars.add(new double[]{220, 250}); // 6
                connections.add(new int[]{0, 1});
                connections.add(new int[]{1, 2});
                connections.add(new int[]{2, 3});
                connections.add(new int[]{3, 4});
                connections.add(new int[]{4, 5});
                connections.add(new int[]{5, 6});
                connections.add(new int[]{6, 3});
                break;

            case "SCORPIUS":
                // Scorpion — 8 stars
                stars.add(new double[]{245, 80});  // 0 — head
                stars.add(new double[]{245, 140}); // 1
                stars.add(new double[]{230, 190}); // 2
                stars.add(new double[]{245, 240}); // 3 — Antares
                stars.add(new double[]{260, 290}); // 4
                stars.add(new double[]{245, 340}); // 5
                stars.add(new double[]{220, 380}); // 6
                stars.add(new double[]{200, 420}); // 7 — tail
                connections.add(new int[]{0, 1});
                connections.add(new int[]{1, 2});
                connections.add(new int[]{2, 3});
                connections.add(new int[]{3, 4});
                connections.add(new int[]{4, 5});
                connections.add(new int[]{5, 6});
                connections.add(new int[]{6, 7});
                break;

            default: // LEO
                // Lion — 6 stars
                stars.add(new double[]{180, 150}); // 0
                stars.add(new double[]{220, 120}); // 1
                stars.add(new double[]{270, 130}); // 2
                stars.add(new double[]{300, 180}); // 3
                stars.add(new double[]{260, 250}); // 4
                stars.add(new double[]{180, 260}); // 5
                connections.add(new int[]{0, 1});
                connections.add(new int[]{1, 2});
                connections.add(new int[]{2, 3});
                connections.add(new int[]{3, 4});
                connections.add(new int[]{4, 5});
                connections.add(new int[]{5, 0});
                break;
        }

        drawStarMap(playerCanvas, playerConnected, lastPlayerStar, Color.DODGERBLUE);
        drawStarMap(aiCanvas, aiConnected, lastAiStar, Color.TOMATO);
    }

    // ─── Star Map Drawing ─────────────────────────────────────

    private void drawStarMap(Canvas canvas, List<Integer> connected,
                             int lastStar, Color lineColor) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Background
        gc.setFill(Color.rgb(5, 8, 22, 0.95));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw ambient background stars
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        for (int i = 0; i < 40; i++) {
            double x = random.nextDouble() * canvas.getWidth();
            double y = random.nextDouble() * canvas.getHeight();
            gc.fillOval(x, y, 1.5, 1.5);
        }

        // Draw completed connections
        gc.setStroke(lineColor);
        gc.setLineWidth(2);
        gc.setGlobalAlpha(0.8);
        for (int i = 0; i < connected.size() - 1; i++) {
            int from = connected.get(i);
            int to = connected.get(i + 1);
            // Check if this is a valid connection
            for (int[] conn : connections) {
                if ((conn[0] == from && conn[1] == to) ||
                        (conn[0] == to && conn[1] == from)) {
                    gc.strokeLine(
                            stars.get(from)[0], stars.get(from)[1],
                            stars.get(to)[0], stars.get(to)[1]);
                }
            }
        }
        gc.setGlobalAlpha(1.0);

        // Draw stars
        for (int i = 0; i < stars.size(); i++) {
            double x = stars.get(i)[0];
            double y = stars.get(i)[1];

            boolean isConnected = connected.contains(i);
            boolean isLast = (i == lastStar);

            // Glow effect
            if (isLast) {
                gc.setFill(Color.rgb(255, 255, 100, 0.3));
                gc.fillOval(x - 15, y - 15, 30, 30);
            }

            // Star body
            if (isConnected) {
                gc.setFill(lineColor);
            } else {
                gc.setFill(Color.WHITE);
            }
            gc.fillOval(x - 7, y - 7, 14, 14);

            // Star number
            gc.setFill(Color.rgb(0, 0, 0));
            gc.fillText(String.valueOf(i + 1), x - 4, y + 4);
        }
    }

    // ─── Game Start ───────────────────────────────────────────

    private void startGame() {
        gameActive = true;
        timeLeft = 30;
        startTimer();
        startAI();
    }

    // ─── Timer ────────────────────────────────────────────────

    private void startTimer() {
        gameTimer = new Timeline();
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        KeyFrame kf = new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            timerLabel.setText(String.format("⏱ %d:%02d", minutes, seconds));

            // Timer turns red when low
            if (timeLeft <= 10) {
                timerLabel.setStyle(
                        "-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #ff4444;");
            }

            if (timeLeft <= 0) {
                endGame();
            }
        });
        gameTimer.getKeyFrames().add(kf);
        gameTimer.play();
    }

    // ─── Player Click ─────────────────────────────────────────

    private void handlePlayerClick(MouseEvent event) {
        if (!gameActive) return;

        double clickX = event.getX();
        double clickY = event.getY();

        // Find nearest star within 30px
        for (int i = 0; i < stars.size(); i++) {
            double sx = stars.get(i)[0];
            double sy = stars.get(i)[1];
            double dist = Math.sqrt(Math.pow(clickX - sx, 2) + Math.pow(clickY - sy, 2));

            if (dist <= 30) {
                handlePlayerStarClick(i);
                break;
            }
        }
    }

    private void handlePlayerStarClick(int starIndex) {
        if (lastPlayerStar == -1) {
            // First star selected
            lastPlayerStar = starIndex;
            playerConnected.add(starIndex);
        } else {
            // Check if valid connection
            boolean valid = false;
            for (int[] conn : connections) {
                if ((conn[0] == lastPlayerStar && conn[1] == starIndex) ||
                        (conn[0] == starIndex && conn[1] == lastPlayerStar)) {
                    valid = true;
                    break;
                }
            }

            if (valid && !playerConnected.contains(starIndex)) {
                // Correct connection
                playerConnected.add(starIndex);
                lastPlayerStar = starIndex;
                playerScore += 10;
                playerScoreLabel.setText("Score: " + playerScore);
                playCorrectEffect(playerCanvas);

                // Check if player completed constellation
                if (playerConnected.size() >= stars.size()) {
                    endGame();
                }
            } else {
                // Wrong connection — penalty
                playerScore = Math.max(0, playerScore - 5);
                playerScoreLabel.setText("Score: " + playerScore);
                playWrongEffect(playerCanvas);
                lastPlayerStar = -1;
                playerConnected.clear();
            }
        }
        drawStarMap(playerCanvas, playerConnected, lastPlayerStar, Color.DODGERBLUE);
    }

    // ─── AI Logic ─────────────────────────────────────────────

    private void startAI() {
        // AI connects one star every 2 seconds
        aiTimeline = new Timeline();
        aiTimeline.setCycleCount(Timeline.INDEFINITE);
        KeyFrame kf = new KeyFrame(Duration.seconds(2), e -> {
            if (!gameActive) return;
            makeAiMove();
        });
        aiTimeline.getKeyFrames().add(kf);
        aiTimeline.play();
    }

    private void makeAiMove() {
        if (aiConnected.isEmpty()) {
            // Start from first star
            aiConnected.add(0);
            lastAiStar = 0;
        } else {
            // Find next unconnected star
            for (int[] conn : connections) {
                int next = -1;
                if (conn[0] == lastAiStar && !aiConnected.contains(conn[1])) {
                    next = conn[1];
                } else if (conn[1] == lastAiStar && !aiConnected.contains(conn[0])) {
                    next = conn[0];
                }

                if (next != -1) {
                    aiConnected.add(next);
                    lastAiStar = next;
                    aiScore += 10;
                    aiScoreLabel.setText("Score: " + aiScore);

                    if (aiConnected.size() >= stars.size()) {
                        endGame();
                    }
                    break;
                }
            }
        }
        drawStarMap(aiCanvas, aiConnected, lastAiStar, Color.TOMATO);
    }

    // ─── Effects ──────────────────────────────────────────────

    private void playCorrectEffect(Canvas canvas) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), canvas);
        scale.setFromX(1.02);
        scale.setFromY(1.02);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    private void playWrongEffect(Canvas canvas) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(80), canvas);
        shake.setFromX(-8);
        shake.setToX(8);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }

    // ─── End Game ─────────────────────────────────────────────

    private void endGame() {
        if (!gameActive) return;
        gameActive = false;

        if (gameTimer != null) gameTimer.stop();
        if (aiTimeline != null) aiTimeline.stop();

        // Show result after short pause
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> showResult());
        pause.play();
    }

    private void showResult() {
        resultOverlay.setVisible(true);
        resultScoreLabel.setText(
                "Your Score: " + playerScore + "  —  AI Score: " + aiScore);

        if (playerScore > aiScore) {
            resultLabel.setText("⚔️ YOU WIN!");
            resultLabel.setStyle(
                    "-fx-font-size: 52; -fx-font-weight: bold; -fx-text-fill: #00ff88;");
        } else if (aiScore > playerScore) {
            resultLabel.setText("🤖 AI WINS!");
            resultLabel.setStyle(
                    "-fx-font-size: 52; -fx-font-weight: bold; -fx-text-fill: #ff4444;");
        } else {
            resultLabel.setText("⚔️ IT'S A DRAW!");
            resultLabel.setStyle(
                    "-fx-font-size: 52; -fx-font-weight: bold; -fx-text-fill: #f0d060;");
        }

        // Animate result
        FadeTransition fade = new FadeTransition(Duration.millis(600), resultOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    // ─── Buttons ──────────────────────────────────────────────

    @FXML
    private void rematch() {
        // Reset everything
        playerScore = 0;
        aiScore = 0;
        timeLeft = 30;
        playerScoreLabel.setText("Score: 0");
        aiScoreLabel.setText("Score: 0");
        timerLabel.setStyle(
                "-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: white;");
        timerLabel.setText("⏱ 0:30");
        resultOverlay.setVisible(false);
        countdownOverlay.setVisible(true);
        countdownLabel.setStyle(
                "-fx-font-size: 120; -fx-font-weight: bold; -fx-text-fill: white;");
        startCountdown();
    }

    @FXML
    private void backToHub() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();

            GameHubController controller = loader.getController();
            if (currentPlayer != null) controller.setPlayer(currentPlayer);

            root.setOpacity(0);
            Stage stage = (Stage) playerCanvas.getScene().getWindow();
            stage.setScene(new Scene(root));

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}