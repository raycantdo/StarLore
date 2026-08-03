package com.starlore.starlore;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class BattleController {

    @FXML private ImageView backgroundImageView;
    @FXML private ImageView perseusSprite;
    @FXML private ImageView medusaSprite;
    @FXML private ProgressBar playerHealthBar;
    @FXML private ProgressBar enemyHealthBar;
    @FXML private Label battleTextLabel;
    @FXML private HBox actionMenu;
    @FXML private Button backButton;

    private double playerHealth = 1.0;
    private double enemyHealth = 1.0;
    private Player currentPlayer;
    private boolean isPlayerDefending = false;
    private Random random = new Random();

    private double PERSEUS_W, PERSEUS_H;
    private double MEDUSA_W, MEDUSA_H;

    private Timeline perseusAnim, medusaAnim;

    @FXML
    public void initialize() {
        // Load Background
        backgroundImageView.setImage(new Image(getClass().getResource("images/battle_gemini.png").toExternalForm()));

        // Load Perseus (4 Columns, 4 Rows)
        Image pSheet = new Image(getClass().getResource("images/perseus.png").toExternalForm());
        perseusSprite.setImage(pSheet);
        PERSEUS_W = (int) (pSheet.getWidth() / 4);
        PERSEUS_H = (int) (pSheet.getHeight() / 4);

        // Load Medusa (4 Columns, 6 Rows)
        Image mSheet = new Image(getClass().getResource("images/medusa1.png").toExternalForm());
        medusaSprite.setImage(mSheet);
        MEDUSA_W = (int) (mSheet.getWidth() / 4);
        MEDUSA_H = (int) (mSheet.getHeight() / 6);
        // Push Medusa down so she isn't floating
        medusaSprite.setLayoutY(360.0); // Increase this number to push her lower, decrease to raise her
        startIdles();
        // Start battle with Medusa attacking first spontaneously
        /*PauseTransition introDelay = new PauseTransition(Duration.seconds(1.5));
        introDelay.setOnFinished(e -> {
            battleTextLabel.setText("Medusa coming to attack!");
            disableMenu();
            enemyTurn();
        });
        introDelay.play();*/
    }

    private void startIdles() {
        // Perseus Idle (Row 1, Col 0 - Stance)
        playPerseusAnim(1, 0, 1, 800, true);
        // Medusa Idle (Row 0, 4 frames)
        playMedusaAnim(0, 0, 4, 800, true);
    }

    // ─── PLAYER ACTIONS ──────────────────────────────────────────

    @FXML
    void onSwordAttack(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus dashes forward with his sword!");

        // Perseus Run (Row 3, 4 frames)
        playPerseusAnim(3, 0, 4, 400, true);
        TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
        moveFwd.setByX(400);
        moveFwd.setOnFinished(e -> {

            // Perseus Leaping Attack (Row 0, Starts at Col 1, 3 frames)
            playPerseusAnim(0, 1, 2, 400, false);
            // Medusa Hurt Flinch (First frame of Death animation - Row 5, Col 0)
            playMedusaAnim(5, 0, 1, 400, false);

            damageEnemy(0.2);

            if (enemyHealth > 0) {
                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(ev -> {
                    // Perseus Run Back
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

        // Perseus Block (Row 2, 4 frames)
        playPerseusAnim(2, 0, 4, 600, false);

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            startIdles();
            enemyTurn();
        });
        pause.play();
    }

    @FXML
    void onSpecialAttack(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus unleashes a devastating leap strike!");

        // 1. Perseus runs forward first
        playPerseusAnim(3, 0, 4, 400, true);
        TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
        moveFwd.setByX(400);
        moveFwd.setOnFinished(e -> {

            // 2. Perform the Special Leap (Row 0, Starts at Col 1, 3 frames)
            playPerseusAnim(0, 1, 3, 600, false);
            playMedusaAnim(5, 0, 1, 600, false);  // Medusa Flinch

            damageEnemy(0.35);

            if (enemyHealth > 0) {
                PauseTransition pause = new PauseTransition(Duration.millis(800));
                pause.setOnFinished(ev -> {
                    // 3. Perseus runs back
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
    void onRun(ActionEvent event) {
        disableMenu();
        battleTextLabel.setText("Perseus tries to flee... but there is no escape!");
        // Perseus Run (Row 3, 4 frames)
        playPerseusAnim(3, 0, 4, 600, true);

        TranslateTransition moveBack = new TranslateTransition(Duration.millis(600), perseusSprite);
        moveBack.setByX(-100);
        moveBack.setOnFinished(e -> {
            TranslateTransition moveFwd = new TranslateTransition(Duration.millis(400), perseusSprite);
            moveFwd.setByX(100);
            moveFwd.setOnFinished(ev -> {
                startIdles();
                enemyTurn();
            });
            moveFwd.play();
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
                // Melee Slither Attack
                battleTextLabel.setText("Medusa slithers in for a strike!");
                playMedusaAnim(1, 0, 4, 400, true); // Slither Row 1
                TranslateTransition slither = new TranslateTransition(Duration.millis(400), medusaSprite);
                slither.setByX(-400);
                slither.setOnFinished(ev -> {
                    playMedusaAnim(2, 0, 4, 400, false); // Melee Row 2
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
                // Ranged Venom Attack
                battleTextLabel.setText("Medusa summons a Cobra blast!");
                playMedusaAnim(3, 0, 4, 600, false); // Cobra Row 3
                applyEnemyDamage(0.15);
                PauseTransition p = new PauseTransition(Duration.seconds(1));
                p.setOnFinished(ev -> finishEnemyTurn());
                p.play();

            } else {
                // Special: Stone Gaze
                battleTextLabel.setText("Medusa uses PETRIFYING GAZE!");
                playMedusaAnim(4, 0, 4, 800, false); // Gaze Row 4

                if (isPlayerDefending) {
                    battleTextLabel.setText("The shield reflected the gaze! Medusa takes damage!");
                    playMedusaAnim(5, 0, 1, 500, false); // Medusa flinch
                    damageEnemy(0.2);
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
            amount = 0; // Blocked
            battleTextLabel.setText("Perseus blocked the attack!");
            playPerseusAnim(2, 0, 4, 500, false); // Shield Block
        } else {
            playerHealth -= amount;
            playerHealthBar.setProgress(playerHealth);
            // Perseus Hurt (Row 1, Col 2)
            playPerseusAnim(1, 2, 1, 400, false);
        }
    }

    private void damageEnemy(double amount) {
        enemyHealth -= amount;
        enemyHealthBar.setProgress(enemyHealth);
        if (enemyHealth <= 0) {
            endGame(true);
        }
    }

    private void finishEnemyTurn() {
        if (playerHealth <= 0) {
            endGame(false);
        } else if (enemyHealth > 0) {
            startIdles();
            isPlayerDefending = false;
            actionMenu.setDisable(false);
            battleTextLabel.setText("What will Perseus do?");
        }
    }

    // ─── END GAME LOGIC ──────────────────────────────────────────

    private void endGame(boolean playerWon) {
        actionMenu.setVisible(false);
        backButton.setVisible(true);

        if (playerWon) {
            battleTextLabel.setText("VICTORY! Medusa has been defeated!");
            // Medusa Death (Row 5, 4 frames)
            playMedusaAnim(5, 0, 4, 1000, false);
            playPerseusAnim(0, 2, 1, 800, true);
        } else {
            battleTextLabel.setText("DEFEAT... Perseus has fallen.");
            // Perseus Death/Kneel (Row 1, Col 3)
            playPerseusAnim(1, 2, 2, 1000, false);
            playMedusaAnim(0, 0, 4, 800, true);
        }
    }

    @FXML
    void returnToHub(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("GameHubView.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
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
        double tempWidthCrop = 0.0;

        double tempXOffset = 0.0;

        if (sprite == medusaSprite) {
            tempYOffset = 18.0;
            tempHeightCrop = 10.0;
        } else if (sprite == perseusSprite) {
            if (row == 1) { // IDLE / HURT / DEFEAT
                // Shift camera down to hide the sword bleed, but expand height so legs aren't cut!
                tempYOffset = 12.0;
                tempHeightCrop = -10.0;
            } else if (row == 3) { // RUN
                tempYOffset = -10.0;
                tempHeightCrop = -10.0;
            } else if (row == 0) { // SWORD ATTACK
                // Move camera up slightly to catch the sword, expand height heavily to catch feet
                tempYOffset = -10.0;
                tempHeightCrop = -10.0;
            } else if (row == 2) { // SHIELD BLOCK
                tempYOffset = 15.0;
                tempHeightCrop = 30.0;
            }
        }

        final double finalYOffset = tempYOffset;
        final double finalHeightCrop = tempHeightCrop;
        final double finalWidthCrop = tempWidthCrop;
        final double finalXOffset = tempXOffset;

        for (int i = 0; i < frames; i++) {
            final int col = startCol + i;
            KeyFrame kf = new KeyFrame(Duration.millis(timePerFrame * (i + 1)), e -> {

                sprite.setViewport(new Rectangle2D(
                        (col * w) ,
                        (row * h) + finalYOffset,
                        w ,
                        h - finalHeightCrop
                ));

            });
            t.getKeyFrames().add(kf);
        }
        return t;
    }
}