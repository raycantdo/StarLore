package com.starlore.starlore;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShootingStarController {

    @FXML private Canvas gameCanvas;
    @FXML private Label scoreLabel;
    //@FXML private Label livesLabel;
    @FXML private Label timerLabel;   // NEW
    @FXML private Label comboLabel;   // NEW
    @FXML private Button backButton;
    private GraphicsContext gc;
    private int score = 0;
    //private int lives = 3;
    private int combo = 1;            // NEW
    private int maxCombo = 1;         // NEW
    private int timeLeft = 60;        // NEW
    private boolean gameOver = false;
    private List<EnhancedStar> stars = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private AnimationTimer gameTimer;
    private Timeline countdownTimer;
    private String currentFact = "Quickly click the stars before they vanish!";
    private Player currentPlayer;
    // Image Sprites
    private Image normalStarImg;
    private Image rareStarImg;
    private boolean imagesLoaded = false;

    @FXML
    public void initialize() {
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);

        // Load Sprites
        try {
            normalStarImg = new Image(getClass().getResourceAsStream("images/star_normal.png"));
            rareStarImg = new Image(getClass().getResourceAsStream("images/star_rare.png"));
            imagesLoaded = true;
        } catch (Exception e) {
            System.out.println("Sprites not found. Using vector graphics fallback.");
            imagesLoaded = false;
        }

        // 1. Set up Mouse Click Detection
        gameCanvas.setOnMousePressed(this::handleMouseClick);
        startCountdown();
        startGameLoop();
    }
    public void setPlayer(Player player) { // NEW
        this.currentPlayer = player;
    }

    // NEW — Countdown timer from Replit
    private void startCountdown() {
        countdownTimer = new Timeline();
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        KeyFrame kf = new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText("⏱ " + timeLeft + "s");
            if (timeLeft <= 10) {
                timerLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
            }
            if (timeLeft <= 0) {
                gameOver = true;
                countdownTimer.stop();
                if (gameTimer != null) gameTimer.stop();
                showGameOver();
            }
        });
        countdownTimer.getKeyFrames().add(kf);
        countdownTimer.play();
    }
    // 2. Handle the "Make a Wish" Clicks
    private void handleMouseClick(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        boolean hit = false;
        // Check backwards to safely remove from the list
        for (int i = stars.size() - 1; i >= 0; i--) {
            EnhancedStar star = stars.get(i);

            // Calculate distance between mouse click and star center
            double starCenterX = star.getX() + 15; // 15 is half the 30px width
            double starCenterY = star.getY() + 15;
            double distance = Math.hypot(mouseX - starCenterX, mouseY - starCenterY);

            // If clicked within 25 pixels of the star's center
            if (distance < 25) {
                hit = true; // NEW
                int points = (star.isRare() ? 300 : 100) * combo; // MODIFIED — combo multiplier
                score += points;
                combo++;                          // NEW
                if (combo > maxCombo) maxCombo = combo; // NEW
                currentFact = "Wish granted: " + star.getTypeName() + "! +" + points;

                // NEW — Particle explosion
                for (int j = 0; j < 10; j++) {
                    particles.add(new Particle(starCenterX, starCenterY,
                            star.isRare() ? Color.CYAN : Color.YELLOW));
                }

                comboLabel.setText("Combo: x" + combo); // NEW
                stars.remove(i);
                break;
            }
        }

        // NEW — Miss resets combo
        if (!hit) {
            combo = 1;
            comboLabel.setText("Combo: x" + combo);
        }
    }

    private void startGameLoop() {
        gameTimer = new AnimationTimer() {
            long lastSpawn = 0;

            @Override
            public void handle(long now) {
                if (gameOver) return;
                // Spawn a star every ~0.8 seconds
                if (now - lastSpawn > 800_000_000L) {
                    spawnStar();
                    lastSpawn = now;
                }

                updateGame();
                renderGame();

               /* if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop();
                    if (countdownTimer != null) countdownTimer.stop(); // NEW
                    showGameOver();
                }*/
            }
        };
        gameTimer.start();
    }

    private void spawnStar() {
        boolean isRare = random.nextDouble() < 0.15;
        String typeName = isRare ? "Blue Supergiant (+30)" : "Shooting Star (+10)";

        double startX, startY, speedX, speedY;
        double baseSpeed = (isRare ? 4.5 : 2.5) + (score / 150.0);

        int edge = random.nextInt(4);
        if (edge == 0) {
            startX = random.nextInt((int) gameCanvas.getWidth());
            startY = -30;
            speedX = (random.nextDouble() * 4) - 2;
            speedY = baseSpeed;
        } else if (edge == 1) {
            startX = gameCanvas.getWidth() + 30;
            startY = random.nextInt((int) gameCanvas.getHeight());
            speedX = -baseSpeed;
            speedY = (random.nextDouble() * 2) - 1;
        } else if (edge == 2) {
            startX = random.nextInt((int) gameCanvas.getWidth());
            startY = gameCanvas.getHeight() + 30;
            speedX = (random.nextDouble() * 4) - 2;
            speedY = -baseSpeed;
        } else {
            startX = -30;
            startY = random.nextInt((int) gameCanvas.getHeight() / 2);
            speedX = baseSpeed;
            speedY = random.nextDouble() * 2 + 1;
        }

        stars.add(new EnhancedStar(startX, startY, speedX, speedY, typeName, isRare));
    }

    private void updateGame() {
        for (int i = stars.size() - 1; i >= 0; i--) {
            EnhancedStar star = stars.get(i);
            star.update();

            if (star.getX() < -50 || star.getX() > gameCanvas.getWidth() + 50 ||
                    star.getY() > gameCanvas.getHeight() + 50 ||
                    star.getY() < -50) { // NEW — also check top exit
               // lives--;
                combo = 1; // NEW — miss resets combo
                comboLabel.setText("Combo: x" + combo); // NEW
                stars.remove(i);
            }
        }

        // NEW — update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) particles.remove(i);
        }
    }

    private void renderGame() {
        gc.setFill(Color.web("#0b0b19"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        for (EnhancedStar star : stars) {
            if (imagesLoaded && normalStarImg != null && rareStarImg != null) {
                Image imgToDraw = star.isRare() ? rareStarImg : normalStarImg;
                gc.drawImage(imgToDraw, star.getX(), star.getY(), 30, 30);
            } else {
                if (star.isRare()) {
                    gc.setFill(Color.CYAN);
                    gc.fillOval(star.getX(), star.getY(), 16, 16);
                } else {
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(star.getX(), star.getY(), 12, 12);
                }
            }

            gc.setFont(Font.font("Verdana", 10));
            gc.setFill(Color.LIGHTBLUE);
            gc.fillText(star.getTypeName(), star.getX() - 15, star.getY() - 5);
        }

        // NEW — draw particles
        for (Particle p : particles) {
            gc.setGlobalAlpha(p.opacity);
            gc.setFill(p.color);
            gc.fillOval(p.x - 3, p.y - 3, 6, 6);
            gc.setGlobalAlpha(1.0);
        }

        scoreLabel.setText("Score: " + score);
       // livesLabel.setText("Lives: " + lives);

        gc.setFill(Color.web("#ffffff", 0.9));
        gc.setFont(Font.font("Verdana", 13));
        gc.fillText(currentFact, 30, gameCanvas.getHeight() - 15);
    }

    private void showGameOver() {
        gc.setFill(Color.web("#0b0b19", 0.92));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.RED);
        gc.setFont(Font.font("Verdana", 38));
        gc.fillText("MISSION ENDED", 240, 240);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 20));
        gc.fillText("Final Cosmic Score: " + score, 280, 320);
        gc.fillText("Max Combo: x" + maxCombo, 310, 360); // NEW
    }

    // NEW — Back to hub
    @FXML
    private void backToHub() {
        if (gameTimer != null) gameTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            if (currentPlayer != null) controller.setPlayer(currentPlayer);
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Unchanged EnhancedStar class
    private static class EnhancedStar {
        private double x, y, speedX, speedY;
        private String typeName;
        private boolean rare;

        public EnhancedStar(double x, double y, double speedX, double speedY,
                            String typeName, boolean rare) {
            this.x = x; this.y = y;
            this.speedX = speedX; this.speedY = speedY;
            this.typeName = typeName; this.rare = rare;
        }

        public void update() { this.x += speedX; this.y += speedY; }
        public double getX() { return x; }
        public double getY() { return y; }
        public String getTypeName() { return typeName; }
        public boolean isRare() { return rare; }
    }

    // NEW — Particle class from Replit
    private static class Particle {
        double x, y, vx, vy, opacity;
        Color color;
        Random rng = new Random();

        Particle(double x, double y, Color color) {
            this.x = x; this.y = y;
            this.vx = (rng.nextDouble() - 0.5) * 10;
            this.vy = (rng.nextDouble() - 0.5) * 10;
            this.opacity = 1.0;
            this.color = color;
        }

        void update() { x += vx; y += vy; opacity -= 0.04; }
        boolean isDead() { return opacity <= 0; }
    }
}