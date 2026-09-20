package com.starlore.starlore;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleController {

    @FXML private AnchorPane rootPane;
    @FXML private AnchorPane battleFieldPane;
    @FXML private StackPane contentWrapper;
    @FXML private Canvas galaxyCanvas;
    @FXML private ImageView perseusSprite;
    @FXML private ImageView medusaSprite;
    @FXML private ProgressBar playerHealthBar;
    @FXML private ProgressBar enemyHealthBar;
    @FXML private ProgressBar ultimateBar;
    @FXML private Label battleTextLabel;
    @FXML private Label weakPointLabel;
    @FXML private Label potionsLabel;
    @FXML private HBox actionMenu;
    @FXML private Button attackButton;
    @FXML private Button defendButton;
    @FXML private Button focusButton;
    @FXML private Button specialButton;
    @FXML private Button potionButton;
    @FXML private Button fleeButton;
    @FXML private Button backButton;
    @FXML private Rectangle flashOverlay;

    @FXML private StackPane storyOverlay;
    @FXML private VBox storyCard;
    @FXML private Label storySpeakerLabel;
    @FXML private Label storyTextLabel;
    @FXML private Button storyContinueButton;
    @FXML private Button storySkipButton;

    private AnimationTimer galaxyTimer;
    private double galaxyTime = 0;
    private final Color nebulaColor = Color.web("#38bdf8");

    private static final int STAR_COUNT = 160;
    private final double[] starX = new double[STAR_COUNT];
    private final double[] starY = new double[STAR_COUNT];
    private final double[] starR = new double[STAR_COUNT];
    private final double[] starAlpha = new double[STAR_COUNT];

    private final String[][] storyLines = {
            {"PERSEUS", "I was born to Princess Danaë, but my grandfather King Acrisius feared a prophecy that I would one day be his end."},
            {"PERSEUS", "He sealed my mother and me in a wooden chest and cast us into the sea, hoping the waves would finish what he could not."},
            {"MEDUSA", "I was not always this. Once, I served as a priestess in Athena's temple, admired for my beauty above all others."},
            {"MEDUSA", "Poseidon defiled me within those sacred walls — and Athena's fury fell not on him, but on me."},
            {"MEDUSA", "She cursed my hair into serpents and my gaze into stone, and cast me into exile among the rocks."},
            {"PERSEUS", "Years later, a king who desired my mother sent me to bring him Medusa's head, certain the task would be my death."},
            {"PERSEUS", "But the gods armed me: a mirrored shield from Athena, winged sandals from Hermes, a blade sharper than fate itself."},
            {"MEDUSA", "Now a hero comes to end my curse... and my life. Let the stars remember what was truly done to me."}
    };
    private int storyIndex = 0;
    private String currentStoryLine = "";
    private Timeline typewriterTimeline;

    private double playerHealth = 1.0;
    private double enemyHealth = 1.0;
    private double ultimateCharge = 0.0;
    private boolean weakPointAvailable = false;
    private Player currentPlayer;
    private boolean isPlayerDefending = false;
    private final Random random = new Random();

    private static final double CRIT_CHANCE_ATTACK = 0.22;
    private static final double CRIT_CHANCE_SPECIAL = 0.30;
    private static final double FLEE_CHANCE = 0.20;
    private static final int MAX_POTIONS = 2;
    private int potionsLeft = MAX_POTIONS;

    private double PERSEUS_W, PERSEUS_H;
    private double MEDUSA_W, MEDUSA_H;

    private Timeline perseusAnim, medusaAnim;
    private ScaleTransition weakPointPulse;

    // ─── Combat particles (drawn on galaxyCanvas alongside the starfield) ──
    private static class BattleParticle {
        double x, y, vx, vy, life, maxLife;
        Color color;
    }
    private final List<BattleParticle> battleParticles = new ArrayList<>();

    @FXML
    public void initialize() {
        if (rootPane != null) {
            galaxyCanvas.widthProperty().bind(rootPane.widthProperty());
            galaxyCanvas.heightProperty().bind(rootPane.heightProperty());
        }
        seedStars();
        startGalaxyAnimation();

        Image pSheet = new Image(getClass().getResource("images/perseus.png").toExternalForm());
        perseusSprite.setImage(pSheet);
        PERSEUS_W = (int) (pSheet.getWidth() / 4);
        PERSEUS_H = (int) (pSheet.getHeight() / 4);

        Image mSheet = new Image(getClass().getResource("images/medusa1.png").toExternalForm());
        medusaSprite.setImage(mSheet);
        MEDUSA_W = (int) (mSheet.getWidth() / 4);
        MEDUSA_H = (int) (mSheet.getHeight() / 6);
        medusaSprite.setLayoutY(360.0);

        startIdles();

        actionMenu.setVisible(false);
        actionMenu.setDisable(true);
        storyOverlay.setVisible(true);
        storyIndex = 0;
        playCardEntrance();

        potionsLabel.setText("🧪 x" + potionsLeft);
        weakPointLabel.setVisible(false);
        weakPointLabel.setOpacity(0);
        flashOverlay.setOpacity(0);
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
    }

    // ─── PRE-FIGHT STORY ─────────────────────────────────────────

    private void showStoryLine() {
        if (storyIndex >= storyLines.length) {
            beginFight();
            return;
        }

        String speaker = storyLines[storyIndex][0];
        String line = storyLines[storyIndex][1];
        currentStoryLine = line;
        boolean perseusSpeaking = speaker.equals("PERSEUS");

        storySpeakerLabel.setText(spaced(speaker));
        storySpeakerLabel.setStyle(perseusSpeaking
                ? "-fx-font-family: 'Verdana'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-letter-spacing: 4; -fx-effect: dropshadow(gaussian, #38bdf8, 8, 0.5, 0, 0);"
                : "-fx-font-family: 'Verdana'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-letter-spacing: 4; -fx-effect: dropshadow(gaussian, #ef4444, 8, 0.5, 0, 0);");

        typewriterReveal(line);
        storyContinueButton.setText(storyIndex == storyLines.length - 1 ? "BEGIN BATTLE ⚔" : "CONTINUE");

        spotlightSpeaker(perseusSpeaking);
        if (storyIndex > 0) playCardPulse();
    }

    /** Spreads a word out into letter-spaced plaque text: "PERSEUS" -> "P E R S E U S". */
    private String spaced(String word) {
        return String.join(" ", word.split(""));
    }

    /** Reveals a line of dialogue, wrapped in quotation marks, one character at a time. */
    private void typewriterReveal(String rawText) {
        String fullText = "\"" + rawText + "\"";
        if (typewriterTimeline != null) typewriterTimeline.stop();
        storyTextLabel.setText("");
        typewriterTimeline = new Timeline();
        double msPerChar = 18;
        for (int i = 1; i <= fullText.length(); i++) {
            final String sub = fullText.substring(0, i);
            KeyFrame kf = new KeyFrame(Duration.millis(msPerChar * i), e -> storyTextLabel.setText(sub));
            typewriterTimeline.getKeyFrames().add(kf);
        }
        typewriterTimeline.play();
    }

    /** First-time reveal of the story tablet: fades and scales in from the center. */
    private void playCardEntrance() {
        storyCard.setOpacity(0);
        storyCard.setScaleX(0.9);
        storyCard.setScaleY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(450), storyCard);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(450), storyCard);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition entrance = new ParallelTransition(fade, scale);
        entrance.setOnFinished(e -> showStoryLine());
        entrance.play();
    }

    /** A brief settle/flicker on the tablet whenever a new line of dialogue appears. */
    private void playCardPulse() {
        FadeTransition flicker = new FadeTransition(Duration.millis(140), storyCard);
        flicker.setFromValue(0.55);
        flicker.setToValue(1.0);
        flicker.play();

        ScaleTransition pop = new ScaleTransition(Duration.millis(160), storyCard);
        pop.setFromX(0.97);
        pop.setFromY(0.97);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.play();
    }

    private void spotlightSpeaker(boolean perseusSpeaking) {
        ImageView speaking = perseusSpeaking ? perseusSprite : medusaSprite;
        ImageView quiet = perseusSpeaking ? medusaSprite : perseusSprite;

        FadeTransition dim = new FadeTransition(Duration.millis(300), quiet);
        dim.setToValue(0.35);
        dim.play();

        FadeTransition lightUp = new FadeTransition(Duration.millis(300), speaking);
        lightUp.setToValue(1.0);
        lightUp.play();

        speaking.setEffect(new DropShadow(30, perseusSpeaking ? Color.web("#38bdf8") : Color.web("#ef4444")));
        quiet.setEffect(null);

        TranslateTransition bob = new TranslateTransition(Duration.millis(180), speaking);
        bob.setByY(-10);
        bob.setCycleCount(2);
        bob.setAutoReverse(true);
        bob.play();
    }
    @FXML
    void onStorySkip(ActionEvent event) {
        if (typewriterTimeline != null) typewriterTimeline.stop();
        beginFight();
    }

    @FXML
    void onStoryContinue(ActionEvent event) {
        storyIndex++;
        showStoryLine();
    }

    private void beginFight() {
        storyOverlay.setVisible(false);
        perseusSprite.setOpacity(1.0);
        medusaSprite.setOpacity(1.0);
        perseusSprite.setEffect(null);
        medusaSprite.setEffect(null);
        actionMenu.setVisible(true);
        actionMenu.setDisable(false);
        battleTextLabel.setText("What will Perseus do?");
    }

    // ─── GALAXY BACKGROUND + COMBAT PARTICLES ────────────────────

    private void seedStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble();
            starR[i] = random.nextDouble() * 1.8 + 0.6;
            starAlpha[i] = random.nextDouble() * 0.5 + 0.3;
        }
    }

    private void startGalaxyAnimation() {
        galaxyTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                galaxyTime += 0.012;
                renderGalaxy();
            }
        };
        galaxyTimer.start();
    }

    private void renderGalaxy() {
        double w = galaxyCanvas.getWidth();
        double h = galaxyCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext g = galaxyCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        g.setFill(Color.web("#020308"));
        g.fillRect(0, 0, w, h);

        RadialGradient nebula = new RadialGradient(0, 0, w * 0.5, h * 0.35, w * 0.6, false, CycleMethod.NO_CYCLE,
                new Stop(0, nebulaColor.deriveColor(0, 1, 1, 0.18)),
                new Stop(0.6, nebulaColor.deriveColor(0, 1, 1, 0.06)),
                new Stop(1, Color.TRANSPARENT));
        g.setFill(nebula);
        g.fillRect(0, 0, w, h);

        for (int i = 0; i < STAR_COUNT; i++) {
            double alpha = Math.max(0.1, Math.min(0.9, starAlpha[i] + Math.sin(galaxyTime * 1.5 + i) * 0.2));
            g.setFill(Color.color(1, 1, 1, alpha));
            g.fillOval(starX[i] * w, starY[i] * h * 0.78, starR[i], starR[i]);
        }

        double offsetX = (w - 950) / 2.0;
        double offsetY = (h - 700) / 2.0;

        drawPlanetPlatform(g, offsetX + 210, offsetY + 585, 110, 28);
        drawPlanetPlatform(g, offsetX + 790, offsetY + 605, 130, 32);

        updateAndDrawParticles(g);
    }

    private void updateAndDrawParticles(GraphicsContext g) {
        for (int i = battleParticles.size() - 1; i >= 0; i--) {
            BattleParticle p = battleParticles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.05;
            p.life -= 1;
            if (p.life <= 0) {
                battleParticles.remove(i);
                continue;
            }
            double alpha = Math.max(0, p.life / p.maxLife);
            g.setGlobalAlpha(alpha);
            g.setFill(p.color);
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
        g.setGlobalAlpha(1.0);
    }

    /** Spawns a small burst of particles centered on the given sprite's on-screen position. */
    private void spawnHitParticles(ImageView target, Color color, int count) {
        Point2D scenePoint = target.localToScene(target.getFitWidth() / 2.0, target.getFitHeight() / 2.0);
        Point2D canvasPoint = galaxyCanvas.sceneToLocal(scenePoint);
        for (int i = 0; i < count; i++) {
            BattleParticle p = new BattleParticle();
            p.x = canvasPoint.getX();
            p.y = canvasPoint.getY();
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 3.2;
            p.vx = Math.cos(angle) * speed;
            p.vy = Math.sin(angle) * speed;
            p.maxLife = 26 + random.nextInt(18);
            p.life = p.maxLife;
            p.color = color;
            battleParticles.add(p);
        }
    }

    private void drawPlanetPlatform(GraphicsContext g, double cx, double cy, double rx, double ry) {
        RadialGradient platform = new RadialGradient(0, 0, cx, cy, rx, false, CycleMethod.NO_CYCLE,
                new Stop(0, nebulaColor.deriveColor(0, 1, 1.3, 0.55)),
                new Stop(0.7, nebulaColor.deriveColor(0, 1, 0.6, 0.25)),
                new Stop(1, Color.TRANSPARENT));
        g.setFill(platform);
        g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);

        g.setStroke(nebulaColor.deriveColor(0, 1, 1, 0.6));
        g.setLineWidth(1.2);
        g.strokeOval(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    private void startIdles() {
        playPerseusAnim(1, 0, 1, 800, true);
        playMedusaAnim(0, 0, 4, 800, true);
    }

    // ─── COMBAT JUICE (damage numbers, hit flash, screen shake/flash) ──

    private void showDamagePopup(ImageView target, String text, String colorHex) {
        Label popup = new Label(text);
        popup.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: "
                + colorHex + "; -fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 1);");
        popup.setLayoutX(target.getLayoutX() + target.getFitWidth() / 2 - 20);
        popup.setLayoutY(target.getLayoutY() - 10);
        battleFieldPane.getChildren().add(popup);

        TranslateTransition rise = new TranslateTransition(Duration.millis(900), popup);
        rise.setByY(-50);
        FadeTransition fade = new FadeTransition(Duration.millis(900), popup);
        fade.setFromValue(1);
        fade.setToValue(0);
        ParallelTransition combo = new ParallelTransition(rise, fade);
        combo.setOnFinished(e -> battleFieldPane.getChildren().remove(popup));
        combo.play();
    }

    private void hitFlash(ImageView sprite) {
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, e -> sprite.setOpacity(0.3)),
                new KeyFrame(Duration.millis(80), e -> sprite.setOpacity(1.0)),
                new KeyFrame(Duration.millis(160), e -> sprite.setOpacity(0.3)),
                new KeyFrame(Duration.millis(240), e -> sprite.setOpacity(1.0))
        );
        flash.play();
    }

    private void screenShake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), contentWrapper);
        shake.setFromX(-10);
        shake.setToX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> contentWrapper.setTranslateX(0));
        shake.play();
    }

    /** Quick full-battlefield white flash — used on Critical Hits and the Ultimate. */
    private void flashScreen() {
        flashOverlay.setOpacity(0.0);
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, e -> flashOverlay.setOpacity(0.55)),
                new KeyFrame(Duration.millis(160), e -> flashOverlay.setOpacity(0.0))
        );
        flash.play();
    }

    private void showWeakPointBanner() {
        weakPointLabel.setVisible(true);
        weakPointLabel.setOpacity(0);

        FadeTransition in = new FadeTransition(Duration.millis(300), weakPointLabel);
        in.setToValue(1);
        in.play();

        if (weakPointPulse != null) weakPointPulse.stop();
        weakPointPulse = new ScaleTransition(Duration.millis(500), weakPointLabel);
        weakPointPulse.setFromX(1.0);
        weakPointPulse.setFromY(1.0);
        weakPointPulse.setToX(1.15);
        weakPointPulse.setToY(1.15);
        weakPointPulse.setCycleCount(Timeline.INDEFINITE);
        weakPointPulse.setAutoReverse(true);
        weakPointPulse.play();
    }

    private void hideWeakPointBanner() {
        if (weakPointPulse != null) {
            weakPointPulse.stop();
            weakPointPulse = null;
        }
        FadeTransition out = new FadeTransition(Duration.millis(250), weakPointLabel);
        out.setToValue(0);
        out.setOnFinished(e -> weakPointLabel.setVisible(false));
        out.play();
    }

    // ─── PLAYER ACTIONS ──────────────────────────────────────────

    @FXML
    void onSwordAttack(ActionEvent event) {
        disableMenu();
        boolean crit = random.nextDouble() < CRIT_CHANCE_ATTACK;
        battleTextLabel.setText(crit ? "Perseus strikes true — a CRITICAL HIT incoming!" : "Perseus dashes forward with his sword!");

        playPerseusAnim(3, 0, 4, 400, true);
        TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
        moveFwd.setByX(400);
        moveFwd.setOnFinished(e -> {
            playPerseusAnim(0, 1, 2, 400, false);
            playMedusaAnim(5, 0, 1, 400, false);

            damageEnemy(0.2, crit);
            addUltimateCharge(0.34);

            if (enemyHealth > 0) {
                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(ev -> {
                    playPerseusAnim(3, 0, 4, 400, true);
                    TranslateTransition moveBack = new TranslateTransition(Duration.millis(400), perseusSprite);
                    moveBack.setByX(-400);
                    moveBack.setOnFinished(backEv -> {
                        startIdles();
                        enemyTurn();
                    });
                    moveBack.play();
                });
                pause.play();
            }
        });
        moveFwd.play();
    }

    @FXML
    void onShieldBlock(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus readies his mirror shield!");
        isPlayerDefending = true;

        playPerseusAnim(2, 0, 4, 600, false);

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            startIdles();
            enemyTurn();
        });
        pause.play();
    }

    @FXML
    void onFocus(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus channels starlight, charging his resolve...");
        playPerseusAnim(1, 0, 1, 700, true);
        addUltimateCharge(0.4);
        spawnHitParticles(perseusSprite, Color.web("#67e8f9"), 14);

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            startIdles();
            enemyTurn();
        });
        pause.play();
    }

    @FXML
    void onSpecialAttack(ActionEvent event) {
        if (ultimateCharge < 1.0) return;

        ultimateCharge = 0.0;
        ultimateBar.setProgress(0);
        specialButton.setDisable(true);

        disableMenu();
        boolean crit = random.nextDouble() < CRIT_CHANCE_SPECIAL;
        battleTextLabel.setText("Perseus unleashes a devastating leap strike!");
        flashScreen();

        playPerseusAnim(3, 0, 4, 400, true);
        TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
        moveFwd.setByX(400);
        moveFwd.setOnFinished(e -> {
            playPerseusAnim(0, 1, 3, 600, false);
            playMedusaAnim(5, 0, 1, 600, false);

            damageEnemy(0.35, crit);

            if (enemyHealth > 0) {
                PauseTransition pause = new PauseTransition(Duration.millis(800));
                pause.setOnFinished(ev -> {
                    playPerseusAnim(3, 0, 4, 400, true);
                    TranslateTransition moveBack = new TranslateTransition(Duration.millis(400), perseusSprite);
                    moveBack.setByX(-400);
                    moveBack.setOnFinished(backEv -> {
                        startIdles();
                        enemyTurn();
                    });
                    moveBack.play();
                });
                pause.play();
            }
        });
        moveFwd.play();
    }

    @FXML
    void onPotion(ActionEvent event) {
        if (potionsLeft <= 0) return;

        potionsLeft--;
        potionsLabel.setText("🧪 x" + potionsLeft);
        potionButton.setDisable(potionsLeft <= 0);

        disableMenu();
        battleTextLabel.setText("Perseus drinks a vial of starlight — health restored!");

        playerHealth = Math.min(1.0, playerHealth + 0.25);
        playerHealthBar.setProgress(playerHealth);
        showDamagePopup(perseusSprite, "+25", "#86efac");
        spawnHitParticles(perseusSprite, Color.web("#4ade80"), 16);

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            startIdles();
            enemyTurn();
        });
        pause.play();
    }

    @FXML
    void onRun(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus searches for a chance to flee...");
        playPerseusAnim(3, 0, 4, 600, true);

        boolean escaped = random.nextDouble() < FLEE_CHANCE;

        TranslateTransition moveBack = new TranslateTransition(Duration.millis(600), perseusSprite);
        moveBack.setByX(-100);
        moveBack.setOnFinished(e -> {
            if (escaped) {
                battleTextLabel.setText("Perseus vanishes into the starlit mist — a narrow escape!");
                PauseTransition p = new PauseTransition(Duration.seconds(1.5));
                p.setOnFinished(ev -> fleeToHub());
                p.play();
            } else {
                TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
                moveFwd.setByX(100);
                moveFwd.setOnFinished(ev -> {
                    battleTextLabel.setText("Medusa blocks the escape route!");
                    startIdles();
                    enemyTurn();
                });
                moveFwd.play();
            }
        });
        moveBack.play();
    }

    // ─── ENEMY TURN ──────────────────────────────────────────────

    private void enemyTurn() {
        battleTextLabel.setText("Medusa is preparing to strike...");

        PauseTransition wait = new PauseTransition(Duration.seconds(1));
        wait.setOnFinished(e -> {
            int attackType = random.nextInt(3);

            if (attackType == 0) {
                battleTextLabel.setText("Medusa slithers in for a strike!");
                playMedusaAnim(1, 0, 4, 400, true);
                TranslateTransition slither = new TranslateTransition(Duration.millis(400), medusaSprite);
                slither.setByX(-400);
                slither.setOnFinished(ev -> {
                    playMedusaAnim(2, 0, 4, 400, false);
                    applyEnemyDamage(0.15);

                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(pEv -> {
                        playMedusaAnim(1, 0, 4, 400, true);
                        TranslateTransition slitherBack = new TranslateTransition(Duration.millis(400), medusaSprite);
                        slitherBack.setByX(400);
                        slitherBack.setOnFinished(bEv -> finishEnemyTurn());
                        slitherBack.play();
                    });
                    pause.play();
                });
                slither.play();

            } else if (attackType == 1) {
                battleTextLabel.setText("Medusa summons a Cobra blast!");
                playMedusaAnim(3, 0, 4, 600, false);
                applyEnemyDamage(0.15);
                PauseTransition p = new PauseTransition(Duration.seconds(1));
                p.setOnFinished(ev -> finishEnemyTurn());
                p.play();

            } else {
                battleTextLabel.setText("Medusa uses PETRIFYING GAZE!");
                playMedusaAnim(4, 0, 4, 800, false);

                if (isPlayerDefending) {
                    battleTextLabel.setText("The shield reflected the gaze! Medusa takes damage!");
                    playMedusaAnim(5, 0, 1, 500, false);
                    damageEnemy(0.2, false);
                } else {
                    applyEnemyDamage(0.3);
                }

                PauseTransition p = new PauseTransition(Duration.seconds(1.5));
                p.setOnFinished(ev -> finishEnemyTurn());
                p.play();
            }
        });
        wait.play();
    }

    private void applyEnemyDamage(double amount) {
        if (isPlayerDefending) {
            battleTextLabel.setText("Perseus blocked the attack!");
            playPerseusAnim(2, 0, 4, 500, false);
            spawnHitParticles(perseusSprite, Color.web("#38bdf8"), 10);
        } else {
            playerHealth -= amount;
            playerHealthBar.setProgress(Math.max(0, playerHealth));
            playPerseusAnim(1, 2, 1, 400, false);
            showDamagePopup(perseusSprite, "-" + Math.round(amount * 100), "#93c5fd");
            spawnHitParticles(perseusSprite, Color.web("#ef4444"), 12);
            hitFlash(perseusSprite);
            if (amount >= 0.3) {
                screenShake();
                flashScreen();
            }
        }
    }

    private void damageEnemy(double baseAmount, boolean crit) {
        double amount = baseAmount;
        boolean weakPointHit = false;

        if (crit) amount *= 1.6;

        if (weakPointAvailable) {
            amount *= 1.5;
            weakPointAvailable = false;
            weakPointHit = true;
            hideWeakPointBanner();
            battleTextLabel.setText("Medusa's weak point was struck for bonus damage!");
        }

        enemyHealth -= amount;
        enemyHealthBar.setProgress(Math.max(0, enemyHealth));

        String prefix = crit ? "CRIT -" : weakPointHit ? "WEAK! -" : "-";
        String popupColor = (crit || weakPointHit) ? "#fde047" : "#fca5a5";
        showDamagePopup(medusaSprite, prefix + Math.round(amount * 100), popupColor);
        spawnHitParticles(medusaSprite, Color.web(popupColor), (crit || weakPointHit) ? 22 : 12);
        hitFlash(medusaSprite);

        if (crit || weakPointHit || amount >= 0.3) {
            screenShake();
            flashScreen();
        }

        // Only re-arm the weak point on a hit that didn't just consume it.
        if (!weakPointHit && enemyHealth > 0 && enemyHealth <= 0.3 && !weakPointAvailable) {
            weakPointAvailable = true;
            showWeakPointBanner();
        }

        if (enemyHealth <= 0) {
            endGame(true);
        }
    }

    private void addUltimateCharge(double amount) {
        ultimateCharge = Math.min(1.0, ultimateCharge + amount);
        ultimateBar.setProgress(ultimateCharge);
        specialButton.setDisable(ultimateCharge < 1.0);
    }

    private void finishEnemyTurn() {
        if (playerHealth <= 0) {
            endGame(false);
        } else if (enemyHealth > 0) {
            startIdles();
            isPlayerDefending = false;
            actionMenu.setDisable(false);
            specialButton.setDisable(ultimateCharge < 1.0);
            potionButton.setDisable(potionsLeft <= 0);
            battleTextLabel.setText("What will Perseus do?");
        }
    }

    // ─── END GAME LOGIC ──────────────────────────────────────────

    private void endGame(boolean playerWon) {
        actionMenu.setVisible(false);
        backButton.setVisible(true);

        if (playerWon) {
            battleTextLabel.setText("VICTORY! Medusa has been defeated!");

            // Award 100 StarDust and permanently save it to MySQL
            if (currentPlayer != null) {
                ScoreService.addStarDust(currentPlayer, 100);
            }

            // Medusa Death (Row 5, 4 frames)
            playMedusaAnim(5, 0, 4, 1000, false);
            playPerseusAnim(0, 2, 1, 800, true);
        } else {
            battleTextLabel.setText("DEFEAT... Perseus has fallen.");
            playPerseusAnim(1, 2, 2, 1000, false);
            playMedusaAnim(0, 0, 4, 800, true);
        }
    }

    @FXML
    void returnToHub(ActionEvent event) {
        if (galaxyTimer != null) galaxyTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            if (currentPlayer != null) {
                controller.setPlayer(currentPlayer);
            }
            Stage stage = (Stage) backButton.getScene().getWindow();
            SceneManager.switchScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fleeToHub() {
        if (galaxyTimer != null) galaxyTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            if (currentPlayer != null) {
                controller.setPlayer(currentPlayer);
            }
            Stage stage = (Stage) fleeButton.getScene().getWindow();
            SceneManager.switchScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disableMenu() {
        actionMenu.setDisable(true);
        isPlayerDefending = false;
    }

    // ─── ANIMATION ENGINES ───────────────────────────────────────

    private void playPerseusAnim(int row, int startCol, int frameCount, int durationMs, boolean loop) {
        if (perseusAnim != null) perseusAnim.stop();
        perseusAnim = buildTimeline(perseusSprite, row, startCol, frameCount, durationMs, loop, PERSEUS_W, PERSEUS_H);
        perseusAnim.play();
    }

    private void playMedusaAnim(int row, int startCol, int frameCount, int durationMs, boolean loop) {
        if (medusaAnim != null) medusaAnim.stop();
        medusaAnim = buildTimeline(medusaSprite, row, startCol, frameCount, durationMs, loop, MEDUSA_W, MEDUSA_H);
        medusaAnim.play();
    }

    private Timeline buildTimeline(ImageView sprite, int row, int startCol, int frames, int dur, boolean loop, double w, double h) {
        Timeline t = new Timeline();
        t.setCycleCount(loop ? Timeline.INDEFINITE : 1);
        double timePerFrame = (double) dur / frames;

        double tempYOffset = 0.0;
        double tempHeightCrop = 0.0;

        if (sprite == medusaSprite) {
            tempYOffset = 18.0;
            tempHeightCrop = 10.0;
        } else if (sprite == perseusSprite) {
            if (row == 1) {
                tempYOffset = 12.0;
                tempHeightCrop = -10.0;
            } else if (row == 3) {
                tempYOffset = -10.0;
                tempHeightCrop = -10.0;
            } else if (row == 0) {
                tempYOffset = -10.0;
                tempHeightCrop = -10.0;
            } else if (row == 2) {
                tempYOffset = 15.0;
                tempHeightCrop = 30.0;
            }
        }

        final double finalYOffset = tempYOffset;
        final double finalHeightCrop = tempHeightCrop;

        for (int i = 0; i < frames; i++) {
            final int col = startCol + i;
            KeyFrame kf = new KeyFrame(Duration.millis(timePerFrame * (i + 1)), e ->
                    sprite.setViewport(new Rectangle2D(col * w, (row * h) + finalYOffset, w, h - finalHeightCrop)));
            t.getKeyFrames().add(kf);
        }
        return t;
    }
}