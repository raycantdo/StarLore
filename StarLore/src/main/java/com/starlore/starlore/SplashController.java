package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SplashController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Canvas splashCanvas;
    @FXML
    private AnchorPane rootPane;

    private GraphicsContext gc;
    private Random random = new Random();

    // ─── Background Stars ─────────────────────────────────────
    private static final int NUM_STARS = 300;
    private double[] starX = new double[NUM_STARS];
    private double[] starY = new double[NUM_STARS];
    private double[] starRadius = new double[NUM_STARS];
    private double[] starOpacity = new double[NUM_STARS];
    private double[] starTwinkleSpeed = new double[NUM_STARS];
    private boolean[] starTwinkleDir = new boolean[NUM_STARS];

    // ─── StarLore Constellation Points ───────────────────────
    // Each letter defined by star positions and connections
    // Centered around y=340, spanning x=80 to x=920

    // S
    private double[][] sStars = {
            {170, 300}, {130, 280}, {90, 300},
            {130, 340}, {170, 360}, {130, 380}, {90, 360}
    };
    private int[][] sConn = {
            {0,1},{1,2},{2,3},{3,4},{4,5},{5,6}
    };

    // T
    private double[][] tStars = {
            {190, 280}, {270, 280},
            {230, 280}, {230, 380}
    };
    private int[][] tConn = {
            {0,1},{2,3}
    };

    // A
    private double[][] aStars = {
            {290, 380}, {330, 280}, {370, 380},
            {300, 335}, {360, 335}
    };
    private int[][] aConn = {
            {0,1},{1,2},{3,4}
    };

    // R
    private double[][] rStars = {
            {390, 380}, {390, 280}, {430, 280},
            {460, 310}, {430, 335}, {390, 335}, {460, 380}
    };
    private int[][] rConn = {
            {0,1},{1,2},{2,3},{3,4},{4,5},{4,6}
    };

    // L
    private double[][] lStars = {
            {490, 280}, {490, 380}, {560, 380}
    };
    private int[][] lConn = {
            {0,1},{1,2}
    };

    // O
    private double[][] oStars = {
            {590, 280}, {640, 280},
            {575, 330}, {655, 330},
            {590, 380}, {640, 380}
    };
    private int[][] oConn = {
            {0,1},{0,2},{1,3},{2,4},{3,5},{4,5}
    };

    // R2
    private double[][] r2Stars = {
            {675, 380}, {675, 280}, {715, 280},
            {740, 310}, {715, 335}, {675, 335}, {740, 380}
    };
    private int[][] r2Conn = {
            {0,1},{1,2},{2,3},{3,4},{4,5},{4,6}
    };

    // E
    private double[][] eStars = {
            {765, 280}, {845, 280},
            {765, 330}, {820, 330},
            {765, 380}, {845, 380}
    };
    private int[][] eConn = {
            {0,1},{2,3},{4,5},{0,4}
    };

    // All letter data combined
    private double[][][] allLetterStars = {sStars, tStars, aStars, rStars, lStars, oStars, r2Stars, eStars};
    private int[][][] allLetterConns = {sConn, tConn, aConn, rConn, lConn, oConn, r2Conn, eConn};

    // Animation state
    private double[] letterProgress = new double[8]; // 0 to 1 per letter
    private double[] starfieldOpacity = {0};
    private double shootingStarX = -50;
    private double shootingStarY = 340;
    private List<double[]> sparkles = new ArrayList<>();
    private double textOpacity = 0;
    private double constellationOpacity = 1;

    // Master timeline
    private Timeline masterTimeline;
    private double globalTime = 0;

    // ─── Initialize ───────────────────────────────────────────

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();
        splashCanvas.widthProperty().bind(rootPane.widthProperty());
        splashCanvas.heightProperty().bind(rootPane.heightProperty());
        gc = splashCanvas.getGraphicsContext2D();

        if (rootPane != null) {
            splashCanvas.widthProperty().bind(rootPane.prefWidthProperty());
            splashCanvas.heightProperty().bind(rootPane.prefHeightProperty());
            rootPane.prefWidthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    generateBackgroundStars(newVal.doubleValue(), 700.0);
                }
            });
        }

        double initialW = rootPane != null && rootPane.getPrefWidth() > 0 ? rootPane.getPrefWidth() : 1400.0;
        generateBackgroundStars(initialW, 700.0);

        splashCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                startAnimation();
            }
        });
    }

    // ─── Generate Background Stars ────────────────────────────

    private void generateBackgroundStars(double width, double height) {
        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] = random.nextDouble() * width;
            starY[i] = random.nextDouble() * height;
            starRadius[i] = random.nextDouble() * 2 + 0.5;
            starOpacity[i] = random.nextDouble() * 0.5 + 0.1;
            starTwinkleSpeed[i] = random.nextDouble() * 0.02 + 0.005;
            starTwinkleDir[i] = random.nextBoolean();
        }
    }

    // ─── Master Animation ─────────────────────────────────────

    private void startAnimation() {
        masterTimeline = new Timeline();
        masterTimeline.setCycleCount(Timeline.INDEFINITE);

        KeyFrame kf = new KeyFrame(Duration.millis(16), e -> {
            globalTime += 0.016;
            update();
            render();
        });

        masterTimeline.getKeyFrames().add(kf);
        masterTimeline.play();
    }

    // ─── Update State ─────────────────────────────────────────

    private void update() {

        // Phase 1 — Starfield fade in (0 to 1 sec)
        if (globalTime < 1.0) {
            starfieldOpacity[0] = globalTime;
        } else {
            starfieldOpacity[0] = 1.0;
        }

        // Twinkle update
        for (int i = 0; i < NUM_STARS; i++) {
            if (starTwinkleDir[i]) {
                starOpacity[i] += starTwinkleSpeed[i];
                if (starOpacity[i] >= 0.9) starTwinkleDir[i] = false;
            } else {
                starOpacity[i] -= starTwinkleSpeed[i];
                if (starOpacity[i] <= 0.1) starTwinkleDir[i] = true;
            }
        }

        // Phase 2 — Constellation draws (1 to 3 sec)
        if (globalTime >= 1.0 && globalTime < 3.5) {
            double progress = (globalTime - 1.0) / 2.0; // 0 to 1
            for (int i = 0; i < 8; i++) {
                double letterStart = i / 8.0;
                double letterEnd = (i + 1) / 8.0;

                if (progress <= letterStart) {
                    letterProgress[i] = 0;
                } else if (progress >= letterEnd) {
                    letterProgress[i] = 1.0;
                } else {
                    letterProgress[i] = (progress - letterStart) / (letterEnd - letterStart);
                }
            }
        }

        // Phase 3 — Shooting star (3.8 to 5.5 sec)
        if (globalTime >= 3.8 && globalTime < 5.5) {
            shootingStarX += 22;
            shootingStarY += 0.8;

            // Add sparkles near constellation stars as shooting star passes
            if (random.nextDouble() < 0.4) {
                sparkles.add(new double[]{
                        shootingStarX + (random.nextDouble() * 20 - 10),
                        shootingStarY + (random.nextDouble() * 20 - 10),
                        1.0, // opacity
                        random.nextDouble() * 3 + 1 // size
                });
            }
        }

        // Update sparkles
        for (int i = sparkles.size() - 1; i >= 0; i--) {
            double[] s = sparkles.get(i);
            s[2] -= 0.04; // fade out
            if (s[2] <= 0) sparkles.remove(i);
        }

        // Phase 4 — StarLore text fades in (5.0 to 6.0 sec)
        if (globalTime >= 5.0 && globalTime < 6.5) {
            textOpacity = Math.min(1.0, (globalTime - 5.0) / 1.0);
        }

        // Phase 5 — Constellation slowly dims (5.5 to 6.5 sec)
        if (globalTime >= 5.5 && globalTime < 6.8) {
            constellationOpacity = Math.max(0.2, 1.0 - (globalTime - 5.5) * 0.8);
        }

        // Phase 6 — Fade out and navigate (7.5 sec)
        if (globalTime >= 7.5) {
            masterTimeline.stop();
            navigateToNameScreen();
        }
    }

    // ─── Render ───────────────────────────────────────────────

    private void render() {
        double w = splashCanvas.getWidth() > 0 ? splashCanvas.getWidth() : 1400.0;
        double h = splashCanvas.getHeight() > 0 ? splashCanvas.getHeight() : 700.0;

        // Clear
        gc.setFill(Color.rgb(5, 8, 22));
        gc.fillRect(0, 0, w, h);

        // Draw background stars
        for (int i = 0; i < NUM_STARS; i++) {
            gc.setGlobalAlpha(starOpacity[i] * starfieldOpacity[0]);
            gc.setFill(Color.rgb(175, 201, 255));
            gc.fillOval(starX[i] - starRadius[i],
                    starY[i] - starRadius[i],
                    starRadius[i] * 2,
                    starRadius[i] * 2);
        }
        gc.setGlobalAlpha(1.0);

        // Draw constellation letters centered on wider screen
        drawConstellation();

        // Draw shooting star
        if (globalTime >= 3.8 && globalTime < 5.5) {
            drawShootingStar();
        }

        // Draw sparkles
        double canvasW = splashCanvas.getWidth() > 0 ? splashCanvas.getWidth() : 950.0;
        double offsetX = Math.max(0, (canvasW - 950.0) / 2.0);

        for (double[] s : sparkles) {
            gc.setGlobalAlpha(s[2]);
            gc.setFill(Color.rgb(255, 255, 200));
            gc.fillOval(s[0] + offsetX - s[3] / 2, s[1] - s[3] / 2, s[3], s[3]);
        }
        gc.setGlobalAlpha(1.0);

        // Phase 6 fade out
        if (globalTime >= 6.8) {
            double fadeProgress = Math.min(1.0, (globalTime - 6.8) / 0.7);
            gc.setGlobalAlpha(fadeProgress);
            gc.setFill(Color.rgb(5, 8, 22));
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }
    }

    // ─── Draw Constellation ───────────────────────────────────

    private void drawConstellation() {
        double canvasW = splashCanvas.getWidth() > 0 ? splashCanvas.getWidth() : 950.0;
        double offsetX = Math.max(0, (canvasW - 950.0) / 2.0);

        for (int l = 0; l < 8; l++) {
            if (letterProgress[l] <= 0) continue;

            double[][] lstars = allLetterStars[l];
            int[][] lconns = allLetterConns[l];

            // Glow pulse for completed letters
            double pulse = 1.0;
            if (globalTime >= 3.0 && globalTime < 3.5) {
                pulse = 0.7 + 0.3 * Math.sin((globalTime - 3.0) * Math.PI * 4);
            }

            // Draw connections
            int totalConns = lconns.length;
            int drawnConns = (int)(letterProgress[l] * totalConns);

            gc.setStroke(Color.rgb(175, 201, 255,
                    constellationOpacity * pulse));
            gc.setLineWidth(1.5);

            for (int c = 0; c < drawnConns; c++) {
                int from = lconns[c][0];
                int to = lconns[c][1];
                gc.strokeLine(
                        lstars[from][0] + offsetX, lstars[from][1],
                        lstars[to][0] + offsetX, lstars[to][1]);
            }

            // Draw stars
            for (int s = 0; s < lstars.length; s++) {
                double sx = lstars[s][0] + offsetX;
                double sy = lstars[s][1];

                // Outer glow
                gc.setFill(Color.rgb(175, 201, 255,
                        0.2 * constellationOpacity * pulse));
                gc.fillOval(sx - 8, sy - 8, 16, 16);

                // Star dot
                gc.setFill(Color.rgb(175, 201, 255,
                        constellationOpacity * pulse));
                gc.fillOval(sx - 3, sy - 3, 6, 6);
            }
        }
    }

    // ─── Draw Shooting Star ───────────────────────────────────

    private void drawShootingStar() {
        double canvasW = splashCanvas.getWidth() > 0 ? splashCanvas.getWidth() : 950.0;
        double offsetX = Math.max(0, (canvasW - 950.0) / 2.0);
        double actualX = shootingStarX + offsetX;

        // Trail
        for (int i = 0; i < 12; i++) {
            double trailX = actualX - i * 12;
            double trailY = shootingStarY - i * 1.5;
            double trailOpacity = (12 - i) / 12.0 * 0.8;
            double trailSize = (12 - i) / 12.0 * 8;

            gc.setFill(Color.rgb(255, 255, 220, trailOpacity));
            gc.fillOval(trailX - trailSize / 2,
                    trailY - trailSize / 2,
                    trailSize, trailSize);
        }

        // Star head
        gc.setFill(Color.WHITE);
        gc.fillOval(actualX - 6, shootingStarY - 6, 12, 12);

        // Head glow
        gc.setFill(Color.rgb(255, 255, 200, 0.4));
        gc.fillOval(actualX - 14, shootingStarY - 14, 28, 28);
    }

    // ─── Navigate ─────────────────────────────────────────────

    private void navigateToNameScreen() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("hello-view.fxml"));
            root.setOpacity(0);
            Stage stage = (Stage) splashCanvas.getScene().getWindow();
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
