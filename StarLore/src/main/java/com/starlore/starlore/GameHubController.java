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
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
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
    }

    // ─── Vertical Banner Hover Physics ───────────────────────────

    private void setupHoverEffects() {
        // Duel: Blue
        setupBannerHover(duelCard, "rgba(8, 14, 40, 0.7)", "rgba(12, 24, 70, 0.95)", "#38bdf8");
        // Story: Purple
        setupBannerHover(storyCard, "rgba(20, 8, 35, 0.7)", "rgba(45, 15, 90, 0.95)", "#c084fc");
        // Arcade: Red
        setupBannerHover(arcadeCard, "rgba(30, 8, 8, 0.7)", "rgba(75, 15, 25, 0.95)", "#f87171");
        // Quiz: Emerald
        setupBannerHover(quizCard, "rgba(6, 25, 20, 0.7)", "rgba(10, 65, 45, 0.95)", "#34d399");
    }

    private void setupBannerHover(StackPane banner, String normalBg, String hoverBg, String accentColor) {

        // Setup initial default style (Base state)
        banner.setStyle("-fx-background-color: " + normalBg + "; -fx-background-radius: 15; " +
                "-fx-border-color: " + accentColor + "66; -fx-border-radius: 15; -fx-border-width: 2; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, " + accentColor + "33, 15, 0, 0, 5);");

        // Physics: Y-axis lift and slight scale up
        TranslateTransition lift = new TranslateTransition(Duration.millis(150), banner);
        lift.setToY(-15);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), banner);
        scaleUp.setToX(1.03); scaleUp.setToY(1.03);

        // Physics: Return to base
        TranslateTransition drop = new TranslateTransition(Duration.millis(150), banner);
        drop.setToY(0);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), banner);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0);

        banner.setOnMouseEntered(e -> {
            banner.setStyle("-fx-background-color: " + hoverBg + "; -fx-background-radius: 15; " +
                    "-fx-border-color: " + accentColor + "; -fx-border-radius: 15; -fx-border-width: 3.5; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + accentColor + ", 45, 0.5, 0, 0);");
            lift.play();
            scaleUp.play();
        });

        banner.setOnMouseExited(e -> {
            banner.setStyle("-fx-background-color: " + normalBg + "; -fx-background-radius: 15; " +
                    "-fx-border-color: " + accentColor + "66; -fx-border-radius: 15; -fx-border-width: 2; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + accentColor + "33, 15, 0, 0, 5);");
            drop.play();
            scaleDown.play();
        });
    }

    // ─── Core Navigation ────────────────────────────────────────

    @FXML private void openProfile() { System.out.println("Opening Profile..."); }
    @FXML private void openLeaderboard() { System.out.println("Opening Leaderboard..."); }

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
        playBookTransition(() -> navigateTo("BattleScreen.fxml"));
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
        // Navigate when ready
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

    private void playBookTransition(Runnable onComplete) {
        GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
        double initW = getOverlayWidth();
        double initH = getOverlayHeight();
        gc.setFill(Color.rgb(5, 5, 20, 0.95));
        gc.fillRect(0, 0, initW, initH);

        double[] progress = {0};
        Timeline bookFlip = new Timeline();
        bookFlip.setCycleCount(25);
        KeyFrame frame = new KeyFrame(Duration.millis(60), e -> {
            progress[0] += 0.04;
            double w = getOverlayWidth();
            double h = getOverlayHeight();

            gc.clearRect(0, 0, w, h);
            gc.setFill(Color.rgb(5, 5, 20, 0.95));
            gc.fillRect(0, 0, w, h);

            double centerX = w / 2.0;
            double centerY = h / 2.0;
            double pageWidth = 300 + (progress[0] * 400);
            double pageHeight = 220;

            gc.setFill(Color.rgb(60, 0, 100, 0.9));
            gc.fillRoundRect(centerX - pageWidth, centerY - pageHeight / 2.0, pageWidth, pageHeight, 10, 10);
            gc.setFill(Color.rgb(80, 0, 130, 0.9));
            gc.fillRoundRect(centerX, centerY - pageHeight / 2.0, pageWidth, pageHeight, 10, 10);
        });
        bookFlip.getKeyFrames().add(frame);
        bookFlip.setOnFinished(e -> {
            double w = getOverlayWidth();
            double h = getOverlayHeight();
            gc.setFill(Color.rgb(20, 0, 40));
            gc.fillRect(0, 0, w, h);
            fadeOutAndNavigate(onComplete);
        });
        bookFlip.play();
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

        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRect(0, 0, w, h);
        transitionLabel.setText("INITIALIZING VELOCITY DRIVE...");
        transitionLabel.setVisible(true);

        PauseTransition msgPause = new PauseTransition(Duration.seconds(1.0));
        msgPause.setOnFinished(e -> {
            transitionLabel.setVisible(false);
            double curW = getOverlayWidth();
            double curH = getOverlayHeight();

            double[] starX = {-80};
            double[] starY = {curH * 0.35 + random.nextInt(Math.max(1, (int)(curH * 0.3)))};
            double speed = (curW + 160) / 30.0;

            Timeline starAnim = new Timeline();
            starAnim.setCycleCount(30);
            KeyFrame kf = new KeyFrame(Duration.millis(25), ev -> {
                double frameW = getOverlayWidth();
                double frameH = getOverlayHeight();

                gc.clearRect(0, 0, frameW, frameH);
                gc.setFill(Color.rgb(0, 0, 0, 0.85));
                gc.fillRect(0, 0, frameW, frameH);

                starX[0] += speed;
                starY[0] += 6;

                gc.setStroke(Color.rgb(255, 255, 200, 0.6));
                gc.setLineWidth(4);
                gc.strokeLine(starX[0] - 120, starY[0] - 18, starX[0], starY[0]);
                gc.setFill(Color.WHITE);
                gc.fillOval(starX[0] - 8, starY[0] - 8, 16, 16);
            });
            starAnim.getKeyFrames().add(kf);
            starAnim.setOnFinished(ev -> {
                double finW = getOverlayWidth();
                double finH = getOverlayHeight();
                gc.setFill(Color.rgb(10, 0, 0));
                gc.fillRect(0, 0, finW, finH);
                fadeOutAndNavigate(onComplete);
            });
            starAnim.play();
        });
        msgPause.play();
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