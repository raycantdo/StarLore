package com.starlore.starlore;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.layout.AnchorPane;

public class ShootingStarController {

    @FXML private AnchorPane rootPane;
    @FXML private Canvas gameCanvas;

    @FXML private Label scoreLabel;
    @FXML private Label bestLabel;
    @FXML private Label timerLabel;
    @FXML private Label comboLabel;
    @FXML private Label levelLabel;
    @FXML private ProgressBar levelProgressBar;
    @FXML private Circle life1;
    @FXML private Circle life2;
    @FXML private Circle life3;
    @FXML private Button backButton;
    @FXML private Label factLabel;
    private AudioClip catchSound;
    private AudioClip bonusSound;
    private MediaPlayer gameMusicPlayer;

    private GraphicsContext gc;

    // ─── Core state ─────────────────────────────────────────────
    private int score = 0;
    private int best = 0;
    private int lives = 3;
    private static final int MAX_LIVES = 3;
    private int combo = 1;
    private int maxCombo = 1;
    private int timeLeft = 60;
    private int level = 1;
    private boolean gameOver = false;

    private static final int[] LEVEL_THRESHOLDS = {0, 300, 700, 1200, 1800};

    private final List<FallingObject> objects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<BgStar> bgStars = new ArrayList<>();
    private final Random random = new Random();

    private AnimationTimer gameTimer;
    private Timeline countdownTimer;
    private String currentFact = "Click the falling objects — avoid the red ones!";
    private Player currentPlayer;
    private double elapsedSeconds = 0;

    @FXML
    public void initialize() {
        gc = gameCanvas.getGraphicsContext2D();
        SoundManager.stopMenuMusic();
        loadSounds();
        gameMusicPlayer.play();
        gameCanvas.setFocusTraversable(true);

        if (rootPane != null) {
            gameCanvas.widthProperty().bind(rootPane.widthProperty());
            gameCanvas.heightProperty().bind(rootPane.heightProperty());
        }

        // Re-seed the background stars whenever the real size becomes known
        gameCanvas.widthProperty().addListener((obs, oldV, newV) -> seedBackgroundStars());
        gameCanvas.heightProperty().addListener((obs, oldV, newV) -> seedBackgroundStars());
        seedBackgroundStars();

        best = (currentPlayer != null) ? currentPlayer.getHighestArcadeScore() : 0;
        bestLabel.setText(String.valueOf(best));

        gameCanvas.setOnMousePressed(this::handleMouseClick);
        setFact(currentFact);
        startCountdown();
        startGameLoop();
    }
    private void loadSounds() {
        catchSound = new AudioClip(getClass().getResource("sounds/catch.wav").toExternalForm());
        bonusSound = new AudioClip(getClass().getResource("sounds/bonus.wav").toExternalForm());

        Media gameMedia = new Media(getClass().getResource("sounds/game_theme.wav").toExternalForm());
        gameMusicPlayer = new MediaPlayer(gameMedia);
        gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        gameMusicPlayer.setVolume(0.4);
    }

    private void seedBackgroundStars() {
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        bgStars.clear();
        for (int i = 0; i < 90; i++) {
            bgStars.add(new BgStar(
                    random.nextDouble() * w,
                    random.nextDouble() * h,
                    random.nextDouble() * 1.6 + 0.4,
                    random.nextDouble() * Math.PI * 2));
        }
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
        if (bestLabel != null) {
            best = (currentPlayer != null) ? currentPlayer.getHighestArcadeScore() : 0;
            bestLabel.setText(String.valueOf(best));
        }
    }

    // ─── Countdown ──────────────────────────────────────────────
    private void startCountdown() {
        countdownTimer = new Timeline();
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        KeyFrame kf = new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText(timeLeft + "s");
            if (timeLeft <= 10) {
                timerLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
            }
            if (timeLeft <= 0) {
                triggerGameOver();
            }
        });
        countdownTimer.getKeyFrames().add(kf);
        countdownTimer.play();
    }

    // ─── Mouse handling ─────────────────────────────────────────
    private void handleMouseClick(MouseEvent event) {
        if (gameOver) return;
        double mouseX = event.getX();
        double mouseY = event.getY();
        boolean hit = false;

        for (int i = objects.size() - 1; i >= 0; i--) {
            FallingObject obj = objects.get(i);
            CelestialType type = obj.getType();
            double cx = obj.getX() + type.baseRadius;
            double cy = obj.getY() + type.baseRadius;
            double distance = Math.hypot(mouseX - cx, mouseY - cy);

            if (distance < type.baseRadius + 12) {
                hit = true;
                resolveHit(type, cx, cy);
                objects.remove(i);
                break;
            }
        }

        // NEW — Miss resets combo
        if (!hit) {
            combo = 1;
            comboLabel.setText("x" + combo);
        }
    }

    private void setFact(String fact) {
        this.currentFact = fact;
        if (factLabel != null) {
            factLabel.setText(fact);
        }
    }

    private void resolveHit(CelestialType type, double cx, double cy) {
        if (type.isHazard()) {
            score = Math.max(0, score + type.points);
            lives = Math.max(0, Math.min(MAX_LIVES, lives + type.livesDelta));
            updateLifePips();
            combo = 1;
            setFact(type.label + "! " + type.effectLabel);
            spawnParticles(cx, cy, type.primaryColor, 12);
            spawnFloatingText(cx, cy, type.effectLabel, Color.web("#ff6b6b"));
        } else {
            catchSound.play();
            int gained = type.points * combo;
            score += gained;
            combo++;
            if (combo > maxCombo) maxCombo = combo;
            setFact("Caught " + type.label + "! +" + gained);
            spawnParticles(cx, cy, type.primaryColor, 10);
            spawnFloatingText(cx, cy, "+" + gained, Color.web("#7cffb2"));

            if (combo >= 5 && combo % 5 == 0) {
                bonusSound.play();
                score += 50;
                spawnFloatingText(cx, cy - 24, "COMBO x" + combo + "!  +50 BONUS", Color.web("#ffd166"));
                spawnParticles(cx, cy, Color.web("#ffd166"), 18);
            }
        }

        comboLabel.setText("x" + combo);
        if (score > best) {
            best = score;
            bestLabel.setText(String.valueOf(best));
            if (currentPlayer != null) currentPlayer.setHighestArcadeScore(best);
        }
        updateLevel();

        if (lives <= 0) {
            triggerGameOver();
        }
    }

    private void updateLifePips() {
        Circle[] pips = {life1, life2, life3};
        for (int i = 0; i < pips.length; i++) {
            boolean alive = i < lives;
            Circle pip = pips[i];
            if (!alive && pip.getOpacity() > 0.3) {
                FadeTransition ft = new FadeTransition(Duration.millis(300), pip);
                ft.setFromValue(1.0);
                ft.setToValue(0.25);
                ft.play();
                ScaleTransition st = new ScaleTransition(Duration.millis(300), pip);
                st.setFromX(1.0);
                st.setFromY(1.0);
                st.setToX(0.55);
                st.setToY(0.55);
                st.play();
                pip.setFill(Color.web("#2b2f45"));
            } else if (alive) {
                pip.setOpacity(1.0);
                pip.setScaleX(1.0);
                pip.setScaleY(1.0);
                pip.setFill(Color.web("#ff5d73"));
            }
        }
    }

    // ─── Level progression ──────────────────────────────────────
    private void updateLevel() {
        int newLevel = 1;
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= LEVEL_THRESHOLDS[i]) { newLevel = i + 1; break; }
        }
        level = Math.min(newLevel, 5);

        int idx = level - 1;
        int currentThreshold = LEVEL_THRESHOLDS[idx];
        int nextThreshold = (level < 5) ? LEVEL_THRESHOLDS[level] : currentThreshold + 800;
        double progress = (double) (score - currentThreshold) / (nextThreshold - currentThreshold);
        levelProgressBar.setProgress(Math.max(0, Math.min(1, progress)));
        levelLabel.setText("LVL " + level);
    }

    // ─── Spawn table per level ──────────────────────────────────
    private List<CelestialType> buildPool(int lvl) {
        List<CelestialType> pool = new ArrayList<>();
        switch (lvl) {
            case 1:
                addN(pool, CelestialType.NORMAL_STAR, 7);
                addN(pool, CelestialType.STARDUST, 3);
                break;
            case 2:
                addN(pool, CelestialType.NORMAL_STAR, 4);
                addN(pool, CelestialType.GOLDEN_STAR, 3);
                addN(pool, CelestialType.COSMIC_STAR, 1);
                addN(pool, CelestialType.STARDUST, 2);
                break;
            case 3:
                addN(pool, CelestialType.NORMAL_STAR, 3);
                addN(pool, CelestialType.GOLDEN_STAR, 2);
                addN(pool, CelestialType.COMET, 2);
                addN(pool, CelestialType.METEOR, 2);
                addN(pool, CelestialType.STARDUST, 1);
                break;
            case 4:
                addN(pool, CelestialType.PLANET, 2);
                addN(pool, CelestialType.NORMAL_STAR, 2);
                addN(pool, CelestialType.GOLDEN_STAR, 1);
                addN(pool, CelestialType.COMET, 2);
                addN(pool, CelestialType.METEOR, 2);
                addN(pool, CelestialType.BLACK_HOLE, 1);
                break;
            default:
                addN(pool, CelestialType.GOLDEN_STAR, 2);
                addN(pool, CelestialType.COMET, 2);
                addN(pool, CelestialType.NORMAL_STAR, 1);
                addN(pool, CelestialType.BLACK_HOLE, 2);
                addN(pool, CelestialType.COSMIC_STAR, 1);
                addN(pool, CelestialType.METEOR, 2);
                addN(pool, CelestialType.DARK_MATTER, 1);
        }
        return pool;
    }

    private void addN(List<CelestialType> pool, CelestialType type, int n) {
        for (int i = 0; i < n; i++) pool.add(type);
    }

    // ─── Game loop ──────────────────────────────────────────────
    private void startGameLoop() {
        gameTimer = new AnimationTimer() {
            long lastSpawn = 0;
            long start = -1;

            @Override
            public void handle(long now) {
                if (gameOver) return;
                if (start < 0) start = now;
                elapsedSeconds = (now - start) / 1_000_000_000.0;

                long spawnIntervalNanos = (long) Math.max(340, 800 - (level - 1) * 90) * 1_000_000L;
                if (now - lastSpawn > spawnIntervalNanos) {
                    spawnObject();
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

    private void spawnObject() {
        List<CelestialType> pool = buildPool(level);
        CelestialType type = pool.get(random.nextInt(pool.size()));

        double levelSpeedBoost = 1.0 + (level - 1) * 0.18;
        double baseSpeed = 2.1 * levelSpeedBoost * type.speedFactor + (score / 600.0);

        double w = Math.max(800, gameCanvas.getWidth());
        double h = Math.max(600, gameCanvas.getHeight());

        double startX, startY, speedX, speedY;
        int edge = random.nextInt(4);
        if (edge == 0) { // Spawning off-screen above top
            startX = random.nextDouble() * w;
            startY = -40;
            speedX = (random.nextDouble() * 4) - 2;
            speedY = baseSpeed;
        } else if (edge == 1) { // Spawning off-screen on the right
            startX = w + 40;
            startY = random.nextDouble() * h;
            speedX = -baseSpeed;
            speedY = (random.nextDouble() * 2) - 1;
        } else if (edge == 2) { // Spawning off-screen below bottom
            startX = random.nextDouble() * w;
            startY = h + 40;
            speedX = (random.nextDouble() * 4) - 2;
            speedY = -baseSpeed;
        } else { // Spawning off-screen on the left
            startX = -40;
            startY = random.nextDouble() * h;
            speedX = baseSpeed;
            speedY = (random.nextDouble() * 2) - 1;
        }

        objects.add(new FallingObject(startX, startY, speedX, speedY, type, random.nextDouble() * 1000));
    }

    private void updateGame() {
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();

        for (int i = objects.size() - 1; i >= 0; i--) {
            FallingObject obj = objects.get(i);
            obj.update();

            // Only vanish once the star has fully exited the screen at the outer edges
            if (obj.getX() < -60 || obj.getX() > w + 60 ||
                    obj.getY() < -60 || obj.getY() > h + 60) {
                if (!obj.getType().isHazard()) {
                    combo = 1;
                    comboLabel.setText("x" + combo);
                }
                objects.remove(i);
            }
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) particles.remove(i);
        }

        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update();
            if (ft.isDead()) floatingTexts.remove(i);
        }
    }

    private void spawnParticles(double x, double y, Color color, int count) {
        for (int j = 0; j < count; j++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void spawnFloatingText(double x, double y, String text, Color color) {
        floatingTexts.add(new FloatingText(x, y, text, color));
    }

    // ─── Rendering ──────────────────────────────────────────────
    private void renderGame() {
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();

        LinearGradient bg = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#020308")),
                new Stop(1, Color.web("#050c20")));
        gc.setFill(bg);
        gc.fillRect(0, 0, w, h);

        // Soft celestial nebula clouds
        RadialGradient nebulaPurple = new RadialGradient(
                0, 0, w * 0.85, h * 0.25, w * 0.45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(112, 26, 230, 0.08)),
                new Stop(0.6, Color.TRANSPARENT)
        );
        gc.setFill(nebulaPurple);
        gc.fillRect(0, 0, w, h);

        RadialGradient nebulaCyan = new RadialGradient(
                0, 0, w * 0.15, h * 0.75, w * 0.45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 165, 233, 0.07)),
                new Stop(0.6, Color.TRANSPARENT)
        );
        gc.setFill(nebulaCyan);
        gc.fillRect(0, 0, w, h);

        gc.setTextAlign(TextAlignment.LEFT);

        gc.setStroke(Color.web("#ffffff", 0.03));
        gc.setLineWidth(1);
        for (int x = 0; x < w; x += 45) gc.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 45) gc.strokeLine(0, y, w, y);

        for (BgStar star : bgStars) {
            double twinkle = 0.35 + 0.35 * Math.sin(elapsedSeconds * 1.5 + star.phase);
            gc.setGlobalAlpha(Math.max(0, twinkle));
            gc.setFill(Color.WHITE);
            gc.fillOval(star.x, star.y, star.r, star.r);
        }
        gc.setGlobalAlpha(1.0);

        for (FallingObject obj : objects) {
            drawObject(obj);
        }

        for (Particle p : particles) {
            gc.setGlobalAlpha(Math.max(0, p.opacity));
            gc.setFill(p.color);
            gc.fillOval(p.x - 3, p.y - 3, 6, 6);
            gc.setGlobalAlpha(1.0);
        }

        for (FloatingText ft : floatingTexts) {
            gc.setGlobalAlpha(Math.max(0, ft.alpha));
            gc.setFill(ft.color);
            gc.setFont(Font.font("Verdana", javafx.scene.text.FontWeight.BOLD, 14));
            gc.fillText(ft.text, ft.x, ft.y);
            gc.setGlobalAlpha(1.0);
        }

        scoreLabel.setText(String.valueOf(score));
    }

    private void drawObject(FallingObject obj) {
        CelestialType type = obj.getType();
        double cx = obj.getX() + type.baseRadius;
        double cy = obj.getY() + type.baseRadius;
        double r = type.baseRadius;

        switch (type) {
            case NORMAL_STAR:
            case GOLDEN_STAR:
            case COSMIC_STAR:
            case STARDUST:
                drawGlow(cx, cy, r * 2.2, type.primaryColor);
                drawRing(cx, cy, r, type.primaryColor, 2);
                drawStarSpikes(cx, cy, r * 0.75, type.glowColor);
                break;

            case PLANET:
                drawGlow(cx, cy, r * 2.0, type.primaryColor);
                gc.setFill(type.primaryColor);
                gc.fillOval(cx - r * 0.6, cy - r * 0.6, r * 1.2, r * 1.2);
                gc.setStroke(type.glowColor);
                gc.setLineWidth(2.5);
                gc.save();
                gc.translate(cx, cy);
                gc.rotate(-20);
                gc.strokeOval(-r, -r * 0.32, r * 2, r * 0.64);
                gc.restore();
                break;

            case COMET: {
                double angle = Math.atan2(obj.getSpeedY(), obj.getSpeedX());
                double tailLen = r * 3.4;
                double tx = cx - Math.cos(angle) * tailLen;
                double ty = cy - Math.sin(angle) * tailLen;
                gc.setStroke(type.glowColor);
                gc.setLineWidth(r * 0.7);
                gc.setGlobalAlpha(0.32);
                gc.strokeLine(cx, cy, tx, ty);
                gc.setGlobalAlpha(1.0);
                drawGlow(cx, cy, r * 1.8, type.primaryColor);
                gc.setFill(Color.WHITE);
                gc.fillOval(cx - r * 0.4, cy - r * 0.4, r * 0.8, r * 0.8);
                drawRing(cx, cy, r, type.primaryColor, 2);
                break;
            }

            case METEOR: {
                drawGlow(cx, cy, r * 1.8, type.primaryColor);
                double[] xs = new double[6];
                double[] ys = new double[6];
                for (int k = 0; k < 6; k++) {
                    double a = obj.getRotation() + k * (Math.PI * 2 / 6);
                    double rr = r * (k % 2 == 0 ? 1.0 : 0.62);
                    xs[k] = cx + Math.cos(a) * rr;
                    ys[k] = cy + Math.sin(a) * rr;
                }
                gc.setFill(type.primaryColor);
                gc.fillPolygon(xs, ys, 6);
                gc.setStroke(type.glowColor);
                gc.setLineWidth(1.5);
                gc.strokePolygon(xs, ys, 6);
                break;
            }

            case BLACK_HOLE: {
                double pulse = 1.0 + 0.12 * Math.sin(elapsedSeconds * 3 + obj.getSeed());
                drawGlow(cx, cy, r * 2.3 * pulse, type.primaryColor);
                drawRing(cx, cy, r * pulse, type.primaryColor, 3);
                drawRing(cx, cy, r * 0.6 * pulse, type.glowColor, 2);
                gc.setFill(Color.web("#05030a"));
                gc.fillOval(cx - r * 0.4, cy - r * 0.4, r * 0.8, r * 0.8);
                break;
            }

            case DARK_MATTER:
                drawGlow(cx, cy, r * 1.6, type.primaryColor);
                gc.save();
                gc.translate(cx, cy);
                gc.rotate(Math.toDegrees(obj.getRotation()));
                gc.setFill(type.primaryColor);
                gc.fillPolygon(new double[]{0, r, 0, -r}, new double[]{-r, 0, r, 0}, 4);
                gc.setStroke(type.glowColor);
                gc.setLineWidth(1.5);
                gc.strokePolygon(new double[]{0, r, 0, -r}, new double[]{-r, 0, r, 0}, 4);
                gc.restore();
                break;
        }
    }

    private void drawGlow(double cx, double cy, double r, Color color) {
        RadialGradient rg = new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55)),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0)));
        gc.setFill(rg);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawRing(double cx, double cy, double r, Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawStarSpikes(double cx, double cy, double r, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeLine(cx - r, cy, cx + r, cy);
        gc.strokeLine(cx, cy - r, cx, cy + r);
        gc.strokeLine(cx - r * 0.6, cy - r * 0.6, cx + r * 0.6, cy + r * 0.6);
        gc.strokeLine(cx - r * 0.6, cy + r * 0.6, cx + r * 0.6, cy - r * 0.6);
        gc.setFill(color);
        gc.fillOval(cx - r * 0.32, cy - r * 0.32, r * 0.64, r * 0.64);
    }

    // ─── Game over ──────────────────────────────────────────────
    private void triggerGameOver() {
        gameOver = true;
        if (gameTimer != null) gameTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        if (gameMusicPlayer != null) gameMusicPlayer.stop();
        showGameOver();
    }

    private void showGameOver() {
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();

        gc.setFill(Color.web("#05070f", 0.93));
        gc.fillRect(0, 0, w, h);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.web("#ff5d73"));
        gc.setFont(Font.font("Verdana", javafx.scene.text.FontWeight.BOLD, 38));
        gc.fillText("MISSION ENDED", w / 2, h / 2 - 70);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 20));
        gc.fillText("Final Score: " + score, w / 2, h / 2 - 20);

        gc.setFill(Color.web("#ffd166"));
        gc.fillText("Best Score: " + best, w / 2, h / 2 + 12);

        gc.setFill(Color.web("#8fd6ff"));
        gc.fillText("Max Combo: x" + maxCombo, w / 2, h / 2 + 44);

        gc.setFill(Color.web("#c9cfe0"));
        gc.setFont(Font.font("Verdana", 15));
        gc.fillText("Level Reached: " + level, w / 2, h / 2 + 74);

        gc.setFill(Color.web("#7f88a3"));
        gc.setFont(Font.font("Verdana", 13));
        gc.fillText("Press ← to return to the Hub", w / 2, h / 2 + 110);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // NEW — Back to hub
    @FXML
    private void backToHub() {
        if (gameTimer != null) gameTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        if (gameMusicPlayer != null) gameMusicPlayer.stop();
        SoundManager.playMenuMusic();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            if (currentPlayer != null) controller.setPlayer(currentPlayer);
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            SceneManager.switchScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Supporting classes ─────────────────────────────────────
    private static class FallingObject {
        private double x, y, speedX, speedY, rotation;
        private final CelestialType type;
        private final double seed;

        FallingObject(double x, double y, double speedX, double speedY, CelestialType type, double seed) {
            this.x = x; this.y = y;
            this.speedX = speedX; this.speedY = speedY;
            this.type = type;
            this.seed = seed;
        }

        void update() {
            x += speedX;
            y += speedY;
            rotation += 0.05;
        }

        double getX() { return x; }
        double getY() { return y; }
        double getSpeedX() { return speedX; }
        double getSpeedY() { return speedY; }
        double getRotation() { return rotation; }
        double getSeed() { return seed; }
        CelestialType getType() { return type; }
    }

    private static class Particle {
        double x, y, vx, vy, opacity;
        Color color;

        Particle(double x, double y, Color color) {
            this.x = x; this.y = y;
            Random rng = new Random();
            this.vx = (rng.nextDouble() - 0.5) * 10;
            this.vy = (rng.nextDouble() - 0.5) * 10;
            this.opacity = 1.0;
            this.color = color;
        }

        void update() { x += vx; y += vy; opacity -= 0.04; }
        boolean isDead() { return opacity <= 0; }
    }

    private static class FloatingText {
        double x, y, alpha = 1.0;
        String text;
        Color color;

        FloatingText(double x, double y, String text, Color color) {
            this.x = x; this.y = y;
            this.text = text;
            this.color = color;
        }

        void update() { y -= 0.6; alpha -= 0.018; }
        boolean isDead() { return alpha <= 0; }
    }

    private static class BgStar {
        double x, y, r, phase;

        BgStar(double x, double y, double r, double phase) {
            this.x = x; this.y = y; this.r = r; this.phase = phase;
        }
    }
}
