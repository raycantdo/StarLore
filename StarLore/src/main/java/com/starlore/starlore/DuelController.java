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
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;

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
    @FXML private Button serverLobbyBtn;

    // ─── FXML Server Lobby Elements ─────────────────────────────
    @FXML private StackPane serverLobbyOverlay;
    @FXML private Label serverStatusLabel;
    @FXML private Label lobbyConstellationLabel;
    @FXML private ListView<String> serverUsersListView;
    @FXML private Button challengePlayerBtn;
    @FXML private Label myOnlineTagLabel;

    // ─── FXML In-Scene Celestial Challenge Overlays ─────────────
    @FXML private StackPane incomingChallengeOverlay;
    @FXML private Label incomingChallengerLabel;
    @FXML private Label incomingArenaLabel;
    @FXML private Button acceptChallengeBtn;
    @FXML private Button declineChallengeBtn;

    @FXML private StackPane waitingChallengeOverlay;
    @FXML private Label waitingTargetLabel;
    @FXML private Label waitingArenaLabel;

    @FXML private StackPane challengeNoticeOverlay;
    @FXML private Label noticeTitleLabel;
    @FXML private Label noticeMessageLabel;

    // ─── FXML Duel Game Elements ────────────────────────────────
    @FXML private BorderPane gamePane;
    @FXML private Label player1TagLabel;
    @FXML private Label playerScoreLabel;
    @FXML private Label playerProgressLabel;
    @FXML private Label duelConstellationNameLabel;
    @FXML private Label timerLabel;
    @FXML private Label turnIndicatorLabel;
    @FXML private VBox opponentHeader;
    @FXML private Label opponentTagLabel;
    @FXML private Label aiScoreLabel;
    @FXML private Label opponentProgressLabel;
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
    private int selectedStarIdxP2 = -1;
    private int player1Score = 0;
    private int player2OrAiScore = 0;
    private int timeLeft = 30;

    private final List<int[]> p1Links = new ArrayList<>();
    private final List<int[]> p2OrAiLinks = new ArrayList<>();
    private final Map<int[], Color> friendLinkColors = new HashMap<>();

    private Timeline duelTimer;
    private Timeline aiTimer;
    private final Random random = new Random();
    // ─── Networked Friends Mode (P2P) ───────────────────────────
    private NetworkManager network;
    private boolean isHost;
    private boolean isNetworked = false;

    // ─── Socket Server Online Duel Mode ──────────────────────────
    private DuelSocketClient duelSocketClient;
    private boolean isServerMode = false;
    private String remoteOpponentName;
    private final Set<String> serverOnlineUsers = new HashSet<>();
    private String pendingChallenger = "";
    private String pendingConstellation = "";
    private String outgoingTarget = "";
    private DuelSocketClient.DuelListener duelSocketListener;

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
        setupServerLobbyListCell();
        setupOnlineSession();
        startMapAnimation();
        updateSelectionCard();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
        if (player != null && player.getUsername() != null && !player.getUsername().trim().isEmpty()) {
            OnlineSessionManager.getInstance().login(player.getUsername());
        }
        updateHud();
        updateSelectionCard();
        drawMap();
        updateOnlineFriendsList();
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
        if (aiCanvas != null) {
            aiCanvas.setOnMouseClicked(this::onAiCanvasClick);
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

    // ─── Socket Server Online Lobby & Matchmaking ───────────────

    private void setupOnlineSession() {
        duelSocketListener = new DuelSocketClient.DuelListener() {
            @Override
            public void onOnlineUsers(List<String> onlineUsernames) {
                serverOnlineUsers.clear();
                serverOnlineUsers.addAll(onlineUsernames);
                updateOnlineFriendsList();
            }

            @Override
            public void onDuelInvite(String fromUser, String constellation) {
                handleIncomingChallenge(fromUser, constellation);
            }

            @Override
            public void onDuelAccepted(String fromUser, String constellation) {
                handleChallengeAccepted(fromUser, constellation);
            }

            @Override
            public void onDuelDeclined(String fromUser) {
                handleChallengeDeclined(fromUser);
            }

            @Override
            public void onRemoteLink(String fromUser, int starA, int starB) {
                applyRemoteLink(starA, starB);
            }

            @Override
            public void onRematch(String fromUser) {
                if (resultOverlay != null) resultOverlay.setVisible(false);
                launchDuelGame(currentDuelDef);
            }

            @Override
            public void onOpponentLeft(String fromUser) {
                if (turnIndicatorLabel != null) {
                    turnIndicatorLabel.setText(fromUser + " left the match.");
                }
                duelActive = false;
                if (duelTimer != null) duelTimer.stop();
                showChallengeNotice("✦ OPPONENT DEPARTED ✦", "Stargazer " + fromUser + " has left the match.");
            }

            @Override
            public void onDisconnected() {
                if (serverStatusLabel != null) {
                    serverStatusLabel.setText("⚪ Offline / Reconnecting...");
                    serverStatusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: 'Verdana'; -fx-font-size: 11px;");
                }
            }
        };

        OnlineSessionManager.getInstance().setActiveListener(duelSocketListener);
    }

    private void setupServerLobbyListCell() {
        if (serverUsersListView == null) return;

        // Ethereal Empty Placeholder
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        Label emptyIcon = new Label("🔭");
        emptyIcon.setStyle("-fx-font-size: 38px; -fx-effect: dropshadow(gaussian, rgba(56, 189, 248, 0.5), 14, 0, 0, 0);");
        Label emptyTitle = new Label("NO OTHER STARGAZERS ONLINE");
        emptyTitle.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-letter-spacing: 2;");
        Label emptySub = new Label("Invite a friend to log into StarLore, and they will appear here live!");
        emptySub.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 11px; -fx-text-fill: #64748b;");
        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
        serverUsersListView.setPlaceholder(emptyBox);

        serverUsersListView.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                    return;
                }

                // 1. Avatar Orb with User Initial and Glow
                StackPane avatarPane = new StackPane();
                avatarPane.setPrefSize(42, 42);
                avatarPane.setMaxSize(42, 42);

                Circle avatarCircle = new Circle(20);
                avatarCircle.setFill(new LinearGradient(
                        0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#0284c7")),
                        new Stop(0.5, Color.web("#6366f1")),
                        new Stop(1.0, Color.web("#8b5cf6"))
                ));
                avatarCircle.setStroke(Color.web("#38bdf8", 0.75));
                avatarCircle.setStrokeWidth(1.8);

                String initial = (username.length() > 0) ? username.substring(0, 1).toUpperCase() : "✦";
                Label initialLabel = new Label(initial);
                initialLabel.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0, 1, 1);");

                // Tiny bright green live indicator dot in bottom right
                Circle liveDot = new Circle(5);
                liveDot.setFill(Color.web("#10b981"));
                liveDot.setStroke(Color.web("#020617"));
                liveDot.setStrokeWidth(1.5);
                StackPane.setAlignment(liveDot, Pos.BOTTOM_RIGHT);

                avatarPane.getChildren().addAll(avatarCircle, initialLabel, liveDot);

                // 2. Info Column (Name & Status)
                VBox infoBox = new VBox(2);
                infoBox.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(username);
                nameLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

                Label statusLabel = new Label("✦ Celestial Challenger • Online");
                statusLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 10px; -fx-text-fill: #38bdf8;");
                infoBox.getChildren().addAll(nameLabel, statusLabel);

                // 3. Direct Clickable Challenge Button on Row
                Button challengeRowBtn = new Button("⚔️ CHALLENGE");
                challengeRowBtn.setStyle("-fx-background-color: linear-gradient(to right, #059669, #10b981); -fx-text-fill: white; -fx-font-family: 'Verdana'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 6 14; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.45), 8, 0, 0, 1);");
                challengeRowBtn.setOnAction(e -> {
                    e.consume();
                    serverUsersListView.getSelectionModel().select(username);
                    initiateChallenge(username);
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // 4. Outer Card Container
                HBox card = new HBox(14, avatarPane, infoBox, spacer, challengeRowBtn);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 14; -fx-border-color: rgba(56, 189, 248, 0.22); -fx-border-radius: 14; -fx-border-width: 1.2; -fx-padding: 8 16; -fx-cursor: hand;");

                if (isSelected()) {
                    card.setStyle("-fx-background-color: linear-gradient(to right, rgba(14, 165, 233, 0.35), rgba(99, 102, 241, 0.35)); -fx-background-radius: 14; -fx-border-color: #38bdf8; -fx-border-radius: 14; -fx-border-width: 1.8; -fx-padding: 8 16; -fx-effect: dropshadow(gaussian, rgba(56, 189, 248, 0.5), 14, 0.2, 0, 0);");
                }

                card.setOnMouseEntered(e -> {
                    if (!isSelected()) {
                        card.setStyle("-fx-background-color: rgba(28, 52, 98, 0.8); -fx-background-radius: 14; -fx-border-color: #38bdf8; -fx-border-radius: 14; -fx-border-width: 1.2; -fx-padding: 8 16; -fx-effect: dropshadow(gaussian, rgba(56, 189, 248, 0.35), 8, 0, 0, 0); -fx-cursor: hand;");
                    }
                });
                card.setOnMouseExited(e -> {
                    if (!isSelected()) {
                        card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 14; -fx-border-color: rgba(56, 189, 248, 0.22); -fx-border-radius: 14; -fx-border-width: 1.2; -fx-padding: 8 16; -fx-cursor: hand;");
                    }
                });

                card.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        initiateChallenge(username);
                    }
                });

                setGraphic(card);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 2;");
            }
        });

        serverUsersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (challengePlayerBtn != null) {
                if (newV != null && !newV.isEmpty()) {
                    challengePlayerBtn.setText("⚔️ CHALLENGE " + newV.toUpperCase());
                    challengePlayerBtn.setDisable(false);
                    challengePlayerBtn.setStyle("-fx-background-color: linear-gradient(to right, #059669, #10b981); -fx-text-fill: white; -fx-font-family: 'Verdana'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 26; -fx-background-radius: 20; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.6), 14, 0, 0, 2);");
                } else {
                    challengePlayerBtn.setText("👉 Click CHALLENGE on any Stargazer above");
                    challengePlayerBtn.setDisable(true);
                    challengePlayerBtn.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: #94a3b8; -fx-font-family: 'Verdana'; -fx-font-size: 12px; -fx-padding: 10 24; -fx-background-radius: 20;");
                }
            }
        });
    }

    private void updateOnlineFriendsList() {
        if (serverUsersListView == null) return;
        String myName = (currentPlayer != null && currentPlayer.getUsername() != null)
                ? currentPlayer.getUsername().trim() : "";

        List<String> friendsOnly = new ArrayList<>();
        for (String user : serverOnlineUsers) {
            if (!user.equalsIgnoreCase(myName)) {
                friendsOnly.add(user);
            }
        }

        serverUsersListView.getItems().setAll(friendsOnly);
        serverUsersListView.refresh();

        if (serverStatusLabel != null) {
            int friendCount = friendsOnly.size();
            serverStatusLabel.setText("🟢 " + (friendCount == 0 ? "Connected (You are online)" : friendCount + (friendCount == 1 ? " Stargazer Online" : " Stargazers Online")));
            serverStatusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 11px;");
        }

        if (myOnlineTagLabel != null) {
            myOnlineTagLabel.setText("✦ Logged in: " + (myName.isEmpty() ? "Guest" : myName.toUpperCase()));
        }

        if (challengePlayerBtn != null) {
            String sel = serverUsersListView.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isEmpty()) {
                challengePlayerBtn.setText("⚔️ CHALLENGE " + sel.toUpperCase());
                challengePlayerBtn.setDisable(false);
            } else {
                challengePlayerBtn.setText("👉 Click CHALLENGE on any Stargazer above");
                challengePlayerBtn.setDisable(true);
            }
        }
    }

    @FXML
    private void openServerLobby() {
        if (lobbyConstellationLabel != null) {
            String cName = (selectedDef != null) ? selectedDef.name : "ARIES";
            int starsCount = (selectedDef != null) ? selectedDef.stars.length : 4;
            lobbyConstellationLabel.setText(cName + " (" + starsCount + " Stars Arena)");
        }

        if (currentPlayer != null && currentPlayer.getUsername() != null && !OnlineSessionManager.getInstance().isConnected()) {
            OnlineSessionManager.getInstance().login(currentPlayer.getUsername());
        }

        updateOnlineFriendsList();
        if (serverLobbyOverlay != null) {
            serverLobbyOverlay.setVisible(true);
        }
    }

    @FXML
    private void closeServerLobby() {
        if (serverLobbyOverlay != null) {
            serverLobbyOverlay.setVisible(false);
        }
    }

    @FXML
    private void refreshServerUsers() {
        if (!OnlineSessionManager.getInstance().isConnected() && currentPlayer != null) {
            OnlineSessionManager.getInstance().login(currentPlayer.getUsername());
        }
        updateOnlineFriendsList();
    }

    public void initiateChallenge(String targetUser) {
        String myName = (currentPlayer != null && currentPlayer.getUsername() != null)
                ? currentPlayer.getUsername().trim() : "";
        if (targetUser == null || targetUser.equalsIgnoreCase(myName)) {
            showChallengeNotice("✦ INVALID TARGET ✦", "Please select another online stargazer to challenge.");
            return;
        }

        String constName = (selectedDef != null) ? selectedDef.name : "ARIES";
        int starCount = (selectedDef != null) ? selectedDef.stars.length : 4;
        outgoingTarget = targetUser;

        OnlineSessionManager.getInstance().sendInvite(targetUser, constName);
        showWaitingChallengeOverlay(targetUser, constName + " (" + starCount + " Stars)");
    }

    @FXML
    private void challengeSelectedPlayer() {
        if (serverUsersListView == null) return;
        String targetUser = serverUsersListView.getSelectionModel().getSelectedItem();
        if (targetUser == null) {
            showChallengeNotice("✦ NO TARGET SELECTED ✦", "Please select an online stargazer or click the green CHALLENGE button next to their name.");
            return;
        }
        initiateChallenge(targetUser);
    }

    // ─── In-Scene Celestial Challenge Dialogs ────────────────────

    private void handleIncomingChallenge(String fromUser, String constellation) {
        pendingChallenger = fromUser;
        pendingConstellation = constellation;
        if (incomingChallengerLabel != null) {
            incomingChallengerLabel.setText("Stargazer " + fromUser);
        }
        if (incomingArenaLabel != null) {
            ConstellationDef def = findConstellation(constellation);
            int count = (def != null) ? def.stars.length : 4;
            incomingArenaLabel.setText("✦ Arena: " + constellation + " (" + count + " Stars Arena) ✦");
        }
        if (waitingChallengeOverlay != null) waitingChallengeOverlay.setVisible(false);
        if (incomingChallengeOverlay != null) incomingChallengeOverlay.setVisible(true);
    }

    @FXML
    private void acceptIncomingChallenge() {
        if (incomingChallengeOverlay != null) incomingChallengeOverlay.setVisible(false);
        OnlineSessionManager.getInstance().acceptInvite(pendingChallenger, pendingConstellation);
        startServerDuelGame(pendingChallenger, pendingConstellation, false);
    }

    @FXML
    private void declineIncomingChallenge() {
        if (incomingChallengeOverlay != null) incomingChallengeOverlay.setVisible(false);
        OnlineSessionManager.getInstance().declineInvite(pendingChallenger);
    }

    private void showWaitingChallengeOverlay(String targetUser, String arenaInfo) {
        if (waitingTargetLabel != null) {
            waitingTargetLabel.setText("Waiting for " + targetUser + " to respond...");
        }
        if (waitingArenaLabel != null) {
            waitingArenaLabel.setText("Arena: " + arenaInfo);
        }
        if (waitingChallengeOverlay != null) waitingChallengeOverlay.setVisible(true);
    }

    @FXML
    private void cancelOutgoingChallenge() {
        if (waitingChallengeOverlay != null) waitingChallengeOverlay.setVisible(false);
        if (outgoingTarget != null && !outgoingTarget.isEmpty()) {
            OnlineSessionManager.getInstance().declineInvite(outgoingTarget);
        }
    }

    private void handleChallengeAccepted(String fromUser, String constellation) {
        if (waitingChallengeOverlay != null) waitingChallengeOverlay.setVisible(false);
        startServerDuelGame(fromUser, constellation, true);
    }

    private void handleChallengeDeclined(String fromUser) {
        if (waitingChallengeOverlay != null) waitingChallengeOverlay.setVisible(false);
        showChallengeNotice("✦ CHALLENGE DECLINED ✦", "Stargazer " + fromUser + " declined your duel invitation.");
    }

    private void showChallengeNotice(String title, String message) {
        if (noticeTitleLabel != null) noticeTitleLabel.setText(title);
        if (noticeMessageLabel != null) noticeMessageLabel.setText(message);
        if (challengeNoticeOverlay != null) challengeNoticeOverlay.setVisible(true);
    }

    @FXML
    private void dismissChallengeNotice() {
        if (challengeNoticeOverlay != null) challengeNoticeOverlay.setVisible(false);
    }

    private void startServerDuelGame(String opponent, String constellation, boolean iChallenged) {
        ConstellationDef def = findConstellation(constellation);
        if (def == null) def = selectedDef;

        isServerMode = true;
        isNetworked = true;
        isFriendMode = true;
        isHost = iChallenged;
        isPlayer1Turn = iChallenged; // Challenger moves first
        remoteOpponentName = opponent;

        if (serverLobbyOverlay != null) serverLobbyOverlay.setVisible(false);

        launchDuelGame(def);
    }

    // ─── Game Mode Selection & Launching ────────────────────────

    @FXML
    private void startAiMode() {
        if (selectedDef == null) {
            selectedDef = findConstellation("ARIES");
        }
        isServerMode = false;
        isFriendMode = false;
        isNetworked = false;
        remoteOpponentName = null;
        launchDuelGame(selectedDef);
    }

    @FXML
    private void startFriendsMode() {
        if (selectedDef == null) return;

        ButtonType serverBtn = new ButtonType("Online Server Lobby");
        ButtonType hostBtn = new ButtonType("Host Game (P2P)");
        ButtonType joinBtn = new ButtonType("Join Game (P2P)");
        ButtonType localBtn = new ButtonType("Same Device (Pass & Play)");
        Alert modeChoice = new Alert(Alert.AlertType.CONFIRMATION);
        modeChoice.setTitle("Play with Friend");
        modeChoice.setHeaderText("How do you want to play together?");
        modeChoice.getButtonTypes().setAll(serverBtn, hostBtn, joinBtn, localBtn, ButtonType.CANCEL);

        modeChoice.showAndWait().ifPresent(choice -> {
            if (choice == serverBtn) openServerLobby();
            else if (choice == hostBtn) startHostFlow();
            else if (choice == joinBtn) startJoinFlow();
            else if (choice == localBtn) startLocalFriendsMode();
        });
    }

    private void startLocalFriendsMode() {
        isServerMode = false;
        isNetworked = false;
        isFriendMode = true;
        isPlayer1Turn = true;
        launchDuelGame(selectedDef);
    }

    private void startHostFlow() {
        isServerMode = false;
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

    private void applyRemoteLink(int a, int b) {
        if (!duelActive || currentDuelDef == null) return;
        int[] link = getCanonicalLink(a, b);
        if (containsLink(p2OrAiLinks, link)) return; // already registered

        // Add to OPPONENT'S links on their board (never affects player's own board)
        p2OrAiLinks.add(link);
        player2OrAiScore += 10;
        aiScoreLabel.setText("Score: " + player2OrAiScore);

        String oppName = (isServerMode && remoteOpponentName != null) ? remoteOpponentName : (isNetworked ? "Friend" : "AI");
        int oppStars = countUniqueStars(p2OrAiLinks);
        if (turnIndicatorLabel != null) {
            turnIndicatorLabel.setText("★ " + oppName + " has connected " + oppStars + (oppStars == 1 ? " star!" : " stars!"));
        }

        drawCurrentBoards();

        // Opponent finished all links first
        if (p2OrAiLinks.size() == currentDuelDef.links.length) {
            finishDuel();
        }
    }

    private void launchDuelGame(ConstellationDef def) {
        currentDuelDef = def;
        duelActive = true;
        selectedStarIdx = -1;
        selectedStarIdxP2 = -1;
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

        String modeTag = isServerMode ? "ONLINE SERVER DUEL" : (isFriendMode && isNetworked ? "P2P DUEL" : (isFriendMode ? "LOCAL DUEL" : "VS AI"));
        duelConstellationNameLabel.setText("✦ " + def.name + " (" + modeTag + ")");
        playerScoreLabel.setText("Score: 0");
        aiScoreLabel.setText("Score: 0");
        timerLabel.setText("⏱ 0:30");

        // Dual boards are ALWAYS visible and active for competitive duels
        versusDivider.setVisible(true);
        versusDivider.setManaged(true);
        opponentBoard.setVisible(true);
        opponentBoard.setManaged(true);
        opponentHeader.setVisible(true);
        opponentHeader.setManaged(true);

        String myName = (currentPlayer != null && currentPlayer.getUsername() != null && !currentPlayer.getUsername().trim().isEmpty())
                ? currentPlayer.getUsername() : "YOU";

        if (isServerMode && remoteOpponentName != null) {
            player1TagLabel.setText("★ " + myName.toUpperCase() + " (YOU)");
            player1TagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #38bdf8;");
            opponentTagLabel.setText("★ " + remoteOpponentName.toUpperCase());
            opponentTagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #fbbf24;");
            turnIndicatorLabel.setText("Race against " + remoteOpponentName + "! Connect all stars first!");
        } else if (isNetworked) {
            player1TagLabel.setText("★ " + myName.toUpperCase() + " (YOU)");
            player1TagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #38bdf8;");
            opponentTagLabel.setText("★ OPPONENT");
            opponentTagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #fbbf24;");
            turnIndicatorLabel.setText("Race against your friend! Connect all stars first!");
        } else if (isFriendMode) {
            player1TagLabel.setText("★ PLAYER 1");
            player1TagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #38bdf8;");
            opponentTagLabel.setText("★ PLAYER 2");
            opponentTagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #fbbf24;");
            turnIndicatorLabel.setText("Player 1 plays on Left, Player 2 on Right! Race to connect stars!");
        } else {
            player1TagLabel.setText("✦ " + myName.toUpperCase() + " (YOU)");
            player1TagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #38bdf8;");
            opponentTagLabel.setText("✦ AI");
            opponentTagLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #c084fc;");
            turnIndicatorLabel.setText("Race against AI! Connect stars first.");
        }

        drawCurrentBoards();
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

        boolean isAi = !isFriendMode && !isServerMode && !isNetworked;
        if (isAi) {
            // AI makes a link every 2.0 seconds with an initial reaction delay of 1.2s
            aiTimer = new Timeline(new KeyFrame(Duration.seconds(2.0), e -> aiMakeMove()));
            aiTimer.setDelay(Duration.seconds(1.2));
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
        if (!duelActive || currentDuelDef == null) return;

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

                int myStars = countUniqueStars(p1Links);
                if (turnIndicatorLabel != null) {
                    turnIndicatorLabel.setText("✦ You connected " + myStars + " stars! Find the next link.");
                }

                if (isServerMode && remoteOpponentName != null) {
                    OnlineSessionManager.getInstance().sendLink(remoteOpponentName, a, b);
                } else if (isNetworked && network != null) {
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

    private void onAiCanvasClick(MouseEvent e) {
        if (!duelActive || currentDuelDef == null) return;
        // Only allow clicking aiCanvas in local friend mode (pass & play on same screen)
        if (!isFriendMode || isNetworked || isServerMode) return;

        double cw = aiCanvas.getWidth();
        double ch = aiCanvas.getHeight();
        double cx = cw / 2.0;
        double cy = ch / 2.0;

        for (int i = 0; i < currentDuelDef.stars.length; i++) {
            double sx = cx + currentDuelDef.stars[i][0] * 2.2;
            double sy = cy + currentDuelDef.stars[i][1] * 2.2;

            if (Math.hypot(e.getX() - sx, e.getY() - sy) < 22) {
                handleAiCanvasStarClicked(i);
                break;
            }
        }
    }

    private void handleAiCanvasStarClicked(int clickedIdx) {
        if (selectedStarIdxP2 == -1) {
            selectedStarIdxP2 = clickedIdx;
            drawCurrentBoards();
        } else if (selectedStarIdxP2 == clickedIdx) {
            selectedStarIdxP2 = -1;
            drawCurrentBoards();
        } else {
            int a = selectedStarIdxP2;
            int b = clickedIdx;
            selectedStarIdxP2 = -1;

            if (isValidLink(a, b)) {
                int[] link = getCanonicalLink(a, b);
                if (containsLink(p2OrAiLinks, link)) {
                    drawCurrentBoards();
                    return;
                }

                p2OrAiLinks.add(link);
                player2OrAiScore += 10;
                aiScoreLabel.setText("Score: " + player2OrAiScore);
                drawCurrentBoards();

                if (p2OrAiLinks.size() == currentDuelDef.links.length) {
                    finishDuel();
                }
            } else {
                drawCurrentBoards();
            }
        }
    }

    private void aiMakeMove() {
        if (!duelActive || currentDuelDef == null || isFriendMode || isServerMode || isNetworked) return;

        for (int[] l : currentDuelDef.links) {
            if (!containsLink(p2OrAiLinks, l)) {
                p2OrAiLinks.add(l);
                player2OrAiScore += 10;
                aiScoreLabel.setText("Score: " + player2OrAiScore);

                int oppStars = countUniqueStars(p2OrAiLinks);
                int totalStars = currentDuelDef.stars.length;
                if (turnIndicatorLabel != null) {
                    turnIndicatorLabel.setText("★ AI connected " + oppStars + "/" + totalStars + " stars! (" + p2OrAiLinks.size() + "/" + currentDuelDef.links.length + " links)");
                }

                drawCurrentBoards();

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

    private int countUniqueStars(List<int[]> links) {
        Set<Integer> unique = new HashSet<>();
        for (int[] edge : links) {
            unique.add(edge[0]);
            unique.add(edge[1]);
        }
        return unique.size();
    }

    private void updateProgressLabels() {
        if (currentDuelDef == null) return;
        int totalStars = currentDuelDef.stars.length;
        int totalLinks = currentDuelDef.links.length;

        int p1Stars = countUniqueStars(p1Links);
        if (playerProgressLabel != null) {
            playerProgressLabel.setText("★ " + p1Stars + " / " + totalStars + " Stars (" + p1Links.size() + "/" + totalLinks + " links)");
        }

        int p2Stars = countUniqueStars(p2OrAiLinks);
        if (opponentProgressLabel != null) {
            opponentProgressLabel.setText("★ " + p2Stars + " / " + totalStars + " Stars (" + p2OrAiLinks.size() + "/" + totalLinks + " links)");
        }
    }

    private void drawCurrentBoards() {
        drawDuelBoard(playerCanvas, p1Links, selectedStarIdx, Color.web("#38bdf8"));

        boolean isAi = (!isFriendMode && !isNetworked && !isServerMode);
        Color oppThemeColor = isAi ? Color.web("#c084fc") : Color.web("#fbbf24");
        String oppDisplayName = (isServerMode && remoteOpponentName != null) ? remoteOpponentName : (isNetworked ? "Opponent" : (isFriendMode ? "Player 2" : "AI"));

        if (isFriendMode && !isNetworked && !isServerMode) {
            // Local same-device pass & play
            drawDuelBoard(aiCanvas, p2OrAiLinks, selectedStarIdxP2, oppThemeColor);
        } else {
            // Competitor's board (Online Friends, P2P Friends, or VS AI):
            // Shroud secret connections and display live orbital progress tracker (how much they are connecting)!
            drawOpponentProgressBoard(aiCanvas, p2OrAiLinks, oppDisplayName, oppThemeColor);
        }

        updateProgressLabels();
    }

    private void drawOpponentProgressBoard(Canvas canvas, List<int[]> links, String oppName, Color themeColor) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;

        g.setFill(Color.web("#020308"));
        g.fillRect(0, 0, w, h);

        // 1. Soft celestial nebula glow
        RadialGradient boardGlow = new RadialGradient(
                0, 0, cx, cy - 20, 210, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.16)),
                new Stop(0.65, Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.04)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        g.setFill(boardGlow);
        g.fillOval(cx - 210, (cy - 20) - 210, 420, 420);

        // 2. Ambient background stars
        g.setFill(Color.color(1, 1, 1, 0.28));
        for (int i = 0; i < 48; i++) {
            g.fillOval((i * 67 + 13) % w, (i * 131 + 29) % h, 1.6, 1.6);
        }

        if (currentDuelDef == null) return;

        int totalStars = currentDuelDef.stars.length;
        int connectedStars = countUniqueStars(links);
        int totalLinks = currentDuelDef.links.length;
        int connectedLinks = links.size();

        double wheelCenterY = cy - 40;
        double wheelR = 120.0;

        // 3. Shroud Rings (Astrolabe Orbit)
        g.setStroke(Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.22));
        g.setLineWidth(1.5);
        g.strokeOval(cx - wheelR - 18, wheelCenterY - wheelR - 18, (wheelR + 18) * 2, (wheelR + 18) * 2);

        g.setStroke(Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.4));
        g.setLineWidth(2.5);
        g.strokeOval(cx - wheelR, wheelCenterY - wheelR, wheelR * 2, wheelR * 2);

        g.setStroke(Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.18));
        g.setLineWidth(1.0);
        g.strokeOval(cx - wheelR + 25, wheelCenterY - wheelR + 25, (wheelR - 25) * 2, (wheelR - 25) * 2);

        // Connecting progress arc / ring fill
        if (connectedStars > 0) {
            double arcExtent = ((double) connectedStars / totalStars) * 360.0;
            g.setStroke(Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.7));
            g.setLineWidth(6.0);
            g.strokeArc(cx - wheelR, wheelCenterY - wheelR, wheelR * 2, wheelR * 2, 90, -arcExtent, javafx.scene.shape.ArcType.OPEN);
        }

        // 4. Circular Celestial Star Sockets
        for (int i = 0; i < totalStars; i++) {
            double angle = -Math.PI / 2.0 + i * (2.0 * Math.PI / totalStars);
            double sx = cx + Math.cos(angle) * wheelR;
            double sy = wheelCenterY + Math.sin(angle) * wheelR;

            boolean isLit = (i < connectedStars);
            if (isLit) {
                // Outer radiant glow aura
                RadialGradient starAura = new RadialGradient(
                        0, 0, sx, sy, 22, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.6)),
                        new Stop(1.0, Color.TRANSPARENT)
                );
                g.setFill(starAura);
                g.fillOval(sx - 22, sy - 22, 44, 44);

                // Illuminated star
                drawStarShape(g, sx, sy, 16, 7.5,
                        Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.5),
                        Color.WHITE,
                        themeColor, 2.0);
                g.setFill(Color.WHITE);
                g.fillOval(sx - 3.5, sy - 3.5, 7, 7);
            } else {
                // Shrouded dormant socket
                drawStarShape(g, sx, sy, 10, 5,
                        null,
                        Color.web("#0f172a"),
                        Color.web("#475569", 0.7), 1.2);
            }
        }

        // 5. Center Dial Readout
        // Frosted central backing
        g.setFill(new RadialGradient(
                0, 0, cx, wheelCenterY, wheelR - 30, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(10, 20, 48, 0.88)),
                new Stop(0.8, Color.rgb(6, 12, 32, 0.95)),
                new Stop(1.0, Color.TRANSPARENT)
        ));
        g.fillOval(cx - (wheelR - 30), wheelCenterY - (wheelR - 30), (wheelR - 30) * 2, (wheelR - 30) * 2);

        // Number of stars connected
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        g.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        g.setFill(themeColor);
        g.fillText(connectedStars + " / " + totalStars, cx, wheelCenterY - 2);

        // Subtitle inside wheel
        g.setFont(Font.font("Verdana", FontWeight.BOLD, 10));
        g.setFill(Color.web("#cbd5e1"));
        g.fillText("STARS CONNECTED", cx, wheelCenterY + 18);

        g.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
        g.setFill(Color.web("#38bdf8"));
        g.fillText(connectedLinks + " of " + totalLinks + " links", cx, wheelCenterY + 34);

        // 6. Opponent Status Banner Card Below the Wheel
        double bannerY = cy + 120;
        double bannerW = 380;
        double bannerH = 46;
        g.setFill(Color.web("#09122c", 0.9));
        g.fillRoundRect(cx - bannerW / 2.0, bannerY, bannerW, bannerH, 16, 16);
        g.setStroke(Color.color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0.35));
        g.setLineWidth(1.4);
        g.strokeRoundRect(cx - bannerW / 2.0, bannerY, bannerW, bannerH, 16, 16);

        g.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        if (connectedStars == 0) {
            g.setFill(Color.web("#94a3b8"));
            g.fillText("✦ " + oppName + " is searching the sky... ✦", cx, bannerY + 28);
        } else if (connectedLinks == totalLinks) {
            g.setFill(Color.web("#34d399"));
            g.fillText("✦ " + oppName + " illuminated all " + totalStars + " stars! ✦", cx, bannerY + 28);
        } else {
            g.setFill(themeColor);
            String starWord = (connectedStars == 1) ? " star" : " stars";
            g.fillText("★ " + oppName + " has connected " + connectedStars + starWord + "! (" + connectedLinks + "/" + totalLinks + " links)", cx, bannerY + 28);
        }

        // 7. Horizontal Progress Bar
        double barY = cy + 184;
        double barW = 340;
        double barH = 10;
        g.setFill(Color.web("#1e293b", 0.8));
        g.fillRoundRect(cx - barW / 2.0, barY, barW, barH, 10, 10);

        if (totalLinks > 0 && connectedLinks > 0) {
            double fillW = Math.min(barW, barW * (connectedLinks / (double) totalLinks));
            g.setFill(new LinearGradient(
                    0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#f59e0b")),
                    new Stop(1.0, themeColor)
            ));
            g.fillRoundRect(cx - barW / 2.0, barY, fillW, barH, 10, 10);
        }

        int percent = (totalLinks > 0) ? (int)((connectedLinks * 100.0) / totalLinks) : 0;
        g.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
        g.setFill(Color.web("#cbd5e1"));
        g.fillText("RIVAL PROGRESS: " + percent + "%", cx, barY + 26);

        // Shroud notice at the very bottom
        g.setFont(Font.font("Verdana", FontWeight.NORMAL, 9.5));
        g.setFill(Color.web("#64748b"));
        g.fillText("✦ Shrouded Realm: Competitor's constellation lines remain secret ✦", cx, h - 22);
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

            Color linkCol = themeColor;

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

        boolean localCompleted = (currentDuelDef != null && p1Links.size() == currentDuelDef.links.length);
        boolean opponentCompleted = (currentDuelDef != null && p2OrAiLinks.size() == currentDuelDef.links.length);

        boolean isVictory = false;
        boolean isTie = false;

        if (localCompleted && !opponentCompleted) {
            isVictory = true;
        } else if (opponentCompleted && !localCompleted) {
            isVictory = false;
        } else {
            // Evaluated by scores
            if (player1Score > player2OrAiScore) {
                isVictory = true;
            } else if (player2OrAiScore > player1Score) {
                isVictory = false;
            } else {
                isTie = true;
            }
        }

        String oppDisplayName;
        if (isServerMode && remoteOpponentName != null) {
            oppDisplayName = remoteOpponentName;
        } else if (isNetworked) {
            oppDisplayName = "Your friend";
        } else if (isFriendMode) {
            oppDisplayName = "Player 2";
        } else {
            oppDisplayName = "AI";
        }

        if (isTie) {
            resultLabel.setText("✦ CELESTIAL DRAW! ✦");
            resultLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #ffd65a; -fx-effect: dropshadow(gaussian, #f59e0b, 20, 0.5, 0, 0);");
            resultScoreLabel.setText("Both stargazers tied with " + player1Score + " points!");
        } else if (isVictory) {
            resultLabel.setText("✦ VICTORY! ✦");
            resultLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #34d399; -fx-effect: dropshadow(gaussian, #10b981, 20, 0.5, 0, 0);");
            if (localCompleted) {
                resultScoreLabel.setText("You illuminated " + currentDuelDef.name + " before " + oppDisplayName + "! +50 Star Dust");
            } else {
                resultScoreLabel.setText("You outscored " + oppDisplayName + " (" + player1Score + " vs " + player2OrAiScore + ")! +50 Star Dust");
            }
        } else {
            resultLabel.setText("⚔ DEFEAT ⚔");
            resultLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #f87171; -fx-effect: dropshadow(gaussian, #ef4444, 20, 0.5, 0, 0);");
            if (opponentCompleted) {
                resultScoreLabel.setText(oppDisplayName + " illuminated " + currentDuelDef.name + " first! (" + player2OrAiScore + " pts)");
            } else {
                resultScoreLabel.setText(oppDisplayName + " won with higher score (" + player2OrAiScore + " vs " + player1Score + ").");
            }
        }

        // Permanently enlighten constellation in Player state & database on victory!
        if (isVictory && currentDuelDef != null) {
            if (currentPlayer != null) {
                currentPlayer.enlightenConstellation(currentDuelDef.name);

                // Add +50 StarDust and permanently save the new total
                ScoreService.addStarDust(currentPlayer, 50);

                // Save the enlightened constellation
                playerDAO.saveEnlightenedConstellation(
                        currentPlayer.getUsername(),
                        currentDuelDef.name,
                        currentPlayer.getTotalStarDust()
                );
            }
        }

        updateHud();
    }

    @FXML
    private void rematch() {
        resultOverlay.setVisible(false);
        if (isServerMode && remoteOpponentName != null) {
            if (isHost) {
                OnlineSessionManager.getInstance().sendRematch(remoteOpponentName);
                launchDuelGame(currentDuelDef);
            }
        } else if (isNetworked && network != null) {
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
        if (isServerMode && remoteOpponentName != null && duelActive) {
            OnlineSessionManager.getInstance().sendLeave(remoteOpponentName);
        }
        duelActive = false;
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        isServerMode = false;
        isNetworked = false;
        isFriendMode = false;
        remoteOpponentName = null;

        gamePane.setVisible(false);
        resultOverlay.setVisible(false);
        mapOverlay.setVisible(true);

        updateSelectionCard();
        updateHud();
        drawMap();
    }

    @FXML
    private void backToHub() {
        if (isServerMode && remoteOpponentName != null && duelActive) {
            OnlineSessionManager.getInstance().sendLeave(remoteOpponentName);
        }
        OnlineSessionManager.getInstance().removeActiveListener(duelSocketListener);
        if (network != null) network.close();
        duelActive = false;
        if (mapAnimTimer != null) mapAnimTimer.stop();
        if (duelTimer != null) duelTimer.stop();
        if (aiTimer != null) aiTimer.stop();

        isServerMode = false;
        isNetworked = false;
        isFriendMode = false;
        remoteOpponentName = null;

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
