package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameHubController {

    @FXML private StackPane rootStackPane;
    @FXML private Canvas hubCanvas;
    @FXML private Label rankLabel;
    @FXML private Label starDustLabel;
    @FXML private Button profileBtn;
    @FXML private Button leaderboardBtn;
    @FXML private Button signOutBtn;


    @FXML private StackPane storyCard;
    @FXML private StackPane duelCard;
    @FXML private StackPane quizCard;
    @FXML private StackPane arcadeCard;

    @FXML private StackPane transitionOverlay;
    @FXML private Canvas transitionCanvas;
    @FXML private Label transitionLabel;

    private Player currentPlayer;
    private final Random random = new Random();
    private AnimationTimer hubAnimationTimer;
    private double timeElapsed = 0;
    private double mouseX = 500;
    private double mouseY = 350;

    // ─── Procedural Background Star & Constellation Data ────────
    private static final int NUM_BG_STARS = 200;
    private final double[] starX = new double[NUM_BG_STARS];
    private final double[] starY = new double[NUM_BG_STARS];
    private final double[] starR = new double[NUM_BG_STARS];
    private final double[] starAlpha = new double[NUM_BG_STARS];
    private final double[] starSpeed = new double[NUM_BG_STARS];

    private static class ConstellationNode {
        double x, y, vx, vy;
        ConstellationNode(double x, double y, double vx, double vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }
    private final List<ConstellationNode> constellationStars = new ArrayList<>();
    private static final int NUM_CONSTELLATION_NODES = 35;
    private static final double MAX_CONNECT_DIST = 160.0;

    // ─── Ambient Shooting Stars ────────────────────────────────
    private static class HubShootingStar {
        double x, y, vx, vy, len, life, maxLife;
        Color color;
        HubShootingStar(double x, double y, double vx, double vy, double len, double maxLife, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.len = len; this.life = maxLife; this.maxLife = maxLife;
            this.color = color;
        }
    }
    private final List<HubShootingStar> hubShootingStars = new ArrayList<>();

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();

        if (rootStackPane != null && hubCanvas != null) {
            hubCanvas.widthProperty().bind(rootStackPane.widthProperty());
            hubCanvas.heightProperty().bind(rootStackPane.heightProperty());
            if (transitionCanvas != null) {
                transitionCanvas.widthProperty().bind(rootStackPane.widthProperty());
                transitionCanvas.heightProperty().bind(rootStackPane.heightProperty());
            }
        }

        initCosmicStars();
        setupInteractiveMouseTracking();
        setupHoverEffects();
        startHubAnimation();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;

        if (rankLabel != null) {
            String rank = player.getRankTitle();
            if (rank == null || rank.trim().isEmpty()) {
                rank = "Stargazer";
            }
            rankLabel.setText("LVL 1 - " + rank.toUpperCase());
        }

        if (starDustLabel != null) {
            starDustLabel.setText("✦ " + player.getTotalStarDust() + " DUST");
        }
    }

    private void initCosmicStars() {
        for (int i = 0; i < NUM_BG_STARS; i++) {
            starX[i] = random.nextDouble() * 1920;
            starY[i] = random.nextDouble() * 1080;
            starR[i] = random.nextDouble() * 2.5 + 0.5;
            starAlpha[i] = random.nextDouble() * 0.5 + 0.1;
            starSpeed[i] = random.nextDouble() * 0.3 + 0.05;
        }

        constellationStars.clear();
        for (int i = 0; i < NUM_CONSTELLATION_NODES; i++) {
            constellationStars.add(new ConstellationNode(
                    random.nextDouble() * 1200,
                    random.nextDouble() * 800,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.4
            ));
        }
    }

    private void setupInteractiveMouseTracking() {
        if (rootStackPane != null) {
            rootStackPane.setOnMouseMoved(e -> {
                mouseX = e.getX();
                mouseY = e.getY();
            });
        }
    }

    // ─── Heavy UI Canvas Loop (Astrolabe + Constellations) ───────

    private void startHubAnimation() {
        hubAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                timeElapsed += 0.016;
                renderHubCanvas();
            }
        };
        hubAnimationTimer.start();
    }

    private void renderHubCanvas() {
        if (hubCanvas == null) return;
        GraphicsContext gc = hubCanvas.getGraphicsContext2D();
        double w = hubCanvas.getWidth();
        double h = hubCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.web("#020308"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.rgb(255, 255, 255, 0.02));
        gc.setLineWidth(1);
        for(int x=0; x<w; x+=60) gc.strokeLine(x, 0, x, h);
        for(int y=0; y<h; y+=60) gc.strokeLine(0, y, w, y);

        gc.save();
        gc.translate(w/2, h/2);
        gc.rotate(timeElapsed * 5);
        gc.setStroke(Color.rgb(56, 189, 248, 0.08));
        gc.setLineWidth(2);
        gc.strokeOval(-350, -350, 700, 700);
        gc.setStroke(Color.rgb(192, 132, 252, 0.05));
        gc.strokeOval(-450, -450, 900, 900);

        for(int i=0; i<36; i++) {
            gc.rotate(10);
            gc.setStroke(Color.rgb(255, 255, 255, 0.1));
            gc.strokeLine(0, 350, 0, 365);
        }
        gc.restore();

        RadialGradient nebulaPurple = new RadialGradient(
                0, 0, w * 0.8, h * 0.2, w * 0.5, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(112, 26, 230, 0.12)),
                new Stop(0.6, Color.TRANSPARENT)
        );
        gc.setFill(nebulaPurple);
        gc.fillRect(0,0,w,h);

        RadialGradient nebulaCyan = new RadialGradient(
                0, 0, w * 0.2, h * 0.8, w * 0.5, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 165, 233, 0.1)),
                new Stop(0.6, Color.TRANSPARENT)
        );
        gc.setFill(nebulaCyan);
        gc.fillRect(0,0,w,h);

        for (int i = 0; i < NUM_BG_STARS; i++) {
            starY[i] -= starSpeed[i];
            if (starY[i] < -10) starY[i] = h + 10;

            double alpha = starAlpha[i] + Math.sin(timeElapsed * 1.5 + i) * 0.2;
            alpha = Math.max(0.05, Math.min(0.9, alpha));

            gc.setFill(Color.rgb(224, 242, 254, alpha));
            gc.fillOval(starX[i], starY[i], starR[i], starR[i]);
        }

        for (ConstellationNode node : constellationStars) {
            node.x += node.vx;
            node.y += node.vy;
            if (node.x < 0 || node.x > w) node.vx = -node.vx;
            if (node.y < 0 || node.y > h) node.vy = -node.vy;
        }

        gc.setLineWidth(1.2);
        for (int i = 0; i < constellationStars.size(); i++) {
            ConstellationNode s1 = constellationStars.get(i);
            for (int j = i + 1; j < constellationStars.size(); j++) {
                ConstellationNode s2 = constellationStars.get(j);
                double dist = Math.hypot(s1.x - s2.x, s1.y - s2.y);
                if (dist < MAX_CONNECT_DIST) {
                    double lineAlpha = (1.0 - (dist / MAX_CONNECT_DIST)) * 0.35;
                    gc.setStroke(Color.rgb(125, 211, 252, lineAlpha));
                    gc.strokeLine(s1.x, s1.y, s2.x, s2.y);
                }
            }

            double mouseDist = Math.hypot(s1.x - mouseX, s1.y - mouseY);
            if (mouseDist < 200.0) {
                double mouseAlpha = (1.0 - (mouseDist / 200.0)) * 0.6;
                gc.setStroke(Color.rgb(56, 189, 248, mouseAlpha));
                gc.strokeLine(s1.x, s1.y, mouseX, mouseY);
            }

            gc.setFill(Color.rgb(56, 189, 248, 0.4));
            gc.fillOval(s1.x - 5, s1.y - 5, 10, 10);
            gc.setFill(Color.WHITE);
            gc.fillOval(s1.x - 1.5, s1.y - 1.5, 3, 3);
        }

        // ─── Occasional Shooting Stars (☄️) ────────────────────────
        if (random.nextDouble() < 0.025) {
            double startX = random.nextDouble() * (w * 0.85);
            double startY = random.nextDouble() * (h * 0.4);
            double speed = random.nextDouble() * 10.0 + 12.0;
            double angle = Math.toRadians(random.nextDouble() * 25.0 + 25.0);
            Color col = (random.nextBoolean()) ? Color.web("#38bdf8") : (random.nextBoolean() ? Color.web("#fde047") : Color.web("#c084fc"));
            hubShootingStars.add(new HubShootingStar(
                    startX, startY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    random.nextDouble() * 50.0 + 40.0,
                    random.nextDouble() * 25.0 + 20.0,
                    col
            ));
        }

        for (int i = hubShootingStars.size() - 1; i >= 0; i--) {
            HubShootingStar ss = hubShootingStars.get(i);
            ss.x += ss.vx;
            ss.y += ss.vy;
            ss.life--;
            if (ss.life <= 0 || ss.x > w + 100 || ss.y > h + 100) {
                hubShootingStars.remove(i);
                continue;
            }
            double alpha = Math.max(0.0, ss.life / ss.maxLife);
            gc.setStroke(Color.color(ss.color.getRed(), ss.color.getGreen(), ss.color.getBlue(), alpha));
            gc.setLineWidth(2.0);
            gc.strokeLine(ss.x, ss.y, ss.x - ss.vx * (ss.len / 12.0), ss.y - ss.vy * (ss.len / 12.0));
        }
    }

    // ─── Vertical Banner Hover Physics (Differentiated Silhouettes) ───

    private void setupHoverEffects() {
        // Duel: Electric Blue & Cyan (Shield Shape: 28 8 28 8)
        setupBannerHover(duelCard, "rgba(6, 16, 42, 0.78)", "rgba(10, 32, 75, 0.95)", "#38bdf8", "28 8 28 8");
        // Story: Royal Purple (Arched Portal: 40 40 12 12)
        setupBannerHover(storyCard, "rgba(22, 10, 42, 0.78)", "rgba(48, 18, 92, 0.95)", "#c084fc", "40 40 12 12");
        // Arcade: Solar Gold (Dynamic Blade: 8 28 8 28)
        setupBannerHover(arcadeCard, "rgba(36, 24, 6, 0.78)", "rgba(72, 48, 12, 0.95)", "#f59e0b", "8 28 8 28");
        // Quiz: Astral Cyan (Mystic Pedestal: 20)
        setupBannerHover(quizCard, "rgba(6, 28, 36, 0.78)", "rgba(12, 58, 72, 0.95)", "#22d3ee", "20");
    }

    private void setupBannerHover(StackPane banner, String normalBg, String hoverBg, String accentColor, String radius) {
        if (banner == null) return;

        // Setup initial default style (Base state)
        banner.setStyle("-fx-background-color: " + normalBg + "; -fx-background-radius: " + radius + "; " +
                "-fx-border-color: " + accentColor + "55; -fx-border-radius: " + radius + "; -fx-border-width: 1.8; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, " + accentColor + "33, 16, 0, 0, 4);");

        // Physics: Y-axis lift and slight scale up
        TranslateTransition lift = new TranslateTransition(Duration.millis(160), banner);
        lift.setToY(-14);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(160), banner);
        scaleUp.setToX(1.03); scaleUp.setToY(1.03);

        // Physics: Return to base
        TranslateTransition drop = new TranslateTransition(Duration.millis(160), banner);
        drop.setToY(0);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(160), banner);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0);

        banner.setOnMouseEntered(e -> {
            banner.setStyle("-fx-background-color: " + hoverBg + "; -fx-background-radius: " + radius + "; " +
                    "-fx-border-color: " + accentColor + "; -fx-border-radius: " + radius + "; -fx-border-width: 3.0; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + accentColor + ", 38, 0.45, 0, 0);");
            lift.play();
            scaleUp.play();
        });

        banner.setOnMouseExited(e -> {
            banner.setStyle("-fx-background-color: " + normalBg + "; -fx-background-radius: " + radius + "; " +
                    "-fx-border-color: " + accentColor + "55; -fx-border-radius: " + radius + "; -fx-border-width: 1.8; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + accentColor + "33, 16, 0, 0, 4);");
            drop.play();
            scaleDown.play();
        });
    }

    // ─── Core Navigation ────────────────────────────────────────

    @FXML private void openProfile() { System.out.println("Opening Profile..."); }
    @FXML

    private void openLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("Leaderboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) leaderboardBtn.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("StarLore - Leaderboard");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void signOut() {
        stopHubAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
            root.setOpacity(0);
            Stage stage = (Stage) storyCard.getScene().getWindow();
            SceneManager.switchScene(stage, root);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void launchStoryMode() {
        transitionOverlay.setVisible(true);
        playStoryTransition(() -> navigateTo("BattleScreen.fxml"));
    }

    @FXML
    private void launchDuelMode() {
        transitionOverlay.setVisible(true);
        playSparkTransition(() -> {
            stopHubAnimation();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("DuelMode.fxml"));
                Parent root = loader.load();
                DuelController controller = loader.getController();
                controller.setPlayer(currentPlayer);
                root.setOpacity(0);
                Stage stage = (Stage) storyCard.getScene().getWindow();
                SceneManager.switchScene(stage, root);
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void launchMythQuiz() {
        transitionOverlay.setVisible(true);
        // Changed from playSparkTransition to the new playQuizTransition
        playQuizTransition(() -> {
            stopHubAnimation();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("MythQuizView.fxml"));
                Parent root = loader.load();
                MythQuizController controller = loader.getController();
                controller.setPlayer(currentPlayer);
                root.setOpacity(0);
                Stage stage = (Stage) quizCard.getScene().getWindow();
                SceneManager.switchScene(stage, root);
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void launchArcade() {
        transitionOverlay.setVisible(true);
        playArcadeTransition(() -> {
            stopHubAnimation();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ShootingStar.fxml"));
                Parent root = loader.load();
                ShootingStarController controller = loader.getController();
                controller.setPlayer(currentPlayer);
                root.setOpacity(0);
                Stage stage = (Stage) arcadeCard.getScene().getWindow();
                SceneManager.switchScene(stage, root);
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void stopHubAnimation() {
        if (hubAnimationTimer != null) {
            hubAnimationTimer.stop();
        }
    }

    private double getOverlayWidth() {
        if (transitionCanvas != null && transitionCanvas.getWidth() > 0) {
            return transitionCanvas.getWidth();
        }
        if (rootStackPane != null && rootStackPane.getWidth() > 0) {
            return rootStackPane.getWidth();
        }
        return 1000.0;
    }

    private double getOverlayHeight() {
        if (transitionCanvas != null && transitionCanvas.getHeight() > 0) {
            return transitionCanvas.getHeight();
        }
        if (rootStackPane != null && rootStackPane.getHeight() > 0) {
            return rootStackPane.getHeight();
        }
        return 700.0;
    }

    // ─── Transitions ───────────────────────────────────────────

    private void playStoryTransition(Runnable onComplete) {
        GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
        double initW = getOverlayWidth();
        double initH = getOverlayHeight();
        gc.setFill(Color.rgb(2, 4, 15, 0.95));
        gc.fillRect(0, 0, initW, initH);

        transitionLabel.setText("✦ EMBARKING ON CELESTIAL ODYSSEY ✦");
        transitionLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffd65a; -fx-effect: dropshadow(gaussian, #f59e0b, 18, 0.7, 0, 0);");
        transitionLabel.setVisible(true);

        double[] frame = {0};
        Timeline storyTimeline = new Timeline();
        storyTimeline.setCycleCount(32);
        KeyFrame kf = new KeyFrame(Duration.millis(35), e -> {
            frame[0]++;
            double w = getOverlayWidth();
            double h = getOverlayHeight();
            double cx = w / 2.0;
            double cy = h / 2.0;

            gc.clearRect(0, 0, w, h);
            gc.setFill(Color.rgb(2, 4, 15, 0.94));
            gc.fillRect(0, 0, w, h);

            // Expanding cosmic rings
            for (int r = 1; r <= 4; r++) {
                double radius = (frame[0] * 12 + r * 60) % (Math.max(w, h) * 0.75);
                double alpha = Math.max(0, 1.0 - radius / (Math.max(w, h) * 0.75));
                gc.setStroke(Color.rgb(56, 189, 248, alpha * 0.5));
                gc.setLineWidth(1.8);
                gc.strokeOval(cx - radius, cy - radius * 0.6, radius * 2, radius * 1.2);
            }

            // Radial golden starlight rays
            gc.setStroke(Color.rgb(254, 240, 138, 0.45));
            gc.setLineWidth(1.5);
            for (int i = 0; i < 12; i++) {
                double ang = (i * Math.PI / 6.0) + frame[0] * 0.04;
                double len = 40 + frame[0] * 8;
                gc.strokeLine(cx, cy, cx + Math.cos(ang) * len, cy + Math.sin(ang) * len);
            }
        });
        storyTimeline.getKeyFrames().add(kf);
        storyTimeline.setOnFinished(e -> {
            transitionLabel.setVisible(false);
            fadeOutAndNavigate(onComplete);
        });
        storyTimeline.play();
    }

    private void playSparkTransition(Runnable onComplete) {
        GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
        double initW = getOverlayWidth();
        double initH = getOverlayHeight();
        gc.setFill(Color.rgb(0, 0, 10, 0.92));
        gc.fillRect(0, 0, initW, initH);

        double[] frame = {0};
        Timeline spark = new Timeline();
        spark.setCycleCount(30);
        KeyFrame kf = new KeyFrame(Duration.millis(50), e -> {
            frame[0]++;
            double w = getOverlayWidth();
            double h = getOverlayHeight();

            gc.clearRect(0, 0, w, h);
            gc.setFill(Color.rgb(0, 0, 10, 0.92));
            gc.fillRect(0, 0, w, h);

            double cx = w / 2.0;
            double cy = h / 2.0;
            double offset = Math.max(0, 200 - frame[0] * 14);

            gc.setFill(Color.DODGERBLUE);
            gc.fillOval(cx - offset - 20, cy - 20, 40, 40);
            gc.setFill(Color.GOLD);
            gc.fillOval(cx + offset - 20, cy - 20, 40, 40);

            if (frame[0] > 14) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                for (int i = 0; i < 20; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double length = random.nextDouble() * 80 + 20;
                    gc.strokeLine(cx, cy,
                            cx + Math.cos(angle) * length,
                            cy + Math.sin(angle) * length);
                }

                gc.setFill(Color.rgb(255, 255, 200, Math.max(0, 0.6 - frame[0] * 0.03)));
                gc.fillRect(0, 0, w, h);
            }
        });
        spark.getKeyFrames().add(kf);
        spark.setOnFinished(e -> {
            double w = getOverlayWidth();
            double h = getOverlayHeight();
            gc.setFill(Color.rgb(0, 0, 20));
            gc.fillRect(0, 0, w, h);
            fadeOutAndNavigate(onComplete);
        });
        spark.play();
    }

    private void playArcadeTransition(Runnable onComplete) {
        GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
        double w = getOverlayWidth();
        double h = getOverlayHeight();

        gc.setFill(Color.rgb(2, 4, 15, 0.92));
        gc.fillRect(0, 0, w, h);

        // Step 1: Initializing velocity drive
        transitionLabel.setText("INITIALIZING VELOCITY DRIVE...");
        transitionLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 22px; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, #0284c7, 16, 0.8, 0, 0);");
        transitionLabel.setVisible(true);

        PauseTransition msgPause = new PauseTransition(Duration.seconds(0.85));
        msgPause.setOnFinished(e -> {
            // Step 2: Humorous text "CATCH STARS, NOT FEELINGS"
            transitionLabel.setText("✦ CATCH STARS, NOT FEELINGS... ✦");
            transitionLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ffd65a; -fx-effect: dropshadow(gaussian, #f43f5e, 22, 0.85, 0, 0);");

            // Step 3: Love Arrow flies playfully "here and there in the screen"
            int totalFrames = 68;
            double[] frame = {0};
            double[] prevX = {-1};
            double[] prevY = {-1};
            List<double[]> trail = new ArrayList<>();

            Timeline arrowAnim = new Timeline();
            arrowAnim.setCycleCount(totalFrames);
            KeyFrame kf = new KeyFrame(Duration.millis(26), ev -> {
                frame[0]++;
                double curW = getOverlayWidth();
                double curH = getOverlayHeight();
                double cx = curW / 2.0;
                double cy = curH / 2.0;
                double t = frame[0] / (double) totalFrames; // 0.0 -> 1.0

                gc.clearRect(0, 0, curW, curH);
                gc.setFill(Color.rgb(2, 4, 15, 0.90));
                gc.fillRect(0, 0, curW, curH);

                // Playful curving path that loops and swoops across multiple quadrants
                double arrowX = cx + Math.sin(t * Math.PI * 3.4) * (curW * 0.40) + (t - 0.5) * (curW * 0.30);
                double arrowY = cy + Math.cos(t * Math.PI * 2.6) * (curH * 0.30) + Math.sin(t * Math.PI * 5.2) * 35.0;

                double angle;
                if (prevX[0] >= 0) {
                    angle = Math.atan2(arrowY - prevY[0], arrowX - prevX[0]);
                } else {
                    angle = 0.35;
                }
                prevX[0] = arrowX;
                prevY[0] = arrowY;

                // Add to sparkling trail
                trail.add(new double[]{arrowX, arrowY, 1.0, 14.0});

                // Render fading sparkling trail
                for (int ti = trail.size() - 1; ti >= 0; ti--) {
                    double[] p = trail.get(ti);
                    p[2] -= 0.04;
                    if (p[2] <= 0) {
                        trail.remove(ti);
                        continue;
                    }
                    gc.setGlobalAlpha(Math.max(0, p[2]));
                    Color sparkColor = (ti % 3 == 0) ? Color.web("#f43f5e") : ((ti % 3 == 1) ? Color.web("#ffd65a") : Color.web("#38bdf8"));
                    gc.setFill(sparkColor);
                    gc.fillOval(p[0] - p[3] / 2.0, p[1] - p[3] / 2.0, p[3] * p[2], p[3] * p[2]);
                }
                gc.setGlobalAlpha(1.0);

                // Draw Love Arrow
                drawLoveArrow(gc, arrowX, arrowY, angle);
            });

            arrowAnim.getKeyFrames().add(kf);
            arrowAnim.setOnFinished(ev -> {
                transitionLabel.setVisible(false);
                fadeOutAndNavigate(onComplete);
            });
            arrowAnim.play();
        });
        msgPause.play();
    }

     private void playQuizTransition(Runnable onComplete) {
        GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
        double initW = getOverlayWidth();
        double initH = getOverlayHeight();

        gc.setFill(Color.rgb(4, 12, 18, 0.95));
        gc.fillRect(0, 0, initW, initH);

        transitionLabel.setText("✦ CONSULTING THE ASTRAL ORACLE ✦");
        transitionLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #a5f3fc; -fx-effect: dropshadow(gaussian, #06b6d4, 18, 0.7, 0, 0);");
        transitionLabel.setVisible(true);

        double[] frame = {0};
        Timeline quizTimeline = new Timeline();

        // 72 cycles * 35ms = ~2.5 seconds total
        quizTimeline.setCycleCount(72);

        KeyFrame kf = new KeyFrame(Duration.millis(35), e -> {
            frame[0]++;
            double w = getOverlayWidth();
            double h = getOverlayHeight();
            double cx = w / 2.0;
            double cy = h / 2.0;
            double t = frame[0];

            gc.clearRect(0, 0, w, h);
            gc.setFill(Color.rgb(4, 12, 18, 0.95));
            gc.fillRect(0, 0, w, h);

            gc.save();
            gc.translate(cx, cy);

            double radius = t * 18;
            double alpha = Math.max(0, 1.0 - (t / 72.0));
            gc.setStroke(Color.rgb(34, 211, 238, alpha));
            gc.setLineWidth(2.5);
            gc.strokeOval(-radius, -radius, radius * 2, radius * 2);

            gc.setStroke(Color.rgb(165, 243, 252, alpha * 0.5));
            gc.strokeOval(-radius * 0.6, -radius * 0.6, radius * 1.2, radius * 1.2);

            gc.rotate(t * 6);
            gc.setStroke(Color.rgb(6, 182, 212, Math.min(1.0, t * 0.05)));
            gc.setLineWidth(1.5);

            double rectSize = 80 + (Math.sin(t * 0.1) * 20);
            gc.strokeRect(-rectSize/2, -rectSize/2, rectSize, rectSize);
            gc.rotate(45);
            gc.strokeRect(-rectSize/2, -rectSize/2, rectSize, rectSize);

            double pulse = Math.abs(Math.sin(t * 0.2)) * 12;
            gc.setFill(Color.rgb(34, 211, 238, 0.9));

            double[] xPoints = {0, 20 + pulse, 0, -20 - pulse};
            double[] yPoints = {-35 - pulse, 0, 35 + pulse, 0};
            gc.fillPolygon(xPoints, yPoints, 4);

            gc.setFill(Color.WHITE);
            gc.fillOval(-6, -6, 12, 12);

            gc.restore();
        });

        quizTimeline.getKeyFrames().add(kf);
        quizTimeline.setOnFinished(e -> {
            transitionLabel.setVisible(false);
            fadeOutAndNavigate(onComplete);
        });
        quizTimeline.play();
    }

    private void drawLoveArrow(GraphicsContext gc, double x, double y, double angle) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angle));

        // 1. Arrow Shaft (Gradient from cyan to pink to gold)
        gc.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#38bdf8")),
                new Stop(0.5, Color.web("#f43f5e")),
                new Stop(1.0, Color.web("#ffd65a"))));
        gc.setLineWidth(3.5);
        gc.strokeLine(-48, 0, 0, 0);

        // 2. Fletching / Feathers at tail (-48)
        gc.setStroke(Color.web("#38bdf8"));
        gc.setLineWidth(2.2);
        gc.strokeLine(-48, 0, -58, -10);
        gc.strokeLine(-48, 0, -58, 10);
        gc.strokeLine(-38, 0, -48, -8);
        gc.strokeLine(-38, 0, -48, 8);

        // 3. Heart Arrowhead at (0, 0)
        gc.setFill(Color.web("#f43f5e"));
        gc.setEffect(new DropShadow(14, Color.web("#fb7185")));

        double hr = 9.0;
        gc.fillOval(-hr * 0.4, -hr, hr * 1.2, hr * 1.1);
        gc.fillOval(-hr * 0.4, -0.1, hr * 1.2, hr * 1.1);
        double[] tx = {0, 15, 0};
        double[] ty = {-hr * 0.9, 0, hr * 0.9};
        gc.fillPolygon(tx, ty, 3);

        // Inner white starlight shine
        gc.setFill(Color.web("#fff1f2"));
        gc.fillOval(2, -2.5, 5, 5);

        gc.restore();
    }

    private void fadeOutAndNavigate(Runnable onComplete) {
        try {
            onComplete.run();
        } finally {
            transitionOverlay.setVisible(false);
            transitionOverlay.setOpacity(1.0);
        }
    }

    private void navigateTo(String fxmlFile) {
        stopHubAnimation();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof BattleController bc && currentPlayer != null) {
                bc.setPlayer(currentPlayer);
            }
            root.setOpacity(0);
            Stage stage = (Stage) storyCard.getScene().getWindow();
            SceneManager.switchScene(stage, root);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}