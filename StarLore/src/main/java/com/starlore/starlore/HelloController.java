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
            new CelestialCore("Pulsar Blue", "Starlight Pulsar", "Electric Magnetar Pulse",
                    Color.web("#0ea5e9"), Color.web("#38bdf8"), Color.web("#ffffff")),
            new CelestialCore("Toxic Nebula", "Bio-Nebular Core", "Emerald Cosmic Genesis",
                    Color.web("#10b981"), Color.web("#34d399"), Color.web("#a7f3d0")),
            new CelestialCore("Void Amethyst", "Dark Matter Void", "Gravitational Singularity",
                    Color.web("#8b5cf6"), Color.web("#c084fc"), Color.web("#f3e8ff")),
            new CelestialCore("Glacial Nova", "Supernova Diamond", "Pristine Astral Brilliance",
                    Color.web("#38bdf8"), Color.web("#bae6fd"), Color.web("#ffffff"))
    };

    private int activeCoreIndex = 1; // Default to Pulsar Blue
    private double[] orbPulse = new double[5];

    // Background Stars for Depth of Field
    private static final int NUM_BG_STARS = 120;
    private double[] starX = new double[NUM_BG_STARS];
    private double[] starY = new double[NUM_BG_STARS];
    private double[] starR = new double[NUM_BG_STARS];
    private double[] starAlpha = new double[NUM_BG_STARS];

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
                orbLoreLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                        + toRgbCode(core.glowColor) + "; -fx-effect: dropshadow(gaussian, " + toRgbCode(core.primaryColor) + ", 12, 0.7, 0, 0);");
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

        // 1. Deep Space Gradient
        gc.setFill(Color.web("#050819"));
        gc.fillRect(0, 0, w, h);

        // 2. Depth of Field Stars (Soft blur simulation)
        for (int i = 0; i < NUM_BG_STARS; i++) {
            double alpha = starAlpha[i] + Math.sin(timeElapsed * 1.5 + i) * 0.1;
            alpha = Math.max(0.05, Math.min(0.6, alpha));
            // Soft halo
            gc.setFill(Color.rgb(180, 210, 255, alpha * 0.4));
            gc.fillOval(starX[i] - starR[i] * 1.5, starY[i] - starR[i] * 1.5, starR[i] * 3, starR[i] * 3);
            // Pinpoint
            gc.setFill(Color.rgb(255, 255, 255, alpha));
            gc.fillOval(starX[i] - starR[i] * 0.5, starY[i] - starR[i] * 0.5, starR[i], starR[i]);
        }

        // 3. Faint Distant Moon in Upper Right
        double moonX = w * 0.82;
        double moonY = 130;
        double moonR = 65;
        RadialGradient moonGlow = new RadialGradient(
                0, 0, moonX, moonY, moonR * 2.2, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(125, 211, 252, 0.2)),
                new Stop(0.5, Color.rgb(56, 189, 248, 0.08)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(moonGlow);
        gc.fillOval(moonX - moonR * 2.2, moonY - moonR * 2.2, moonR * 4.4, moonR * 4.4);

        gc.setFill(Color.rgb(224, 242, 254, 0.25));
        gc.fillOval(moonX - moonR, moonY - moonR, moonR * 2, moonR * 2);

        // 4. Glowing Astrolabe / Star Wheel in Foreground Center
        double dialCenterX = w / 2.0;
        double dialCenterY = h * 0.70; // Positioned behind the orbs arc and lower section
        double dialRadius = 260.0;

        // Dial Glow
        RadialGradient dialHalo = new RadialGradient(
                0, 0, dialCenterX, dialCenterY, dialRadius * 1.25, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 165, 233, 0.18)),
                new Stop(0.65, Color.rgb(56, 189, 248, 0.06)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(dialHalo);
        gc.fillOval(dialCenterX - dialRadius * 1.25, dialCenterY - dialRadius * 1.25, dialRadius * 2.5, dialRadius * 2.5);

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
            // Flash red warning pulse on baseline if empty
            neonBaseLine.setStroke(Color.RED);
            neonBaseLine.setStyle("-fx-effect: dropshadow(gaussian, #ef4444, 18, 0.9, 0, 0);");
            hologramHint.setText("⚠ Please identify yourself, Stargazer, before initiating the portal ⚠");
            hologramHint.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 11px; -fx-text-fill: #f87171;");
            return;
        }

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
