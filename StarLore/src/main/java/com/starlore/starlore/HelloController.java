package com.starlore.starlore;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HelloController {

    @FXML private AnchorPane rootPane;
    @FXML private Canvas astrolabeCanvas;
    @FXML private TextField nameField;
    @FXML private Pane hologramBaseLinePane;
    @FXML private Line neonBaseLine;
    @FXML private Line scanPulseLine;
    @FXML private Label hologramHint;
    @FXML private HBox orbsContainer;
    @FXML private Label orbLoreLabel;

    private PlayerDAO playerDAO = new PlayerDAO();
    private AnimationTimer animationTimer;
    private double astrolabeAngle = 0;
    private double timeElapsed = 0;
    private Random random = new Random();

    // ─── Celestial Orbs (Star Cores) Data ────────────────────────
    private static class CelestialCore {
        String name;
        String title;
        String element;
        Color primaryColor;
        Color glowColor;
        Color coreColor;

        CelestialCore(String name, String title, String element, Color primaryColor, Color glowColor, Color coreColor) {
            this.name = name;
            this.title = title;
            this.element = element;
            this.primaryColor = primaryColor;
            this.glowColor = glowColor;
            this.coreColor = coreColor;
        }
    }

    private final CelestialCore[] CORES = {
            new CelestialCore("Solar Flame", "Solar Corona", "Solar Flare Energy",
                    Color.web("#f97316"), Color.web("#facc15"), Color.web("#ffffff")),
            new CelestialCore("Crimson Supergiant", "Ruby Stellar Core", "Thermonuclear Fusion Flare",
                    Color.web("#dc2626"), Color.web("#ef4444"), Color.web("#ffffff")),
            new CelestialCore("Toxic Nebula", "Bio-Nebular Core", "Emerald Cosmic Genesis",
                    Color.web("#10b981"), Color.web("#34d399"), Color.web("#a7f3d0")),
            new CelestialCore("Void Amethyst", "Dark Matter Void", "Gravitational Singularity",
                    Color.web("#8b5cf6"), Color.web("#c084fc"), Color.web("#f3e8ff")),
            new CelestialCore("Glacial Nova", "Supernova Diamond", "Pristine Astral Brilliance",
                    Color.web("#0ea5e9"), Color.web("#38bdf8"), Color.web("#ffffff"))
    };

    private int activeCoreIndex = 4; // Default to Glacial Nova (Starlight Blue)
    private double[] orbPulse = new double[5];

    // Background Stars for Depth of Field
    private static final int NUM_BG_STARS = 140;
    private double[] starX = new double[NUM_BG_STARS];
    private double[] starY = new double[NUM_BG_STARS];
    private double[] starR = new double[NUM_BG_STARS];
    private double[] starAlpha = new double[NUM_BG_STARS];

    // Occasional Tiny Shooting Stars
    private static class ShootingStarStreak {
        double x, y, vx, vy, len, life, maxLife;
        Color color;
        ShootingStarStreak(double x, double y, double vx, double vy, double len, double maxLife, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.len = len; this.life = maxLife; this.maxLife = maxLife;
            this.color = color;
        }
    }
    private final List<ShootingStarStreak> shootingStars = new ArrayList<>();

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();

        // Initialize background stars
        for (int i = 0; i < NUM_BG_STARS; i++) {
            starX[i] = random.nextDouble() * 1400;
            starY[i] = random.nextDouble() * 700;
            starR[i] = random.nextDouble() * 2.8 + 1.0;
            starAlpha[i] = random.nextDouble() * 0.4 + 0.15;
        }

        // Bind canvas dimensions to rootPane
        if (rootPane != null) {
            astrolabeCanvas.widthProperty().bind(rootPane.prefWidthProperty());
            astrolabeCanvas.heightProperty().bind(rootPane.prefHeightProperty());
        }

        // Setup the Scanning Line Animation on Hologram Baseline
        setupHologramScanAnimation();

        // Build the 5 Celestial Orb Interactive Widgets
        buildCelestialOrbs();

        // Start Astrolabe & Depth-of-Field Canvas Animation Loop
        startAstrolabeAnimation();
    }

    private void setupHologramScanAnimation() {
        if (scanPulseLine != null) {
            // Animate scan pulse line across the 440px baseline
            TranslateTransition scanTransition = new TranslateTransition(Duration.seconds(2.2), scanPulseLine);
            scanTransition.setFromX(-50);
            scanTransition.setToX(350);
            scanTransition.setCycleCount(Animation.INDEFINITE);
            scanTransition.setAutoReverse(true);
            scanTransition.play();

            // Neon breathing glow on the baseline
            Timeline glowPulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(neonBaseLine.opacityProperty(), 0.7)),
                    new KeyFrame(Duration.seconds(1.2), new KeyValue(neonBaseLine.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(2.4), new KeyValue(neonBaseLine.opacityProperty(), 0.7))
            );
            glowPulse.setCycleCount(Animation.INDEFINITE);
            glowPulse.play();
        }
    }

    private void buildCelestialOrbs() {
        orbsContainer.getChildren().clear();

        for (int i = 0; i < CORES.length; i++) {
            final int index = i;
            CelestialCore core = CORES[i];

            StackPane orbPane = new StackPane();
            orbPane.setPrefSize(72, 72);
            orbPane.setMaxSize(72, 72);
            orbPane.setStyle("-fx-cursor: hand;");

            Canvas orbCanvas = new Canvas(72, 72);
            orbPane.getChildren().add(orbCanvas);

            // Draw initial orb core
            drawOrb(orbCanvas.getGraphicsContext2D(), core, false, 0);

            // Curved arc vertical offset (creating the astrolabe wheel curve)
            // Center orb (index 2) sits slightly higher, ends curve down
            double arcOffset = Math.sin((i / (double)(CORES.length - 1)) * Math.PI) * -16;
            orbPane.setTranslateY(arcOffset);

            // Hover interactions
            orbPane.setOnMouseEntered(e -> {
                activeCoreIndex = index;
                drawOrb(orbCanvas.getGraphicsContext2D(), core, true, 1.0);
                orbLoreLabel.setText("✦ " + core.name + " [" + core.title + "] — " + core.element + " ✦");
                // Increased font to 16px and injected the orb's specific color
                orbLoreLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                        + toRgbCode(core.glowColor) + "; -fx-effect: dropshadow(gaussian, black, 5, 0.8, 1, 1);");
            });

            orbPane.setOnMouseExited(e -> {
                drawOrb(orbCanvas.getGraphicsContext2D(), core, false, 0);
            });

            orbPane.setOnMouseClicked(e -> {
                activeCoreIndex = index;
                // Animate ripple pulse
                ScaleTransition st = new ScaleTransition(Duration.millis(250), orbPane);
                st.setFromX(1.0);
                st.setFromY(1.0);
                st.setToX(1.25);
                st.setToY(1.25);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();

                // Tint hologram baseline to match chosen orb faction!
                neonBaseLine.setStroke(core.primaryColor);
                neonBaseLine.setStyle("-fx-effect: dropshadow(gaussian, " + toRgbCode(core.glowColor) + ", 16, 0.85, 0, 0);");
                nameField.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-family: 'Verdana'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-border-width: 0; -fx-padding: 0 10 4 10; -fx-effect: dropshadow(gaussian, " + toRgbCode(core.primaryColor) + ", 15, 0.5, 0, 0);");
            });

            orbsContainer.getChildren().add(orbPane);
        }
    }

    private void drawOrb(GraphicsContext gc, CelestialCore core, boolean hovered, double pulseVal) {
        gc.clearRect(0, 0, 72, 72);
        double cx = 36;
        double cy = 36;
        double baseRadius = hovered ? 22 : 17;

        // Outer Corona Aura
        RadialGradient aura = new RadialGradient(
                0, 0, cx, cy, baseRadius + 14, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, core.glowColor.deriveColor(0, 1, 1, hovered ? 0.7 : 0.35)),
                new Stop(0.6, core.primaryColor.deriveColor(0, 1, 1, hovered ? 0.4 : 0.15)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(aura);
        gc.fillOval(cx - baseRadius - 14, cy - baseRadius - 14, (baseRadius + 14) * 2, (baseRadius + 14) * 2);

        // Core Body
        RadialGradient coreGrad = new RadialGradient(
                0, 0, cx - 2, cy - 2, baseRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, core.coreColor),
                new Stop(0.45, core.glowColor),
                new Stop(0.85, core.primaryColor),
                new Stop(1.0, core.primaryColor.darker())
        );
        gc.setFill(coreGrad);
        gc.fillOval(cx - baseRadius, cy - baseRadius, baseRadius * 2, baseRadius * 2);

        // Astrolabe Node Rim Ring
        gc.setStroke(hovered ? Color.WHITE : core.glowColor.deriveColor(0, 1, 1, 0.7));
        gc.setLineWidth(hovered ? 2.2 : 1.4);
        gc.strokeOval(cx - baseRadius - 3, cy - baseRadius - 3, (baseRadius + 3) * 2, (baseRadius + 3) * 2);

        // Inner radiant highlight
        gc.setFill(Color.rgb(255, 255, 255, hovered ? 0.9 : 0.6));
        gc.fillOval(cx - baseRadius * 0.4, cy - baseRadius * 0.4, baseRadius * 0.5, baseRadius * 0.5);
    }

    private void startAstrolabeAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                timeElapsed += 0.016;
                astrolabeAngle += 0.06; // Slow celestial drift
                renderAstrolabeCanvas();
            }
        };
        animationTimer.start();
    }

    private void renderAstrolabeCanvas() {
        if (astrolabeCanvas == null) return;
        GraphicsContext gc = astrolabeCanvas.getGraphicsContext2D();
        double w = astrolabeCanvas.getWidth();
        double h = astrolabeCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        // 1. Deep Space Base
        gc.setFill(Color.web("#020308"));
        gc.fillRect(0, 0, w, h);

        // 2. Cosmic Nebula Clouds
        renderNebulae(gc, w, h);

        // 3. Twinkling Stars
        renderStarfield(gc, w, h);

        // 4. Occasional Tiny Shooting Stars
        renderShootingStars(gc, w, h);

        // 6. Subtle Atmospheric Clouds
        renderSubtleClouds(gc, w, h);

        // 7. Distant Mountains & Observatory Silhouette
        renderMountainsAndObservatory(gc, w, h);

        // 8. Glowing Astrolabe Wheel & Foregrounds
        renderAstrolabeForeground(gc, w, h);
    }

    private void renderNebulae(GraphicsContext gc, double w, double h) {
        // Deep purple cosmic nebula (upper left)
        double pX = w * 0.28 + Math.sin(timeElapsed * 0.4) * 20;
        double pY = h * 0.28 + Math.cos(timeElapsed * 0.3) * 15;
        RadialGradient nebulaPurple = new RadialGradient(
                0, 0, pX, pY, w * 0.48, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(112, 26, 230, 0.16)),
                new Stop(0.45, Color.rgb(124, 58, 237, 0.07)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(nebulaPurple);
        gc.fillRect(0, 0, w, h);

        // Electric cyan/blue nebula (upper right)
        double cX = w * 0.72 + Math.cos(timeElapsed * 0.35) * 20;
        double cY = h * 0.24 + Math.sin(timeElapsed * 0.4) * 15;
        RadialGradient nebulaCyan = new RadialGradient(
                0, 0, cX, cY, w * 0.42, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 165, 233, 0.14)),
                new Stop(0.5, Color.rgb(56, 189, 248, 0.05)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(nebulaCyan);
        gc.fillRect(0, 0, w, h);

        // Subtle rose/crimson galactic dust wisp (mid-center)
        RadialGradient nebulaRose = new RadialGradient(
                0, 0, w * 0.50, h * 0.42, w * 0.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(225, 29, 72, 0.07)),
                new Stop(0.6, Color.rgb(147, 51, 234, 0.03)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(nebulaRose);
        gc.fillRect(0, 0, w, h);
    }

    private void renderStarfield(GraphicsContext gc, double w, double h) {
        for (int i = 0; i < NUM_BG_STARS; i++) {
            double alpha = starAlpha[i] + Math.sin(timeElapsed * 1.5 + i) * 0.15;
            alpha = Math.max(0.05, Math.min(0.85, alpha));

            // Faint halo
            gc.setFill(Color.rgb(180, 210, 255, alpha * 0.35));
            gc.fillOval(starX[i] - starR[i] * 1.6, starY[i] - starR[i] * 1.6, starR[i] * 3.2, starR[i] * 3.2);

            // Bright star center
            gc.setFill(Color.rgb(255, 255, 255, alpha));
            gc.fillOval(starX[i] - starR[i] * 0.5, starY[i] - starR[i] * 0.5, starR[i], starR[i]);

            // Subtle 4-point diffraction spike on prominent stars
            if (starR[i] > 2.6 && alpha > 0.4) {
                gc.setStroke(Color.rgb(224, 242, 254, alpha * 0.45));
                gc.setLineWidth(0.8);
                gc.strokeLine(starX[i] - 5, starY[i], starX[i] + 5, starY[i]);
                gc.strokeLine(starX[i], starY[i] - 5, starX[i], starY[i] + 5);
            }
        }
    }

    private void renderShootingStars(GraphicsContext gc, double w, double h) {
        if (random.nextDouble() < 0.022) {
            double startX = random.nextDouble() * (w * 0.85);
            double startY = random.nextDouble() * (h * 0.35);
            double speed = random.nextDouble() * 9.0 + 10.0;
            double angle = Math.toRadians(random.nextDouble() * 20.0 + 25.0);
            Color col = random.nextBoolean() ? Color.web("#e0f2fe") : Color.web("#fef08a");
            shootingStars.add(new ShootingStarStreak(
                    startX, startY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    random.nextDouble() * 32.0 + 24.0, // length of tiny shooting star
                    random.nextDouble() * 20.0 + 16.0, // lifespan
                    col
            ));
        }

        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStarStreak ss = shootingStars.get(i);
            ss.x += ss.vx;
            ss.y += ss.vy;
            ss.life--;
            if (ss.life <= 0 || ss.x > w + 50 || ss.y > h + 50) {
                shootingStars.remove(i);
                continue;
            }
            double alpha = Math.max(0.0, ss.life / ss.maxLife);
            gc.setStroke(Color.color(ss.color.getRed(), ss.color.getGreen(), ss.color.getBlue(), alpha * 0.9));
            gc.setLineWidth(1.6);
            gc.strokeLine(ss.x, ss.y, ss.x - ss.vx * (ss.len / 12.0), ss.y - ss.vy * (ss.len / 12.0));

            // Head pinpoint starlight
            gc.setFill(Color.color(1.0, 1.0, 1.0, alpha));
            gc.fillOval(ss.x - 1, ss.y - 1, 2.5, 2.5);
        }
    }

    private void renderSubtleClouds(GraphicsContext gc, double w, double h) {
        double drift1 = (timeElapsed * 8.0) % (w + 500) - 250;
        double drift2 = (timeElapsed * 5.0) % (w + 600) - 300;

        drawCloudPuff(gc, drift1, h * 0.56, 260, 48, Color.rgb(25, 38, 70, 0.10));
        drawCloudPuff(gc, (drift1 + w * 0.55) % (w + 500) - 250, h * 0.62, 320, 56, Color.rgb(18, 30, 60, 0.12));
        drawCloudPuff(gc, drift2, h * 0.68, 360, 64, Color.rgb(12, 22, 48, 0.14));
        drawCloudPuff(gc, (drift2 + w * 0.65) % (w + 600) - 300, h * 0.72, 300, 52, Color.rgb(10, 18, 42, 0.15));
    }

    private void drawCloudPuff(GraphicsContext gc, double cx, double cy, double rx, double ry, Color col) {
        RadialGradient cloudGrad = new RadialGradient(
                0, 0, cx, cy, rx, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, col),
                new Stop(0.65, Color.color(col.getRed(), col.getGreen(), col.getBlue(), col.getOpacity() * 0.4)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(cloudGrad);
        gc.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    private void renderMountainsAndObservatory(GraphicsContext gc, double w, double h) {
        // ── 1. Far Back Mountain Ridge ──
        double[] backX = {
                0, 0, w * 0.08, w * 0.17, w * 0.27, w * 0.38, w * 0.49,
                w * 0.60, w * 0.71, w * 0.82, w * 0.91, w, w
        };
        double[] backY = {
                h, h * 0.76, h * 0.70, h * 0.74, h * 0.67, h * 0.73, h * 0.68,
                h * 0.74, h * 0.68, h * 0.64, h * 0.70, h * 0.67, h
        };
        gc.setFill(Color.web("#060e22"));
        gc.fillPolygon(backX, backY, backX.length);

        // Faint celestial rim lighting on the back mountain ridge
        gc.setStroke(Color.rgb(56, 189, 248, 0.22));
        gc.setLineWidth(1.4);
        for (int i = 1; i < backX.length - 2; i++) {
            gc.strokeLine(backX[i], backY[i], backX[i + 1], backY[i + 1]);
        }

        // ── 2. Observatory Silhouette on Peak (w * 0.82, h * 0.64) ──
        double obsPeakX = w * 0.82;
        double obsPeakY = h * 0.64;
        drawObservatory(gc, obsPeakX - 18, obsPeakY - 32);

        // ── 3. Near / Foreground Mountain Ridge ──
        double[] frontX = {
                0, 0, w * 0.10, w * 0.21, w * 0.33, w * 0.46, w * 0.58,
                w * 0.69, w * 0.79, w * 0.89, w * 0.96, w, w
        };
        double[] frontY = {
                h, h * 0.84, h * 0.78, h * 0.82, h * 0.75, h * 0.81, h * 0.74,
                h * 0.79, h * 0.73, h * 0.78, h * 0.74, h * 0.76, h
        };
        gc.setFill(Color.web("#02040b"));
        gc.fillPolygon(frontX, frontY, frontX.length);

        // Subtle crest stroke
        gc.setStroke(Color.rgb(30, 41, 59, 0.45));
        gc.setLineWidth(1.0);
        for (int i = 1; i < frontX.length - 2; i++) {
            gc.strokeLine(frontX[i], frontY[i], frontX[i + 1], frontY[i + 1]);
        }
    }

    private void drawObservatory(GraphicsContext gc, double ox, double oy) {
        // Base cylindrical building
        gc.setFill(Color.web("#02040b"));
        gc.fillRect(ox, oy + 15, 38, 20);

        // Main observatory dome
        gc.fillArc(ox + 3, oy, 32, 28, 0, 180, javafx.scene.shape.ArcType.ROUND);

        // Slit / observation hatch opening (angled toward stars)
        gc.setFill(Color.web("#020308"));
        gc.fillRect(ox + 12, oy + 2, 8, 14);

        // Telescope tube extending from dome toward constellations
        gc.setStroke(Color.web("#0f172a"));
        gc.setLineWidth(4.0);
        gc.strokeLine(ox + 16, oy + 10, ox + 6, oy - 10);
        // Telescope lens rim
        gc.setStroke(Color.web("#38bdf8", 0.7));
        gc.setLineWidth(1.5);
        gc.strokeLine(ox + 4, oy - 12, ox + 9, oy - 8);

        // Catwalk / observation railing around dome
        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(1.2);
        gc.strokeLine(ox - 3, oy + 16, ox + 41, oy + 16);
        for (int r = 0; r <= 44; r += 7) {
            gc.strokeLine(ox - 3 + r, oy + 16, ox - 3 + r, oy + 19);
        }

        // Antenna mast with blinking red warning beacon
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(1.0);
        gc.strokeLine(ox + 42, oy + 16, ox + 42, oy - 12);
        double beaconBlink = 0.5 + 0.5 * Math.sin(timeElapsed * 4.5);
        gc.setFill(Color.color(1.0, 0.2, 0.2, beaconBlink));
        gc.fillOval(ox + 40.5, oy - 14, 3, 3);

        // Warm observation window glow inside observatory
        gc.setFill(Color.rgb(251, 191, 36, 0.8));
        gc.fillRect(ox + 14, oy + 20, 10, 5);
    }

    private void renderAstrolabeForeground(GraphicsContext gc, double w, double h) {
        // Glowing Astrolabe / Star Wheel in Foreground Center
        double dialCenterX = w / 2.0;
        double dialCenterY = h * 0.70; // Positioned behind the orbs arc and lower section
        double dialRadius = 260.0;

        // 1. Dial Outer Halo
        RadialGradient dialHalo = new RadialGradient(
                0, 0, dialCenterX, dialCenterY, dialRadius * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(56, 189, 248, 0.26)),
                new Stop(0.50, Color.rgb(14, 165, 233, 0.12)),
                new Stop(0.85, Color.rgb(99, 102, 241, 0.05)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(dialHalo);
        gc.fillOval(dialCenterX - dialRadius * 1.35, dialCenterY - dialRadius * 1.35, dialRadius * 2.7, dialRadius * 2.7);

        // 2. Frosted Blur Backdrop Plate (softly diffuses and blurs the background behind the wheel)
        RadialGradient frostedPlate = new RadialGradient(
                0, 0, dialCenterX, dialCenterY, dialRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(18, 38, 76, 0.72)),
                new Stop(0.45, Color.rgb(14, 30, 62, 0.64)),
                new Stop(0.80, Color.rgb(10, 22, 48, 0.52)),
                new Stop(1.0, Color.rgb(6, 14, 34, 0.35))
        );
        gc.setFill(frostedPlate);
        gc.fillOval(dialCenterX - dialRadius, dialCenterY - dialRadius, dialRadius * 2, dialRadius * 2);

        // 3. Enlightened Internal Core Luminance (pulsating ethereal starlight)
        double pulseGlow = 0.22 + 0.04 * Math.sin(timeElapsed * 1.8);
        RadialGradient internalEnlighten = new RadialGradient(
                0, 0, dialCenterX, dialCenterY, dialRadius * 0.78, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(56, 189, 248, pulseGlow)),
                new Stop(0.35, Color.rgb(99, 102, 241, pulseGlow * 0.65)),
                new Stop(0.70, Color.rgb(14, 165, 233, pulseGlow * 0.30)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(internalEnlighten);
        gc.fillOval(dialCenterX - dialRadius * 0.78, dialCenterY - dialRadius * 0.78, dialRadius * 1.56, dialRadius * 1.56);

        // Concentric Astrolabe Rings
        gc.setLineWidth(1.4);
        gc.setStroke(Color.rgb(56, 189, 248, 0.35));
        gc.strokeOval(dialCenterX - dialRadius, dialCenterY - dialRadius, dialRadius * 2, dialRadius * 2);

        gc.setLineWidth(1.0);
        gc.setStroke(Color.rgb(125, 211, 252, 0.25));
        gc.strokeOval(dialCenterX - (dialRadius - 28), dialCenterY - (dialRadius - 28), (dialRadius - 28) * 2, (dialRadius - 28) * 2);

        gc.setLineWidth(1.8);
        gc.setStroke(Color.rgb(14, 165, 233, 0.45));
        gc.strokeOval(dialCenterX - (dialRadius - 65), dialCenterY - (dialRadius - 65), (dialRadius - 65) * 2, (dialRadius - 65) * 2);

        gc.setLineWidth(0.8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.2));
        gc.strokeOval(dialCenterX - (dialRadius - 130), dialCenterY - (dialRadius - 130), (dialRadius - 130) * 2, (dialRadius - 130) * 2);

        // Astrolabe Rotating Radial Markers & Zodiac Rays
        int numRays = 24;
        for (int i = 0; i < numRays; i++) {
            double angleRad = Math.toRadians(astrolabeAngle + (i * (360.0 / numRays)));
            double innerR = (i % 2 == 0) ? dialRadius - 65 : dialRadius - 28;
            double outerR = dialRadius;

            double x1 = dialCenterX + Math.cos(angleRad) * innerR;
            double y1 = dialCenterY + Math.sin(angleRad) * innerR;
            double x2 = dialCenterX + Math.cos(angleRad) * outerR;
            double y2 = dialCenterY + Math.sin(angleRad) * outerR;

            gc.setStroke(Color.rgb(125, 211, 252, (i % 2 == 0) ? 0.35 : 0.18));
            gc.strokeLine(x1, y1, x2, y2);
        }

        // Constellation Lines linking the Orbs along the Astrolabe Wheel Arc
        gc.setLineWidth(1.2);
        gc.setStroke(Color.rgb(56, 189, 248, 0.5));
        double startX = dialCenterX - 180;
        double startY = 460;
        for (int i = 0; i < 4; i++) {
            double segX1 = startX + i * 90;
            double segY1 = startY + Math.sin((i / 4.0) * Math.PI) * -18;
            double segX2 = startX + (i + 1) * 90;
            double segY2 = startY + Math.sin(((i + 1) / 4.0) * Math.PI) * -18;

            gc.strokeLine(segX1, segY1, segX2, segY2);

            // Small constellation stars at junctions
            gc.setFill(Color.rgb(255, 255, 255, 0.75));
            gc.fillOval(segX1 - 2, segY1 - 2, 4, 4);
        }
        gc.fillOval(startX + 4 * 90 - 2, startY - 2, 4, 4);
    }

    private String toRgbCode(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed() * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue() * 255));
    }

    @FXML
    private void checkPlayer() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            neonBaseLine.setStroke(Color.RED);
            neonBaseLine.setStyle("-fx-effect: dropshadow(gaussian, #ef4444, 18, 0.9, 0, 0);");
            hologramHint.setText("⚠ Please identify yourself, Stargazer, before initiating the portal ⚠");
            // Increased font to 14px for the error text
            hologramHint.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ff8888; -fx-effect: dropshadow(gaussian, black, 4, 0.8, 1, 1);");
            return;
        }
// ... rest of the method stays exactly the same
// ... rest of the method

        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }

            Player player = playerDAO.checkOrCreatePlayer(name);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("WelcomeView.fxml"));
            Parent root = loader.load();

            WelcomeController controller = loader.getController();
            controller.setPlayer(player);

            root.setOpacity(0);
            Stage stage = (Stage) nameField.getScene().getWindow();
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
