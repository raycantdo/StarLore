package com.starlore.starlore;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

/**
 * DuelController manages:
 * 1. The interactive celestial star atlas map matching the real night sky, with 4-way scrolling,
 *    dark unlit constellations, enlightened constellations, and yellow pointer indicator.
 * 2. Play with AI mode (race against the AI).
 * 3. Play with Friends mode (turn-based 2-player local pass-and-play).
 */
public class DuelController {

    // ─── FXML Map Overlay Elements ──────────────────────────────
    @FXML private StackPane rootStackPane;
    @FXML private StackPane mapOverlay;
    @FXML private Canvas mapCanvas;
    @FXML private Label dustHudLabel;
    @FXML private Button compassBtn;
    @FXML private Button hubBtn;
    @FXML private Label constellationTitleLabel;
    @FXML private Label constellationStatusBadge;
    @FXML private Label constellationDescLabel;
    @FXML private Button friendsModeBtn;
    @FXML private Button aiModeBtn;

    // ─── FXML Duel Game Elements ────────────────────────────────
    @FXML private BorderPane gamePane;
    @FXML private Label player1TagLabel;
    @FXML private Label playerScoreLabel;
    @FXML private Label duelConstellationNameLabel;
    @FXML private Label timerLabel;
    @FXML private Label turnIndicatorLabel;
    @FXML private VBox opponentHeader;
    @FXML private Label opponentTagLabel;
    @FXML private Label aiScoreLabel;
    @FXML private Canvas playerCanvas;
    @FXML private VBox versusDivider;
    @FXML private StackPane opponentBoard;
    @FXML private Canvas aiCanvas;

    // ─── FXML Result Overlay Elements ───────────────────────────
    @FXML private StackPane resultOverlay;
    @FXML private Label resultLabel;
    @FXML private Label resultScoreLabel;

    private Player currentPlayer;
    private final PlayerDAO playerDAO = new PlayerDAO();

    // ─── Map Geometry & State ───────────────────────────────────
    private static final double WORLD_W = 2500.0;
    private static final double WORLD_H = 1800.0;
    private double mapX = -700.0;
    private double mapY = -420.0;
    private double dragStartX, dragStartY;
    private boolean isDragging = false;
    private double animTime = 0.0;
    private AnimationTimer mapAnimTimer;

    // Stable background star field (750 stars)
    private static final int NUM_BG_STARS = 750;
    private final double[] bgStarX = new double[NUM_BG_STARS];
    private final double[] bgStarY = new double[NUM_BG_STARS];
    private final double[] bgStarR = new double[NUM_BG_STARS];
    private final double[] bgStarTwinkle = new double[NUM_BG_STARS];

    // Constellation definitions
    public static class ConstellationDef {
        public final String name;
        public final String title;
        public final String desc;
        public final double cx, cy;
        public final double[][] stars;   // relative coordinates to (cx, cy)
        public final int[][] links;      // star index pairs
        public final double[][] boundary; // polygon relative vertices

        public final double minStarX, maxStarX, minStarY, maxStarY;

        public ConstellationDef(String name, String title, String desc, double cx, double cy,
                                double[][] stars, int[][] links, double[][] boundary) {
            this.name = name;
            this.title = title;
            this.desc = desc;
            this.cx = cx;
            this.cy = cy;
            this.stars = stars;
            this.links = links;
            this.boundary = boundary;

            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (double[] s : stars) {
                if (s[0] < minX) minX = s[0];
                if (s[0] > maxX) maxX = s[0];
                if (s[1] < minY) minY = s[1];
                if (s[1] > maxY) maxY = s[1];
            }
            this.minStarX = minX;
            this.maxStarX = maxX;
            this.minStarY = minY;
            this.maxStarY = maxY;
        }
    }

    private final List<ConstellationDef> constellations = new ArrayList<>();
    private ConstellationDef selectedDef;
    private ConstellationDef currentDuelDef;

    // ─── Game Loop & Duel State ─────────────────────────────────
    private boolean isFriendMode = false;
    private boolean isPlayer1Turn = true;
    private boolean duelActive = false;
    private int selectedStarIdx = -1;
    private int player1Score = 0;
    private int player2OrAiScore = 0;
    private int timeLeft = 30;

    private final List<int[]> p1Links = new ArrayList<>();
    private final List<int[]> p2OrAiLinks = new ArrayList<>();
    private final Map<int[], Color> friendLinkColors = new HashMap<>();

    private Timeline duelTimer;
    private Timeline aiTimer;
    private final Random random = new Random();
    // ─── Networked Friends Mode ──────────────────────────────────
    private NetworkManager network;
    private boolean isHost;
    private boolean isNetworked = false;

    // ─── Initialization ─────────────────────────────────────────

    @FXML
    public void initialize() {
        initStarfield();
        initConstellations();

        // Default selected: Aries (as highlighted in user's reference image!)
        selectedDef = findConstellation("ARIES");
        recenterOnSelected();

        setupMapInteractions();
        setupCanvasBinding();
        startMapAnimation();
        updateSelectionCard();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
        updateHud();
        updateSelectionCard();
        drawMap();
    }

    private void updateHud() {
        if (dustHudLabel != null) {
            int dust = (currentPlayer != null) ? currentPlayer.getTotalStarDust() : 0;
            dustHudLabel.setText(dust + " DUST");
        }
    }

    private void setupCanvasBinding() {
        if (rootStackPane != null && mapCanvas != null) {
            mapCanvas.widthProperty().bind(rootStackPane.widthProperty());
            mapCanvas.heightProperty().bind(rootStackPane.heightProperty());
            mapCanvas.widthProperty().addListener((obs, o, n) -> drawMap());
            mapCanvas.heightProperty().addListener((obs, o, n) -> drawMap());
        }
        if (playerCanvas != null) {
            playerCanvas.setOnMouseClicked(this::onPlayerCanvasClick);
        }
    }

    private void initStarfield() {
        Random rnd = new Random(42); // fixed seed for stable layout
        for (int i = 0; i < NUM_BG_STARS; i++) {
            bgStarX[i] = rnd.nextDouble() * WORLD_W;
            bgStarY[i] = rnd.nextDouble() * WORLD_H;
            bgStarR[i] = (i % 25 == 0) ? 2.8 : (i % 8 == 0) ? 1.8 : 1.0;
            bgStarTwinkle[i] = rnd.nextDouble() * Math.PI * 2;
        }
    }

    private void initConstellations() {
        constellations.clear();

        // 1. ARIES (Center - Golden Ram)
        constellations.add(new ConstellationDef(
                "ARIES", "THE RAM",
                "The golden-fleeced ram of celestial myth that carried Phrixus to Colchis.",
                1200, 780,
                new double[][]{{-40, -15}, {0, 10}, {40, -5}, {85, 30}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}},
                new double[][]{{-130, -90}, {140, -90}, {140, 95}, {-130, 95}}
        ));

        // 2. TAURUS (The Bull - Left of Aries)
        constellations.add(new ConstellationDef(
                "TAURUS", "THE BULL",
                "The great celestial bull confronting Orion with the blazing eye of Aldebaran.",
                840, 840,
                new double[][]{{-45, -50}, {-10, -10}, {45, 10}, {80, 50}, {-20, 40}, {20, 60}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {1, 4}, {4, 5}},
                new double[][]{{-130, -95}, {130, -95}, {130, 105}, {-130, 105}}
        ));

        // 3. ORION (The Hunter - Far left)
        constellations.add(new ConstellationDef(
                "ORION", "THE HUNTER",
                "The titan hunter bearing the belt of Alnitak, Alnilam, Mintaka and bright Rigel.",
                460, 960,
                new double[][]{{-40, -90}, {40, -90}, {-25, 0}, {0, 0}, {25, 0}, {-45, 95}, {45, 95}},
                new int[][]{{0, 2}, {1, 4}, {2, 3}, {3, 4}, {2, 5}, {4, 6}},
                new double[][]{{-120, -130}, {120, -130}, {120, 135}, {-120, 135}}
        ));

        // 4. PERSEUS (Above Aries)
        constellations.add(new ConstellationDef(
                "PERSEUS", "THE HERO",
                "The brave slayer of Medusa and rescuer of Andromeda, armed with the Harpe blade.",
                1150, 460,
                new double[][]{{-20, -70}, {25, -40}, {0, 0}, {-45, 45}, {40, 50}, {0, 80}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {2, 4}, {4, 5}},
                new double[][]{{-125, -100}, {125, -100}, {125, 110}, {-125, 110}}
        ));

        // 5. CASSIOPEIA (Top - Queen)
        constellations.add(new ConstellationDef(
                "CASSIOPEIA", "THE QUEEN",
                "The proud Queen of Ethiopia seated upon her iconic celestial W-throne.",
                1460, 240,
                new double[][]{{-70, 30}, {-35, -30}, {0, 20}, {35, -30}, {70, 25}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}},
                new double[][]{{-120, -85}, {120, -85}, {120, 90}, {-120, 90}}
        ));

        // 6. TRIANGULUM (Between Perseus and Aries)
        constellations.add(new ConstellationDef(
                "TRIANGULUM", "THE TRIANGLE",
                "A pristine geometric trinity celebrated by Ptolemy and ancient stargazers.",
                1400, 580,
                new double[][]{{-40, 25}, {40, 25}, {0, -35}},
                new int[][]{{0, 1}, {1, 2}, {2, 0}},
                new double[][]{{-95, -75}, {95, -75}, {95, 80}, {-95, 80}}
        ));

        // 7. ANDROMEDA (Upper right)
        constellations.add(new ConstellationDef(
                "ANDROMEDA", "THE MAIDEN",
                "The chained princess of the night sky, home to the great spiral galaxy.",
                1780, 430,
                new double[][]{{-60, -40}, {-15, -10}, {35, 20}, {75, 50}, {-20, 45}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {1, 4}},
                new double[][]{{-130, -90}, {130, -90}, {130, 95}, {-130, 95}}
        ));

        // 8. PISCES (Right of Aries)
        constellations.add(new ConstellationDef(
                "PISCES", "THE FISHES",
                "Two mythical fishes bound together by starry celestial ribbons.",
                1680, 780,
                new double[][]{{-50, 45}, {0, 25}, {50, 45}, {35, -35}, {-35, -35}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}},
                new double[][]{{-130, -95}, {130, -95}, {130, 95}, {-130, 95}}
        ));

        // 9. CETUS (Lower right - Sea Monster)
        constellations.add(new ConstellationDef(
                "CETUS", "THE SEA MONSTER",
                "The leviathan of the deep celestial ocean with the pulsating beacon of Mira.",
                1600, 1120,
                new double[][]{{-70, -30}, {-30, -45}, {20, -20}, {65, 10}, {30, 50}, {-20, 40}, {-60, 20}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 0}},
                new double[][]{{-135, -100}, {135, -100}, {135, 105}, {-135, 105}}
        ));

        // 10. ERIDANUS (River - Lower center)
        constellations.add(new ConstellationDef(
                "ERIDANUS", "THE RIVER",
                "The winding celestial river that flows past Orion down to radiant Achernar.",
                1100, 1200,
                new double[][]{{-60, -70}, {-25, -35}, {15, -10}, {-20, 30}, {25, 65}, {70, 90}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}},
                new double[][]{{-130, -105}, {130, -105}, {130, 115}, {-130, 115}}
        ));

        // 11. LEPUS (Below Orion)
        constellations.add(new ConstellationDef(
                "LEPUS", "THE HARE",
                "The nimble celestial hare darting quietly across the southern sky.",
                480, 1260,
                new double[][]{{-40, -20}, {10, -35}, {45, 0}, {15, 35}, {-35, 25}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}},
                new double[][]{{-110, -85}, {110, -85}, {110, 85}, {-110, 85}}
        ));

        // 12. URSA MAJOR (Upper left - Great Bear)
        constellations.add(new ConstellationDef(
                "URSA MAJOR", "THE GREAT BEAR",
                "The iconic Big Dipper guiding navigators toward the North Pole Star.",
                440, 350,
                new double[][]{{-65, 20}, {-25, 10}, {15, 20}, {50, 35}, {65, 80}, {25, 95}, {-15, 80}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 2}},
                new double[][]{{-130, -95}, {130, -95}, {130, 120}, {-130, 120}}
        ));

        // 13. LEO (The Lion - Far right)
        constellations.add(new ConstellationDef(
                "LEO", "THE LION",
                "The majestic Nemean Lion crowned with the radiant king star Regulus.",
                2080, 960,
                new double[][]{{-50, 20}, {-10, -20}, {30, 0}, {55, 45}, {20, 80}, {-40, 70}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}, {1, 3}},
                new double[][]{{-130, -95}, {130, -95}, {130, 110}, {-130, 110}}
        ));

        // 14. SCORPIUS (Bottom right)
        constellations.add(new ConstellationDef(
                "SCORPIUS", "THE SCORPION",
                "The fierce arachnid armed with a stinger and the beating crimson heart of Antares.",
                1950, 1440,
                new double[][]{{-40, -80}, {0, -60}, {-20, -20}, {0, 20}, {30, 50}, {10, 85}, {-20, 110}, {-50, 130}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}},
                new double[][]{{-120, -110}, {120, -110}, {120, 150}, {-120, 150}}
        ));
    }

    private ConstellationDef findConstellation(String name) {
        for (ConstellationDef c : constellations) {
            if (c.name.equalsIgnoreCase(name)) return c;
        }
        return constellations.get(0);
    }

    // ─── 4-Way Panning & Mouse Handling ─────────────────────────

    private void setupMapInteractions() {
        mapCanvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            isDragging = false;
        });

        mapCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.abs(dx) + Math.abs(dy) > 4) {
                isDragging = true;
            }
            double viewW = mapCanvas.getWidth() > 0 ? mapCanvas.getWidth() : 1000;
            double viewH = mapCanvas.getHeight() > 0 ? mapCanvas.getHeight() : 700;
            mapX = clamp(mapX + dx, viewW - WORLD_W, 0);
            mapY = clamp(mapY + dy, viewH - WORLD_H, 0);
            dragStartX = e.getX();
            dragStartY = e.getY();
            drawMap();
        });

        mapCanvas.setOnMouseReleased(e -> {
            if (!isDragging) {
                handleMapClick(e.getX(), e.getY());
            }
        });
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private void handleMapClick(double sx, double sy) {
        double worldX = sx - mapX;
        double worldY = sy - mapY;

        ConstellationDef closest = null;
        double minDstSq = Double.MAX_VALUE;

        for (ConstellationDef c : constellations) {
            double dstSq = Math.pow(worldX - c.cx, 2) + Math.pow(worldY - c.cy, 2);
            if (dstSq < minDstSq) {
                minDstSq = dstSq;
                closest = c;
            }
        }

        // Selection radius: ~220px
        if (closest != null && minDstSq < 48400) {
            selectedDef = closest;
            updateSelectionCard();
            drawMap();
        }
    }

    @FXML
    private void recenterOnSelected() {
        if (selectedDef == null) return;
        double viewW = mapCanvas.getWidth() > 0 ? mapCanvas.getWidth() : 1000;
        double viewH = mapCanvas.getHeight() > 0 ? mapCanvas.getHeight() : 700;
        mapX = clamp(viewW / 2.0 - selectedDef.cx, viewW - WORLD_W, 0);
        mapY = clamp(viewH / 2.0 - selectedDef.cy, viewH - WORLD_H, 0);
        drawMap();
    }

    private void updateSelectionCard() {
        if (selectedDef == null) return;
        constellationTitleLabel.setText("✦ " + selectedDef.name + " — " + selectedDef.title + " ✦");
        constellationDescLabel.setText(selectedDef.desc);

        boolean enlightened = isConstellationEnlightened(selectedDef.name);
        if (enlightened) {
            constellationStatusBadge.setText("✦ ENLIGHTENED ✦");
            constellationStatusBadge.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #fde047; -fx-background-color: rgba(234, 179, 8, 0.25); -fx-padding: 3 10; -fx-background-radius: 10; -fx-border-color: #facc15; -fx-border-radius: 10; -fx-border-width: 1;");
        } else {
            constellationStatusBadge.setText("🌑 UNEXPLORED");
            constellationStatusBadge.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-background-color: rgba(148, 163, 184, 0.2); -fx-padding: 3 10; -fx-background-radius: 10;");
        }
    }

    private boolean isConstellationEnlightened(String name) {
        if (currentPlayer != null) {
            return currentPlayer.isConstellationEnlightened(name);
        }
        return false;
    }

    // ─── Celestial Map Canvas Rendering ─────────────────────────

    private void startMapAnimation() {
        mapAnimTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                animTime += 0.025;
                if (mapOverlay.isVisible()) {
                    drawMap();
                }
            }
        };
        mapAnimTimer.start();
    }

    private void drawMap() {
        if (mapCanvas == null) return;
        GraphicsContext g = mapCanvas.getGraphicsContext2D();
        double w = mapCanvas.getWidth() > 0 ? mapCanvas.getWidth() : 1000;
        double h = mapCanvas.getHeight() > 0 ? mapCanvas.getHeight() : 700;

        // Base deep celestial navy background
        g.setFill(Color.web("#020308"));
        g.fillRect(0, 0, w, h);

        // Radiant sky gradient
        RadialGradient skyGlow = new RadialGradient(
                0, 0, w / 2.0, h / 2.0, w * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#051642")),
                new Stop(0.65, Color.web("#020924")),
                new Stop(1.0, Color.web("#020308"))
        );
        g.setFill(skyGlow);
        g.fillRect(0, 0, w, h);

        g.save();
        g.translate(mapX, mapY);

        // Draw astronomical coordinate grid
        g.setStroke(Color.color(0.2, 0.5, 0.95, 0.14));
        g.setLineWidth(1.0);
        for (double x = 0; x <= WORLD_W; x += 150) {
            g.strokeLine(x, 0, x, WORLD_H);
        }
        for (double y = 0; y <= WORLD_H; y += 150) {
            g.strokeLine(0, y, WORLD_W, y);
        }

        // Draw background stars with realistic twinkle
        for (int i = 0; i < NUM_BG_STARS; i++) {
            double alpha = 0.35 + 0.35 * Math.sin(animTime * 1.8 + bgStarTwinkle[i]);
            alpha = Math.max(0.1, Math.min(0.9, alpha));
            double r = bgStarR[i];
            if (r > 2.0) {
                g.setFill(Color.color(0.95, 0.9, 0.6, alpha));
            } else {
                g.setFill(Color.color(0.65, 0.85, 1.0, alpha));
            }
            g.fillOval(bgStarX[i] - r / 2.0, bgStarY[i] - r / 2.0, r, r);
        }

        // Render each constellation
        for (ConstellationDef c : constellations) {
            drawConstellationOnMap(g, c);
        }

        // Render pointer arrow over selected constellation (safely above its top-most star)
        if (selectedDef != null) {
            drawPointerArrow(g, selectedDef.cx, selectedDef.cy + selectedDef.minStarY);
        }

        g.restore();
    }

    private void drawConstellationOnMap(GraphicsContext g, ConstellationDef c) {
        boolean isSelected = (c == selectedDef);
        boolean isEnlightened = isConstellationEnlightened(c.name);

        // Draw IAU Sector Boundaries
        if (c.boundary != null && c.boundary.length > 2) {
            double[] polyX = new double[c.boundary.length];
            double[] polyY = new double[c.boundary.length];
            for (int i = 0; i < c.boundary.length; i++) {
                polyX[i] = c.cx + c.boundary[i][0];
                polyY[i] = c.cy + c.boundary[i][1];
            }

            if (isSelected) {
                // Glowing golden boundary for selected constellation
                g.setStroke(Color.web("#fde047", 0.75));
                g.setLineWidth(2.2);
                g.strokePolygon(polyX, polyY, c.boundary.length);
                g.setFill(Color.color(0.1, 0.4, 0.9, 0.08));
                g.fillPolygon(polyX, polyY, c.boundary.length);
            } else if (isEnlightened) {
                // Soft golden starlight boundary
                g.setStroke(Color.web("#fbbf24", 0.5));
                g.setLineWidth(1.5);
                g.strokePolygon(polyX, polyY, c.boundary.length);
            } else {
                // Standard cyan celestial sector boundary
                g.setStroke(Color.web("#2563eb", 0.45));
                g.setLineWidth(1.2);
                g.strokePolygon(polyX, polyY, c.boundary.length);
            }
        }

        // Draw Constellation Name Label (safely placed ABOVE the highest star)
        int fontSize = isSelected ? 15 : 13;
        g.setFont(Font.font("Verdana", FontWeight.BOLD, fontSize));
        if (isSelected) {
            g.setFill(Color.web("#fef08a"));
        } else if (isEnlightened) {
            g.setFill(Color.web("#fde047"));
        } else {
            g.setFill(Color.web("#93c5fd"));
        }

        // Measure text roughly to center it nicely
        double approxTextHalfW = c.name.length() * (fontSize * 0.32);
        double labelY = c.cy + c.minStarY - 18;
        g.fillText(c.name, c.cx - approxTextHalfW, labelY);

        // Subtitle status: show ✦ ENLIGHTENED only when enlightened, otherwise kept clean (no 'DORMANT')
        if (isEnlightened) {
            g.setFont(Font.font("Verdana", 10));
            g.setFill(Color.web("#fde047", 0.9));
            double subHalfW = 7 * 6.0; // "✦ ENLIGHTENED" half width
            g.fillText("✦ ENLIGHTENED", c.cx - subHalfW, labelY - 14);
        }

        // Draw Links (ONLY if constellation is enlightened!)
        if (isEnlightened) {
            for (int[] edge : c.links) {
                double x1 = c.cx + c.stars[edge[0]][0];
                double y1 = c.cy + c.stars[edge[0]][1];
                double x2 = c.cx + c.stars[edge[1]][0];
                double y2 = c.cy + c.stars[edge[1]][1];

                // Radiant golden starlight beam
                g.setStroke(Color.color(1.0, 0.82, 0.25, 0.35));
                g.setLineWidth(8.0);
                g.strokeLine(x1, y1, x2, y2);
                g.setStroke(Color.web("#fde047"));
                g.setLineWidth(2.5);
                g.strokeLine(x1, y1, x2, y2);
            }
        }

        // Draw Stars as 5-pointed celestial star shapes
        for (double[] s : c.stars) {
            double sx = c.cx + s[0];
            double sy = c.cy + s[1];

            if (isEnlightened) {
                // Radiant enlightened golden star
                drawStarShape(g, sx, sy, 14, 6.5,
                        Color.color(1.0, 0.9, 0.3, 0.4),
                        Color.web("#fef08a"),
                        Color.web("#fde047"), 1.8);
                // Center white hot sparkle
                g.setFill(Color.WHITE);
                g.fillOval(sx - 2.5, sy - 2.5, 5, 5);
            } else {
                // Dark / dormant star (dark slate core, subtle cyan border)
                drawStarShape(g, sx, sy, 10, 4.5,
                        null,
                        Color.web("#0f172a"),
                        Color.web("#38bdf8", 0.75), 1.2);
            }
        }
    }

    /**
     * Helper to draw a 5-pointed star polygon with optional outer aura glow and stroke.
     */
    private void drawStarShape(GraphicsContext g, double cx, double cy, double rOuter, double rInner,
                               Color glowColor, Color fillColor, Color strokeColor, double strokeWidth) {
        if (glowColor != null) {
            g.setFill(glowColor);
            fillStarPoints(g, cx, cy, rOuter * 1.6, rInner * 1.6);
        }
        if (fillColor != null) {
            g.setFill(fillColor);
            fillStarPoints(g, cx, cy, rOuter, rInner);
        }
        if (strokeColor != null) {
            g.setStroke(strokeColor);
            g.setLineWidth(strokeWidth);
            strokeStarPoints(g, cx, cy, rOuter, rInner);
        }
    }

    private void fillStarPoints(GraphicsContext g, double cx, double cy, double rOuter, double rInner) {
        double[] x = new double[10];
        double[] y = new double[10];
        calculateStarPoints(cx, cy, rOuter, rInner, x, y);
        g.fillPolygon(x, y, 10);
    }

    private void strokeStarPoints(GraphicsContext g, double cx, double cy, double rOuter, double rInner) {
        double[] x = new double[10];
        double[] y = new double[10];
        calculateStarPoints(cx, cy, rOuter, rInner, x, y);
        g.strokePolygon(x, y, 10);
    }

    private void calculateStarPoints(double cx, double cy, double rOuter, double rInner, double[] x, double[] y) {
        // 5-pointed star starts pointing upwards (-pi/2)
        double startAngle = -Math.PI / 2.0;
        double step = Math.PI / 5.0;
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? rOuter : rInner;
            double angle = startAngle + i * step;
            x[i] = cx + r * Math.cos(angle);
            y[i] = cy + r * Math.sin(angle);
        }
    }

    /**
     * Renders the floating 3D-styled yellow pointer arrow directly from the user's reference image!
     */
    private void drawPointerArrow(GraphicsContext g, double targetX, double targetY) {
        double bob = Math.sin(animTime * 3.8) * 7.0;
        double arrowTipY = targetY - 45 + bob;
        double headW = 38.0;
        double headH = 22.0;
        double stemW = 18.0;
        double stemH = 32.0;
        double topY = arrowTipY - headH - stemH;

        g.save();
        g.setEffect(new DropShadow(16, Color.web("#facc15")));

        // Arrow polygon points
        double[] ax = {
                targetX,
                targetX + headW / 2.0,
                targetX + stemW / 2.0,
                targetX + stemW / 2.0,
                targetX - stemW / 2.0,
                targetX - stemW / 2.0,
                targetX - headW / 2.0
        };
        double[] ay = {
                arrowTipY,
                arrowTipY - headH,
                arrowTipY - headH,
                topY,
                topY,
                arrowTipY - headH,
                arrowTipY - headH
        };

        // Vibrant 3D Yellow gradient
        LinearGradient arrowGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#fef08a")),
                new Stop(0.5, Color.web("#facc15")),
                new Stop(1.0, Color.web("#eab308"))
        );
        g.setFill(arrowGrad);
        g.fillPolygon(ax, ay, 7);

        g.setStroke(Color.web("#b45309"));
        g.setLineWidth(2.0);
        g.strokePolygon(ax, ay, 7);

        // Highlight stripe
        g.setFill(Color.color(1, 1, 1, 0.4));
        g.fillRect(targetX - 2.5, topY + 4, 5, stemH + headH - 10);

        g.restore();
    }

    // ─── Game Mode Selection & Launching ────────────────────────

    @FXML
    private void startAiMode() {
        if (selectedDef == null) return;
        isFriendMode = false;
        launchDuelGame(selectedDef);
    }

    @FXML
    private void startFriendsMode() {
        if (selectedDef == null) return;

        ButtonType hostBtn = new ButtonType("Host Game");
        ButtonType joinBtn = new ButtonType("Join Game");
        ButtonType localBtn = new ButtonType("Same Device (Pass & Play)");
        Alert modeChoice = new Alert(Alert.AlertType.CONFIRMATION);
        modeChoice.setTitle("Play with Friend");
        modeChoice.setHeaderText("How do you want to play together?");
        modeChoice.getButtonTypes().setAll(hostBtn, joinBtn, localBtn, ButtonType.CANCEL);

        modeChoice.showAndWait().ifPresent(choice -> {
            if (choice == hostBtn) startHostFlow();
            else if (choice == joinBtn) startJoinFlow();
            else if (choice == localBtn) startLocalFriendsMode();
        });
    }

    private void startLocalFriendsMode() {
        isNetworked = false;
        isFriendMode = true;
        isPlayer1Turn = true;
        launchDuelGame(selectedDef);
    }

    private void startHostFlow() {
        network = new NetworkManager();
        isHost = true;

        Alert waiting = new Alert(Alert.AlertType.INFORMATION);
        waiting.setTitle("Hosting");
        waiting.setHeaderText("Waiting for your friend to join...");
        waiting.setContentText("Share your IP address with your friend. Port: " + NetworkManager.DEFAULT_PORT);
        waiting.getButtonTypes().setAll(ButtonType.CANCEL);

        Task<Void> hostTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                network.hostAndWaitForClient(NetworkManager.DEFAULT_PORT);
                return null;
            }
        };
        hostTask.setOnSucceeded(e -> { waiting.close(); beginNetworkedMatch(); });
        hostTask.setOnFailed(e -> { waiting.close(); showNetworkError("Could not host game", hostTask.getException()); });

        Thread t = new Thread(hostTask);
        t.setDaemon(true);
        t.start();
        waiting.show();
    }

    private void startJoinFlow() {
        TextInputDialog ipDialog = new TextInputDialog("127.0.0.1");
        ipDialog.setTitle("Join Game");
        ipDialog.setHeaderText("Enter your friend's IP address");
        ipDialog.showAndWait().ifPresent(ip -> {
            network = new NetworkManager();
            isHost = false;

            Alert connecting = new Alert(Alert.AlertType.INFORMATION);
            connecting.setTitle("Connecting");
            connecting.setHeaderText("Connecting to " + ip + "...");
            connecting.getButtonTypes().setAll(ButtonType.CANCEL);

            Task<Void> joinTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    network.connectToHost(ip.trim(), NetworkManager.DEFAULT_PORT);
                    return null;
                }
            };
            joinTask.setOnSucceeded(e -> { connecting.close(); beginNetworkedMatch(); });
            joinTask.setOnFailed(e -> { connecting.close(); showNetworkError("Could not connect", joinTask.getException()); });

            Thread t = new Thread(joinTask);
            t.setDaemon(true);
            t.start();
            connecting.show();
        });
    }

    private void showNetworkError(String header, Throwable ex) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Network Error");
        error.setHeaderText(header);
        error.setContentText(ex != null ? ex.getMessage() : "Unknown error");
        error.showAndWait();
    }

    private void beginNetworkedMatch() {
        isNetworked = true;
        isFriendMode = true;
        isPlayer1Turn = true; // host always moves first

        network.startListening(this::handleNetworkMessage, this::handleDisconnect);

        if (isHost) {
            network.send("CONST:" + selectedDef.name);
            launchDuelGame(selectedDef);
        }
        // The joiner waits here — launchDuelGame() fires once "CONST:" arrives, below
    }

    private void handleNetworkMessage(String message) {
        if (message.startsWith("CONST:")) {
            ConstellationDef def = findConstellation(message.substring("CONST:".length()));
            if (def != null) {
                selectedDef = def;
                launchDuelGame(def);
            }
        } else if (message.startsWith("LINK:")) {
            String[] parts = message.substring("LINK:".length()).split(",");
            applyRemoteLink(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else if (message.equals("REMATCH")) {
            resultOverlay.setVisible(false);
            launchDuelGame(currentDuelDef);
        }
    }

    private void handleDisconnect() {
        if (turnIndicatorLabel != null) turnIndicatorLabel.setText("Your friend disconnected.");
        duelActive = false;
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();
    }

    private boolean isMyTurn() {
        return !isNetworked || (isPlayer1Turn == isHost);
    }

    private void applyRemoteLink(int a, int b) {
        if (!duelActive || currentDuelDef == null) return;
        int[] link = getCanonicalLink(a, b);
        if (containsLink(p1Links, link)) return; // already applied locally

        p1Links.add(link);
        player2OrAiScore += 10;
        aiScoreLabel.setText("Score: " + player2OrAiScore);

        Color remoteColor = isHost ? Color.web("#fbbf24") : Color.web("#38bdf8");
        friendLinkColors.put(link, remoteColor);

        isPlayer1Turn = !isPlayer1Turn;
        turnIndicatorLabel.setText(isMyTurn() ? "Your turn! Click 2 stars to connect." : "Waiting for your friend...");

        selectedStarIdx = -1;
        drawCurrentBoards();

        if (p1Links.size() == currentDuelDef.links.length) {
            finishDuel();
        }
    }

    private void launchDuelGame(ConstellationDef def) {
        currentDuelDef = def;
        duelActive = true;
        selectedStarIdx = -1;
        player1Score = 0;
        player2OrAiScore = 0;
        timeLeft = 30;

        p1Links.clear();
        p2OrAiLinks.clear();
        friendLinkColors.clear();

        // Switch layers
        mapOverlay.setVisible(false);
        gamePane.setVisible(true);
        resultOverlay.setVisible(false);

        duelConstellationNameLabel.setText("✦ " + def.name + " (" + (isFriendMode ? "FRIENDS DUEL" : "VS AI") + ")");
        playerScoreLabel.setText("Score: 0");
        aiScoreLabel.setText("Score: 0");
        timerLabel.setText("⏱ 0:30");

        if (isFriendMode) {
            player1TagLabel.setText("★ PLAYER 1 (BLUE)");
            opponentTagLabel.setText("★ PLAYER 2 (GOLD)");
            turnIndicatorLabel.setText("Player 1's turn! Click 2 stars to connect.");
            opponentHeader.setVisible(true);
            opponentHeader.setManaged(true);
            versusDivider.setVisible(false);
            versusDivider.setManaged(false);
            opponentBoard.setVisible(false);
            opponentBoard.setManaged(false);
        } else {
            player1TagLabel.setText("✦ YOU");
            opponentTagLabel.setText("✦ AI");
            turnIndicatorLabel.setText("Race against AI! Connect stars first.");
            opponentHeader.setVisible(true);
            opponentHeader.setManaged(true);
            versusDivider.setVisible(true);
            versusDivider.setManaged(true);
            opponentBoard.setVisible(true);
            opponentBoard.setManaged(true);
        }

        drawDuelBoard(playerCanvas, p1Links, selectedStarIdx, Color.web("#38bdf8"));
        if (!isFriendMode) {
            drawDuelBoard(aiCanvas, p2OrAiLinks, -1, Color.web("#c084fc"));
        }

        startTimers();
    }

    private void startTimers() {
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        duelTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText(String.format("⏱ 0:%02d", Math.max(0, timeLeft)));
            if (timeLeft <= 0) {
                finishDuel();
            }
        }));
        duelTimer.setCycleCount(Timeline.INDEFINITE);
        duelTimer.play();

        if (!isFriendMode) {
            // AI makes a link every 2 seconds
            aiTimer = new Timeline(new KeyFrame(Duration.seconds(2.2), e -> aiMakeMove()));
            aiTimer.setCycleCount(Timeline.INDEFINITE);
            aiTimer.play();
        }
    }

    // ─── Duel Gameplay Interactions ─────────────────────────────

    private void onPlayerCanvasClick(MouseEvent e) {
        if (!duelActive || currentDuelDef == null) return;

        double cw = playerCanvas.getWidth();
        double ch = playerCanvas.getHeight();
        double cx = cw / 2.0;
        double cy = ch / 2.0;

        for (int i = 0; i < currentDuelDef.stars.length; i++) {
            double sx = cx + currentDuelDef.stars[i][0] * 2.2;
            double sy = cy + currentDuelDef.stars[i][1] * 2.2;

            if (Math.hypot(e.getX() - sx, e.getY() - sy) < 22) {
                handleStarClicked(i);
                break;
            }
        }
    }

    private void handleStarClicked(int clickedIdx) {
        if (isNetworked && !isMyTurn()) return; // not your turn yet

        if (selectedStarIdx == -1) {
            selectedStarIdx = clickedIdx;
            drawCurrentBoards();
        } else if (selectedStarIdx == clickedIdx) {
            selectedStarIdx = -1;
            drawCurrentBoards();
        } else {
            int a = selectedStarIdx;
            int b = clickedIdx;
            selectedStarIdx = -1;

            if (isValidLink(a, b)) {
                int[] link = getCanonicalLink(a, b);
                if (containsLink(p1Links, link)) {
                    drawCurrentBoards();
                    return;
                }

                p1Links.add(link);
                player1Score += 10;
                playerScoreLabel.setText("Score: " + player1Score);

                if (isFriendMode) {
                    Color playerColor = isPlayer1Turn ? Color.web("#38bdf8") : Color.web("#fbbf24");
                    friendLinkColors.put(link, playerColor);
                    isPlayer1Turn = !isPlayer1Turn;
                    turnIndicatorLabel.setText(isNetworked
                            ? (isMyTurn() ? "Your turn! Click 2 stars to connect." : "Waiting for your friend...")
                            : ((isPlayer1Turn ? "Player 1's" : "Player 2's") + " turn! Click 2 stars to connect."));
                }

                if (isNetworked) {
                    network.send("LINK:" + a + "," + b);
                }

                drawCurrentBoards();

                if (p1Links.size() == currentDuelDef.links.length) {
                    finishDuel();
                }
            } else {
                drawCurrentBoards();
            }
        }
    }

    private void aiMakeMove() {
        if (!duelActive || currentDuelDef == null || isFriendMode) return;

        for (int[] l : currentDuelDef.links) {
            if (!containsLink(p2OrAiLinks, l)) {
                p2OrAiLinks.add(l);
                player2OrAiScore += 10;
                aiScoreLabel.setText("Score: " + player2OrAiScore);
                drawDuelBoard(aiCanvas, p2OrAiLinks, -1, Color.web("#c084fc"));

                if (p2OrAiLinks.size() == currentDuelDef.links.length) {
                    finishDuel();
                }
                return;
            }
        }
    }

    private boolean isValidLink(int a, int b) {
        if (currentDuelDef == null) return false;
        for (int[] edge : currentDuelDef.links) {
            if ((edge[0] == a && edge[1] == b) || (edge[0] == b && edge[1] == a)) {
                return true;
            }
        }
        return false;
    }

    private int[] getCanonicalLink(int a, int b) {
        return a < b ? new int[]{a, b} : new int[]{b, a};
    }

    private boolean containsLink(List<int[]> list, int[] link) {
        for (int[] item : list) {
            if ((item[0] == link[0] && item[1] == link[1]) || (item[0] == link[1] && item[1] == link[0])) {
                return true;
            }
        }
        return false;
    }

    private void drawCurrentBoards() {
        if (isFriendMode) {
            drawDuelBoard(playerCanvas, p1Links, selectedStarIdx, isPlayer1Turn ? Color.web("#38bdf8") : Color.web("#fbbf24"));
        } else {
            drawDuelBoard(playerCanvas, p1Links, selectedStarIdx, Color.web("#38bdf8"));
            drawDuelBoard(aiCanvas, p2OrAiLinks, -1, Color.web("#c084fc"));
        }
    }

    private void drawDuelBoard(Canvas canvas, List<int[]> litLinks, int highlightedStar, Color themeColor) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;

        g.setFill(Color.web("#020308"));
        g.fillRect(0, 0, w, h);

        // Soft celestial glow
        RadialGradient boardGlow = new RadialGradient(
                0, 0, cx, cy, 180, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.12)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        g.setFill(boardGlow);
        g.fillOval(cx - 180, cy - 180, 360, 360);

        // Ambient background stars
        g.setFill(Color.color(1, 1, 1, 0.25));
        for (int i = 0; i < 45; i++) {
            g.fillOval((i * 73) % w, (i * 127) % h, 1.5, 1.5);
        }

        if (currentDuelDef == null) return;

        // Draw lit links ONLY (do not show connecting lines until connected)
        for (int[] link : litLinks) {
            double x1 = cx + currentDuelDef.stars[link[0]][0] * 2.2;
            double y1 = cy + currentDuelDef.stars[link[0]][1] * 2.2;
            double x2 = cx + currentDuelDef.stars[link[1]][0] * 2.2;
            double y2 = cy + currentDuelDef.stars[link[1]][1] * 2.2;

            Color linkCol = (isFriendMode && friendLinkColors.containsKey(link)) ? friendLinkColors.get(link) : themeColor;

            g.setStroke(Color.color(linkCol.getRed(), linkCol.getGreen(), linkCol.getBlue(), 0.35));
            g.setLineWidth(10.0);
            g.strokeLine(x1, y1, x2, y2);

            g.setStroke(linkCol);
            g.setLineWidth(3.5);
            g.strokeLine(x1, y1, x2, y2);
        }

        // Draw stars as 5-pointed celestial star shapes
        for (int i = 0; i < currentDuelDef.stars.length; i++) {
            double sx = cx + currentDuelDef.stars[i][0] * 2.2;
            double sy = cy + currentDuelDef.stars[i][1] * 2.2;

            boolean isLit = isStarInLinks(i, litLinks);
            boolean isSelected = (i == highlightedStar);

            if (isSelected) {
                // Outer highlight aura for clicked star
                g.setFill(Color.web("#fde047", 0.45));
                fillStarPoints(g, sx, sy, 26, 12);
            }

            if (isLit) {
                // Enlightened connected star in duel match
                drawStarShape(g, sx, sy, 16, 7.5,
                        Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.45),
                        Color.WHITE,
                        themeColor, 2.0);
                // Center bright shine
                g.setFill(Color.WHITE);
                g.fillOval(sx - 3, sy - 3, 6, 6);
            } else {
                // Dark / unlit dormant star
                drawStarShape(g, sx, sy, 11, 5,
                        null,
                        Color.web("#0f172a"),
                        Color.web("#38bdf8", 0.75), 1.5);
            }
        }
    }

    private boolean isStarInLinks(int starIdx, List<int[]> links) {
        for (int[] edge : links) {
            if (edge[0] == starIdx || edge[1] == starIdx) return true;
        }
        return false;
    }

    // ─── End of Duel & Enlightenment Hook ───────────────────────

    private void finishDuel() {
        duelActive = false;
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> showResults());
        pause.play();
    }

    private void showResults() {
        resultOverlay.setVisible(true);

        boolean isVictory;
        if (isFriendMode) {
            isVictory = true; // Constellation completed by friends!
            if (player1Score > player2OrAiScore) {
                resultLabel.setText("⚔ PLAYER 1 WINS!");
            } else if (player2OrAiScore > player1Score) {
                resultLabel.setText("⚔ PLAYER 2 WINS!");
            } else {
                resultLabel.setText("✦ CONSTELLATION ENLIGHTENED! ✦");
            }
            resultScoreLabel.setText("Together you lit " + p1Links.size() + " / " + currentDuelDef.links.length + " links!");
        } else {
            boolean completedAll = (p1Links.size() == currentDuelDef.links.length);
            isVictory = completedAll || (player1Score > player2OrAiScore);

            if (isVictory) {
                resultLabel.setText("✦ CONSTELLATION ENLIGHTENED! ✦");
                resultScoreLabel.setText("You illuminated all stars of " + currentDuelDef.name + "! +50 Star Dust");
            } else {
                resultLabel.setText("✦ AI CLAIMED THE STARS ✦");
                resultScoreLabel.setText("You lit " + p1Links.size() + " links, AI lit " + p2OrAiLinks.size() + " links.");
            }
        }

        // Permanently enlighten constellation in Player state & database on victory!
        if (isVictory && currentDuelDef != null) {
            if (currentPlayer != null) {
                currentPlayer.enlightenConstellation(currentDuelDef.name);
                currentPlayer.setTotalStarDust(currentPlayer.getTotalStarDust() + 50);

                // Save to database permanently under player nickname
                playerDAO.saveEnlightenedConstellation(currentPlayer.getUsername(), currentDuelDef.name, currentPlayer.getTotalStarDust());
            }
        }

        updateHud();
    }

    @FXML
    private void rematch() {
        resultOverlay.setVisible(false);
        if (isNetworked) {
            if (isHost) {
                network.send("REMATCH");
                launchDuelGame(currentDuelDef);
            }
            // The joiner's rematch happens automatically via handleNetworkMessage above
        } else {
            launchDuelGame(currentDuelDef);
        }
    }

    @FXML
    private void returnToMap() {
        duelActive = false;
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        gamePane.setVisible(false);
        resultOverlay.setVisible(false);
        mapOverlay.setVisible(true);

        updateSelectionCard();
        updateHud();
        drawMap();
    }

    @FXML
    private void backToHub() {
        if (network != null) network.close();
        duelActive = false;
        if (mapAnimTimer != null) mapAnimTimer.stop();
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            if (currentPlayer != null) {
                controller.setPlayer(currentPlayer);
            }
            Stage stage = (Stage) rootStackPane.getScene().getWindow();
            SceneManager.switchScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
