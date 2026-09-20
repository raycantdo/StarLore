package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MythQuizController {

    @FXML private StackPane rootStackPane;
    @FXML private Canvas quizCanvas;
    @FXML private AnchorPane astrolabeWheel;
    @FXML private StackPane questionCore;
    @FXML private StackPane outerRing;
    @FXML private StackPane innerRing;
    @FXML private StackPane glassTrack;
    @FXML private Button confirmButton;
    @FXML private Button spinLeftBtn;
    @FXML private Button spinRightBtn;

    @FXML private Label questionLabel;
    @FXML private Label feverIcon;
    @FXML private Label ans0Label, ans1Label, ans2Label, ans3Label;
    @FXML private StackPane ans0Pane, ans1Pane, ans2Pane, ans3Pane;
    @FXML private Label bracket0, bracket1, bracket2, bracket3;

    @FXML private Label questionCounterLabel;
    @FXML private Label scoreLabel;
    @FXML private Label dustLabel;

    @FXML private Button btnBlackHole, btnAegisShield, btnWhisper;

    @FXML private StackPane resultOverlay;
    @FXML private Label resultTitle;
    @FXML private Label resultDesc;

    @FXML private VBox achievementBox;
    @FXML private Label achievementLabel;
    @FXML private Label rewardLabel;

    private Player currentPlayer;
    private AnimationTimer bgTimer;
    private double timeElapsed = 0;
    private final Random random = new Random();

    // Mechanics State
    private double currentRotation = 0;
    private boolean isSpinning = false;
    private int topAnswerIndex = 0;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int currentStreak = 0;
    private int correctIndexOnWheel = 0;

    // Advanced Modifiers
    private boolean isFeverMode = false;
    private boolean isShieldActive = false;

    private static class Particle {
        double x, y, vx, vy, life;
        Color color;
        Particle(double x, double y, double vx, double vy, Color c) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = 1.0; this.color = c;
        }
    }
    private final List<Particle> particles = new ArrayList<>();

    private static class Question {
        String query;
        String[] options;
        Question(String q, String[] o) { this.query = q; this.options = o; }
    }

    // EXPANDED ARCHIVES (20 Questions)
    private final Question[] oracleData = {
            new Question("Who was the legendary hunter struck down by a giant scorpion?", new String[]{"Orion", "Perseus", "Hercules", "Theseus"}),
            new Question("Which constellation represents the vain queen tied to a chair?", new String[]{"Cassiopeia", "Andromeda", "Lyra", "Cygnus"}),
            new Question("What is the North Star, anchoring the Little Dipper?", new String[]{"Polaris", "Sirius", "Vega", "Betelgeuse"}),
            new Question("Which hero used Medusa's head to save a princess?", new String[]{"Perseus", "Achilles", "Jason", "Hector"}),
            new Question("What mythological creature does Pegasus represent?", new String[]{"Winged Horse", "Sea Monster", "Lion", "Dragon"}),
            new Question("Who is the Titan condemned to hold up the celestial heavens?", new String[]{"Atlas", "Cronus", "Prometheus", "Oceanus"}),
            new Question("Which zodiac constellation is known as the 'Bull of Heaven'?", new String[]{"Taurus", "Aries", "Capricorn", "Leo"}),
            new Question("What constellation represents the great bear?", new String[]{"Ursa Major", "Ursa Minor", "Draco", "Lupus"}),
            new Question("Who was the ferryman of the underworld in Greek mythology?", new String[]{"Charon", "Hades", "Cerberus", "Thanatos"}),
            new Question("Which star is known as the 'Dog Star' and is the brightest in the night sky?", new String[]{"Sirius", "Rigel", "Altair", "Antares"}),
            new Question("What mythological figure flew too close to the sun?", new String[]{"Icarus", "Daedalus", "Phaethon", "Bellerophon"}),
            new Question("Which Greek goddess is associated with the moon and the hunt?", new String[]{"Artemis", "Athena", "Aphrodite", "Hera"}),
            new Question("The Pleiades star cluster is also known as what?", new String[]{"Seven Sisters", "The Hunters", "The Chariot", "The Crown"}),
            new Question("What weapon is the constellation Sagitta supposed to represent?", new String[]{"Arrow", "Sword", "Shield", "Spear"}),
            new Question("Which hero completed the Twelve Labors?", new String[]{"Hercules", "Theseus", "Perseus", "Odysseus"}),
            new Question("Who was the messenger of the gods, wearing winged sandals?", new String[]{"Hermes", "Apollo", "Ares", "Hephaestus"}),
            new Question("What constellation represents the mythical twins Castor and Pollux?", new String[]{"Gemini", "Pisces", "Libra", "Virgo"}),
            new Question("Which monster had snakes for hair and turned people to stone?", new String[]{"Medusa", "Chimera", "Hydra", "Sphinx"}),
            new Question("What river formed the boundary between Earth and the Underworld?", new String[]{"Styx", "Lethe", "Acheron", "Phlegethon"}),
            new Question("Which constellation represents the harp played by Orpheus?", new String[]{"Lyra", "Cygnus", "Aquila", "Corona Borealis"})
    };

    private List<Question> shuffledQuiz = new ArrayList<>();

    @FXML
    public void initialize() {
        SoundManager.playMenuMusic();

        if (rootStackPane != null) {
            quizCanvas.widthProperty().bind(rootStackPane.widthProperty());
            quizCanvas.heightProperty().bind(rootStackPane.heightProperty());
        }

        // Shuffle the 20 questions
        for (Question q : oracleData) shuffledQuiz.add(q);
        Collections.shuffle(shuffledQuiz);

        setupCoreBreathing();
        setupConfirmButtonPulse();
        startBackgroundAnimation();
        updateLifelineButtons();
        loadQuestion();
    }

    public void setPlayer(Player player) {
        this.currentPlayer = player;
        updateLifelineButtons();
    }

    private void setupCoreBreathing() {
        ScaleTransition st = new ScaleTransition(Duration.seconds(2), questionCore);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.03); st.setToY(1.03);
        st.setAutoReverse(true);
        st.setCycleCount(Animation.INDEFINITE);
        st.play();
    }

    private void setupConfirmButtonPulse() {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(confirmButton.scaleXProperty(), 1.0), new KeyValue(confirmButton.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(confirmButton.scaleXProperty(), 1.04), new KeyValue(confirmButton.scaleYProperty(), 1.04)),
                new KeyFrame(Duration.seconds(2), new KeyValue(confirmButton.scaleXProperty(), 1.0), new KeyValue(confirmButton.scaleYProperty(), 1.0))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    // ─── LIFELINES (STAR DUST MECHANICS) ───

    private void updateLifelineButtons() {
        if (currentPlayer == null) return;
        int dust = currentPlayer.getTotalStarDust();
        dustLabel.setText("✦ DUST: " + dust);

        btnBlackHole.setDisable(dust < 50);
        btnAegisShield.setDisable(dust < 75 || isShieldActive);
        btnWhisper.setDisable(dust < 100);
    }

    @FXML
    private void useBlackHole() {
        if (currentPlayer.getTotalStarDust() < 50) return;
        ScoreService.spendStarDust(currentPlayer, 50);
        updateLifelineButtons();
        btnBlackHole.setDisable(true); // Once per question

        // Hide 2 wrong answers
        int hiddenCount = 0;
        StackPane[] panes = {ans0Pane, ans1Pane, ans2Pane, ans3Pane};
        for (int i = 0; i < 4; i++) {
            if (i != correctIndexOnWheel && hiddenCount < 2) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), panes[i]);
                ft.setToValue(0.1);
                ft.play();
                hiddenCount++;
            }
        }
        spawnParticles(Color.web("#a855f7"), 30); // Purple blast
    }

    @FXML
    private void useAegisShield() {
        if (currentPlayer.getTotalStarDust() < 75 || isShieldActive) return;
        ScoreService.spendStarDust(currentPlayer, 75);
        isShieldActive = true;
        updateLifelineButtons();

        // Visual cue on the question core
        questionCore.setStyle(questionCore.getStyle() + "-fx-border-color: linear-gradient(to bottom right, #38bdf8, #0ea5e9);");
        spawnParticles(Color.web("#38bdf8"), 30); // Cyan shield blast
    }

    @FXML
    private void useOraclesWhisper() {
        if (currentPlayer.getTotalStarDust() < 100 || isSpinning) return;
        ScoreService.spendStarDust(currentPlayer, 100);
        updateLifelineButtons();
        btnWhisper.setDisable(true);

        // Auto-rotate to the correct answer
        while (topAnswerIndex != correctIndexOnWheel) {
            currentRotation += 90;
            topAnswerIndex = (topAnswerIndex + 3) % 4; // Right spin logic
        }
        executeFerrisWheelSpin();
        spawnParticles(Color.web("#facc15"), 50); // Gold blast
    }

    // ─── GAME LOGIC ───
    private static final int MAX_QUESTIONS = 10;
    private void loadQuestion() {
        // Stop the quiz when it hits the 10-question cap, instead of 20
        if (currentQuestionIndex >= MAX_QUESTIONS) {
            finishQuiz();
            return;
        }

        achievementBox.setVisible(false);
        resultOverlay.setVisible(false);
        isShieldActive = false;

        // Reset Lifelines
        ans0Pane.setOpacity(1.0); ans1Pane.setOpacity(1.0);
        ans2Pane.setOpacity(1.0); ans3Pane.setOpacity(1.0);
        updateLifelineButtons();

        Question q = shuffledQuiz.get(currentQuestionIndex);
        questionLabel.setText(q.query);

        // Update the label to show out of 10
        questionCounterLabel.setText("[ QUERY " + String.format("%02d", currentQuestionIndex + 1) + " / " + MAX_QUESTIONS + " ]");

        int[] slots = {0, 1, 2, 3};
        for (int i = 0; i < slots.length; i++) {
            int swap = random.nextInt(4);
            int temp = slots[i];
            slots[i] = slots[swap];
            slots[swap] = temp;
        }

        ans0Label.setText(q.options[slots[0]]);
        ans1Label.setText(q.options[slots[1]]);
        ans2Label.setText(q.options[slots[2]]);
        ans3Label.setText(q.options[slots[3]]);

        for (int i = 0; i < 4; i++) {
            if (slots[i] == 0) {
                correctIndexOnWheel = i;
                break;
            }
        }

        currentRotation = 0;
        astrolabeWheel.setRotate(0);
        ans0Pane.setRotate(0); ans1Pane.setRotate(0);
        ans2Pane.setRotate(0); ans3Pane.setRotate(0);

        topAnswerIndex = 0;
        updateHighlight();
    }

    @FXML
    private void spinLeft() {
        if (isSpinning) return;
        isSpinning = true;
        currentRotation -= 90;
        topAnswerIndex = (topAnswerIndex + 1) % 4;
        executeFerrisWheelSpin();
    }

    @FXML
    private void spinRight() {
        if (isSpinning) return;
        isSpinning = true;
        currentRotation += 90;
        topAnswerIndex = (topAnswerIndex + 3) % 4;
        executeFerrisWheelSpin();
    }

    private void executeFerrisWheelSpin() {
        // Fever mode makes it spin faster!
        double speed = isFeverMode ? 150 : 250;

        RotateTransition wheelRt = new RotateTransition(Duration.millis(speed), astrolabeWheel);
        wheelRt.setToAngle(currentRotation);
        wheelRt.setInterpolator(Interpolator.EASE_BOTH);

        double counterAngle = -currentRotation;
        RotateTransition p0 = new RotateTransition(Duration.millis(speed), ans0Pane); p0.setToAngle(counterAngle);
        RotateTransition p1 = new RotateTransition(Duration.millis(speed), ans1Pane); p1.setToAngle(counterAngle);
        RotateTransition p2 = new RotateTransition(Duration.millis(speed), ans2Pane); p2.setToAngle(counterAngle);
        RotateTransition p3 = new RotateTransition(Duration.millis(speed), ans3Pane); p3.setToAngle(counterAngle);

        ParallelTransition pt = new ParallelTransition(wheelRt, p0, p1, p2, p3);
        pt.setOnFinished(e -> {
            isSpinning = false;
            updateHighlight();
        });
        pt.play();
    }

    private void updateHighlight() {
        String base = "-fx-background-color: rgba(2, 5, 15, 0.95); -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 2.5; ";

        // Color depends on fever mode
        String baseBorder = isFeverMode ? "#fbbf24" : "#38bdf8";
        String hoverColor = isFeverMode ? "#f97316" : "#fde047";

        ans0Pane.setStyle(base + "-fx-border-color: " + baseBorder + ";");
        ans1Pane.setStyle(base + "-fx-border-color: " + baseBorder + ";");
        ans2Pane.setStyle(base + "-fx-border-color: " + baseBorder + ";");
        ans3Pane.setStyle(base + "-fx-border-color: " + baseBorder + ";");

        bracket0.setVisible(false); bracket1.setVisible(false);
        bracket2.setVisible(false); bracket3.setVisible(false);

        String highlight = base + "-fx-background-color: rgba(20, 15, 5, 0.95); -fx-border-color: " + hoverColor + "; -fx-border-width: 3.5; -fx-effect: dropshadow(gaussian, " + hoverColor + ", 25, 0.6, 0, 0);";

        if (topAnswerIndex == 0) { ans0Pane.setStyle(highlight); bracket0.setVisible(true); bracket0.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: black;");}
        else if (topAnswerIndex == 1) { ans1Pane.setStyle(highlight); bracket1.setVisible(true); bracket1.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: black;");}
        else if (topAnswerIndex == 2) { ans2Pane.setStyle(highlight); bracket2.setVisible(true); bracket2.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: black;");}
        else if (topAnswerIndex == 3) { ans3Pane.setStyle(highlight); bracket3.setVisible(true); bracket3.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: black;");}
    }

    @FXML
    private void submitAnswer() {
        if (isSpinning) return;

        boolean isCorrect = (topAnswerIndex == correctIndexOnWheel);

        if (isCorrect) {
            int pts = isFeverMode ? 20 : 10;
            score += pts;
            currentStreak++;
            scoreLabel.setText("WISDOM: " + score);

            ScaleTransition scoreBounce = new ScaleTransition(Duration.millis(250), scoreLabel);
            scoreBounce.setFromX(1.0); scoreBounce.setFromY(1.0);
            scoreBounce.setToX(1.4); scoreBounce.setToY(1.4);
            scoreBounce.setAutoReverse(true); scoreBounce.setCycleCount(2);
            scoreBounce.play();

            resultTitle.setText(isFeverMode ? "FEVER ALIGNMENT" : "CORRECT ALIGNMENT");
            resultTitle.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 52px; -fx-text-fill: #34d399; -fx-effect: dropshadow(gaussian, #10b981, 30, 0.6, 0, 0);");
            resultDesc.setText("+" + pts + " WISDOM ACQUIRED");

            spawnParticles(Color.web(isFeverMode ? "#fbbf24" : "#34d399"), 40);

            if (currentStreak >= 3 && !isFeverMode) activateFeverMode();

            // Achievements
            if (currentStreak == 3) showAchievement("🔮 ORACLE'S INITIATE", 50);
            else if (currentStreak == 7) showAchievement("🌌 NEBULA SCHOLAR", 100);
            else if (currentStreak == 12) showAchievement("👑 MASTER OF THE COSMOS", 250);

        } else {
            if (isShieldActive) {
                // Shield saves you
                resultTitle.setText("AEGIS DEPLOYED");
                resultTitle.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 52px; -fx-text-fill: #38bdf8; -fx-effect: dropshadow(gaussian, #0284c7, 30, 0.6, 0, 0);");
                resultDesc.setText("STREAK PROTECTED. SHIELD CONSUMED.");
                spawnParticles(Color.web("#38bdf8"), 30);
            } else {
                // Total failure -> Screen Shake!
                currentStreak = 0;
                if (isFeverMode) deactivateFeverMode();

                triggerScreenShake();

                resultTitle.setText("MISALIGNMENT DETECTED");
                resultTitle.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 52px; -fx-text-fill: #ef4444; -fx-effect: dropshadow(gaussian, #dc2626, 30, 0.6, 0, 0);");
                resultDesc.setText("THE ORACLE REJECTS YOUR ANSWER");
                spawnParticles(Color.web("#ef4444"), 40);
            }
        }

        resultOverlay.setVisible(true);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), resultTitle);
        st.setFromX(0.5); st.setFromY(0.5); st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(achievementBox.isVisible() ? 3.0 : 1.5));
        pause.setOnFinished(e -> {
            currentQuestionIndex++;
            loadQuestion();
        });
        pause.play();
    }

    private void triggerScreenShake() {
        TranslateTransition tt1 = new TranslateTransition(Duration.millis(40), rootStackPane); tt1.setByX(15);
        TranslateTransition tt2 = new TranslateTransition(Duration.millis(40), rootStackPane); tt2.setByX(-30);
        TranslateTransition tt3 = new TranslateTransition(Duration.millis(40), rootStackPane); tt3.setByX(30);
        TranslateTransition tt4 = new TranslateTransition(Duration.millis(40), rootStackPane); tt4.setByX(-15);
        SequentialTransition seq = new SequentialTransition(tt1, tt2, tt3, tt4);
        seq.play();
    }

    private void activateFeverMode() {
        isFeverMode = true;
        feverIcon.setText("🔥");
        questionCore.setStyle("-fx-background-color: rgba(25, 5, 5, 0.95); -fx-border-color: linear-gradient(to bottom right, #fbbf24, #ea580c); -fx-background-radius: 200; -fx-border-radius: 200; -fx-border-width: 4; -fx-effect: dropshadow(gaussian, rgba(234, 88, 12, 0.5), 40, 0, 0, 0);");
        glassTrack.setStyle("-fx-background-color: rgba(25, 10, 5, 0.6); -fx-border-color: rgba(251, 191, 36, 0.5); -fx-border-radius: 500; -fx-border-width: 4;");
        innerRing.setStyle("-fx-border-color: rgba(234, 88, 12, 0.3); -fx-border-radius: 500; -fx-border-width: 2;");
        outerRing.setStyle("-fx-border-color: rgba(251, 191, 36, 0.15); -fx-border-radius: 500; -fx-border-width: 1; -fx-border-style: dashed;");
        spinLeftBtn.setStyle("-fx-background-color: rgba(25, 5, 5, 0.8); -fx-text-fill: #fbbf24; -fx-font-family: 'Impact'; -fx-font-size: 14px; -fx-border-color: #fbbf24; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 2; -fx-padding: 8 15;");
        spinRightBtn.setStyle("-fx-background-color: rgba(25, 5, 5, 0.8); -fx-text-fill: #fbbf24; -fx-font-family: 'Impact'; -fx-font-size: 14px; -fx-border-color: #fbbf24; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 2; -fx-padding: 8 15;");
        confirmButton.setStyle("-fx-background-color: linear-gradient(to right, #ea580c, #dc2626); -fx-text-fill: #ffffff; -fx-font-family: 'Impact'; -fx-font-size: 20px; -fx-border-color: #fbbf24; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 2.5; -fx-padding: 10 30; -fx-effect: dropshadow(gaussian, #ea580c, 25, 0.6, 0, 0);");
        updateHighlight();
    }

    private void deactivateFeverMode() {
        isFeverMode = false;
        feverIcon.setText("🔮");
        questionCore.setStyle("-fx-background-color: rgba(2, 5, 15, 0.95); -fx-border-color: linear-gradient(to bottom right, #34d399, #064e3b); -fx-background-radius: 200; -fx-border-radius: 200; -fx-border-width: 4; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 30, 0, 0, 0);");
        glassTrack.setStyle("-fx-background-color: rgba(6, 25, 20, 0.6); -fx-border-color: rgba(52, 211, 153, 0.5); -fx-border-radius: 500; -fx-border-width: 4;");
        innerRing.setStyle("-fx-border-color: rgba(16, 185, 129, 0.1); -fx-border-radius: 500; -fx-border-width: 2;");
        outerRing.setStyle("-fx-border-color: rgba(52, 211, 153, 0.05); -fx-border-radius: 500; -fx-border-width: 1; -fx-border-style: dashed;");
        spinLeftBtn.setStyle("-fx-background-color: rgba(2, 15, 10, 0.8); -fx-text-fill: #34d399; -fx-font-family: 'Impact'; -fx-font-size: 14px; -fx-border-color: #34d399; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 2; -fx-padding: 8 15;");
        spinRightBtn.setStyle("-fx-background-color: rgba(2, 15, 10, 0.8); -fx-text-fill: #34d399; -fx-font-family: 'Impact'; -fx-font-size: 14px; -fx-border-color: #34d399; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 2; -fx-padding: 8 15;");
        confirmButton.setStyle("-fx-background-color: linear-gradient(to right, rgba(16, 185, 129, 0.6), rgba(5, 150, 105, 0.8)); -fx-text-fill: #ffffff; -fx-font-family: 'Impact'; -fx-font-size: 20px; -fx-border-color: #6ee7b7; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 2.5; -fx-padding: 10 30; -fx-effect: dropshadow(gaussian, #34d399, 20, 0.5, 0, 0);");
        updateHighlight();
    }

    private void showAchievement(String title, int dustReward) {
        achievementLabel.setText(title);
        rewardLabel.setText("+ " + dustReward + " STAR DUST AWARDED");
        achievementBox.setVisible(true);

        if (currentPlayer != null) {
            ScoreService.addStarDust(currentPlayer, dustReward);
        }

        updateLifelineButtons();

        ScaleTransition badgePop = new ScaleTransition(
                Duration.millis(500),
                achievementBox
        );

        badgePop.setFromX(0);
        badgePop.setFromY(0);
        badgePop.setToX(1.0);
        badgePop.setToY(1.0);
        badgePop.setInterpolator(Interpolator.EASE_OUT);
        badgePop.play();
    }

    private void spawnParticles(Color color, int count) {
        double cx = quizCanvas.getWidth() / 2;
        double cy = quizCanvas.getHeight() / 2;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 10 + 5;
            particles.add(new Particle(cx, cy, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    private void finishQuiz() {
        resultTitle.setText("ARCHIVES COMPLETE");
        resultTitle.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 52px; -fx-text-fill: #38bdf8; -fx-effect: dropshadow(gaussian, #0ea5e9, 30, 0.6, 0, 0);");
        resultDesc.setText("FINAL WISDOM SCORE: " + score);
        achievementBox.setVisible(false);
        resultOverlay.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> backToHub());
        pause.play();
    }

    @FXML
    private void backToHub() {
        if (bgTimer != null) bgTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            GameHubController controller = loader.getController();
            controller.setPlayer(currentPlayer);
            Stage stage = (Stage) rootStackPane.getScene().getWindow();
            SceneManager.switchScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundAnimation() {
        bgTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                timeElapsed += 0.016;
                if (quizCanvas == null) return;
                GraphicsContext gc = quizCanvas.getGraphicsContext2D();
                double w = quizCanvas.getWidth();
                double h = quizCanvas.getHeight();
                if (w <= 0 || h <= 0) return;

                gc.clearRect(0, 0, w, h);
                gc.setFill(Color.web("#020504"));
                gc.fillRect(0, 0, w, h);

                // Fever Mode Grid Color
                Color gridColor = isFeverMode ? Color.rgb(245, 158, 11, 0.05) : Color.rgb(52, 211, 153, 0.03);
                gc.setStroke(gridColor);
                gc.setLineWidth(1);
                for(int x = 0; x < w; x += 40) gc.strokeLine(x, 0, x, h);
                for(int y = 0; y < h; y += 40) gc.strokeLine(0, y, w, y);

                double pulseSpeed = isFeverMode ? 120 : 80;
                double pulseRadius = (timeElapsed * pulseSpeed) % (w * 0.8);
                double pulseAlpha = Math.max(0, 1.0 - (pulseRadius / (w * 0.5)));
                Color ringColor = isFeverMode ? Color.rgb(234, 88, 12, pulseAlpha * 0.5) : Color.rgb(52, 211, 153, pulseAlpha * 0.4);
                gc.setStroke(ringColor);
                gc.setLineWidth(2);
                gc.strokeOval(w/2 - pulseRadius, h/2 - pulseRadius, pulseRadius * 2, pulseRadius * 2);

                double ambientPulse = Math.sin(timeElapsed * (isFeverMode ? 3.0 : 1.5)) * 0.05;
                RadialGradient nebula;
                if (isFeverMode) {
                    nebula = new RadialGradient(0, 0, w / 2, h / 2, w * 0.6, false, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.rgb(234, 88, 12, 0.2 + ambientPulse)),
                            new Stop(0.5, Color.rgb(251, 191, 36, 0.05)),
                            new Stop(1.0, Color.TRANSPARENT));
                } else {
                    nebula = new RadialGradient(0, 0, w / 2, h / 2, w * 0.6, false, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.rgb(16, 185, 129, 0.15 + ambientPulse)),
                            new Stop(0.5, Color.rgb(52, 211, 153, 0.05)),
                            new Stop(1.0, Color.TRANSPARENT));
                }
                gc.setFill(nebula);
                gc.fillOval(w/2 - w*0.6, h/2 - w*0.6, w*1.2, w*1.2);

                for (int i = particles.size() - 1; i >= 0; i--) {
                    Particle p = particles.get(i);
                    p.x += p.vx;
                    p.y += p.vy;
                    p.life -= (isFeverMode ? 0.05 : 0.03);
                    if (p.life <= 0) {
                        particles.remove(i);
                    } else {
                        gc.setFill(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), p.life));
                        gc.fillOval(p.x - 4, p.y - 4, 8, 8);
                    }
                }
            }
        };
        bgTimer.start();
    }
}