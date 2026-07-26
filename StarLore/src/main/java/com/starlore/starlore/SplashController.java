package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
    private Canvas splashCanvas;

    private GraphicsContext gc;
    private Random random = new Random();

    // ─── Background Stars ─────────────────────────────────────
    private double[] starX = new double[150];
    private double[] starY = new double[150];
    private double[] starRadius = new double[150];
    private double[] starOpacity = new double[150];
    private double[] starTwinkleSpeed = new double[150];
    private boolean[] starTwinkleDir = new boolean[150];

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
        gc = splashCanvas.getGraphicsContext2D();
        generateBackgroundStars();

        splashCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                startAnimation();
            }
        });
    }

    // ─── Generate Background Stars ────────────────────────────

    private void generateBackgroundStars() {
        for (int i = 0; i < 150; i++) {
            starX[i] = random.nextDouble() * 1000;
            starY[i] = random.nextDouble() * 700;
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
        for (int i = 0; i < 150; i++) {
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
                if (progress >= letterStart) {
                    letterProgress[i] = Math.min(1.0,
                            (progress - letterStart) / (letterEnd - letterStart));
                }
            }
        }

        // Phase 3 — Glow pulse (3 to 3.5 sec)
        // Handled in render

        // Phase 4 — Shooting star (3.5 to 4.5 sec)
        if (globalTime >= 4 && globalTime < 6) {
            double progress = (globalTime - 4) / 2.0;
            shootingStarX = -50 + progress * 1100;
            shootingStarY = 320 + progress * 20;

            // Add sparkles
            if (random.nextDouble() < 0.4) {
                sparkles.add(new double[]{
                        shootingStarX + random.nextDouble() * 20 - 10,
                        shootingStarY + random.nextDouble() * 20 - 10,
                        1.0, // opacity
                        random.nextDouble() * 3 + 1 // size
                });
            }
        }

        // Update sparkles — fade out
        sparkles.removeIf(s -> s[2] <= 0);
        for (double[] s : sparkles) {
            s[2] -= 0.03;
        }

        // Phase 5 — Constellation to text (4.5 to 5.3 sec)
        if (globalTime >= 6 && globalTime < 6.8) {
            double progress = (globalTime - 6) / 0.8;
            constellationOpacity = 1.0 - progress;
            textOpacity = progress;
        }

        // Phase 6 — Fade out and navigate (5.3 to 6.3 sec)
        if (globalTime >= 7.5) {
            masterTimeline.stop();
            navigateToNameScreen();
        }
    }

    // ─── Render ───────────────────────────────────────────────

    private void render() {
        // Clear
        gc.setFill(Color.rgb(5, 8, 22));
        gc.fillRect(0, 0, 1000, 700);

        // Draw background stars
        for (int i = 0; i < 150; i++) {
            gc.setGlobalAlpha(starOpacity[i] * starfieldOpacity[0]);
            gc.setFill(Color.rgb(175, 201, 255));
            gc.fillOval(starX[i] - starRadius[i],
                    starY[i] - starRadius[i],
                    starRadius[i] * 2,
                    starRadius[i] * 2);
        }
        gc.setGlobalAlpha(1.0);

        // Draw constellation letters
        drawConstellation();

        // Draw shooting star
        if (globalTime >= 4.0 && globalTime < 6.0) {
            drawShootingStar();
        }

        // Draw sparkles
        for (double[] s : sparkles) {
            gc.setGlobalAlpha(s[2]);
            gc.setFill(Color.rgb(255, 255, 200));
            gc.fillOval(s[0] - s[3] / 2, s[1] - s[3] / 2, s[3], s[3]);
        }
        gc.setGlobalAlpha(1.0);

        // Draw StarLore text
       // if (textOpacity > 0) {
       //     drawStarLoreText();
      //  }

        // Phase 6 fade out
        if (globalTime >= 6.8) {
            double fadeProgress = Math.min(1.0, (globalTime - 6.8) / 0.7);
            gc.setGlobalAlpha(fadeProgress);
            gc.setFill(Color.rgb(5, 8, 22));
            gc.fillRect(0, 0, 1000, 700);
            gc.setGlobalAlpha(1.0);
        }
    }

    // ─── Draw Constellation ───────────────────────────────────

    private void drawConstellation() {
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
                        lstars[from][0], lstars[from][1],
                        lstars[to][0], lstars[to][1]);
            }

            // Draw stars
            for (int s = 0; s < lstars.length; s++) {
                double sx = lstars[s][0];
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
        // Trail
        for (int i = 0; i < 12; i++) {
            double trailX = shootingStarX - i * 12;
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
        gc.fillOval(shootingStarX - 6, shootingStarY - 6, 12, 12);

        // Head glow
        gc.setFill(Color.rgb(255, 255, 200, 0.4));
        gc.fillOval(shootingStarX - 14, shootingStarY - 14, 28, 28);
    }

    // ─── Draw StarLore Text ───────────────────────────────────

    private void drawStarLoreText() {
        gc.setGlobalAlpha(textOpacity);

        // Glow effect — draw text multiple times with blur simulation
        gc.setFill(Color.rgb(240, 200, 80, 0.3));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 72));
        gc.fillText("StarLore", 248, 358);
        gc.fillText("StarLore", 252, 362);

        // Main text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 68));
        gc.fillText("StarLore", 250, 358);

        gc.setGlobalAlpha(1.0);
    }

    // ─── Navigate ─────────────────────────────────────────────

    private void navigateToNameScreen() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("hello-view.fxml"));
            root.setOpacity(0);
            Stage stage = (Stage) splashCanvas.getScene().getWindow();
            stage.setScene(new Scene(root));

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}