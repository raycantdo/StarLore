package com.starlore.starlore;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Random;
public class GameHubController {



        @FXML private Label rankLabel;
        @FXML private Label starDustLabel;
        @FXML private StackPane storyCard;
        @FXML private StackPane duelCard;
        @FXML private StackPane quizCard;
        @FXML private StackPane arcadeCard;
        @FXML private StackPane transitionOverlay;
        @FXML private Canvas transitionCanvas;
        @FXML private Label transitionLabel;

        private Player currentPlayer;
        private Random random = new Random();

        // ─── Initialization ───────────────────────────────────────

        @FXML
        public void initialize() {
            setupHoverEffects();
        }

        public void setPlayer(Player player) {
            this.currentPlayer = player;
            rankLabel.setText("⭐ " + player.getRankTitle());
            starDustLabel.setText("🌟 Star Dust: " + player.getTotalStarDust());
        }

        // ─── Hover Effects ────────────────────────────────────────

        private void setupHoverEffects() {
            setupCardHover(storyCard, "#4a0080", "#6a00b0");
            setupCardHover(duelCard, "#0a1a6e", "#1a3a9e");
            setupCardHover(quizCard, "#0a4a4a", "#0a7a7a");
            setupCardHover(arcadeCard, "#6e0a0a", "#9e1a1a");
        }

        private void setupCardHover(StackPane card, String normalColor, String hoverColor) {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), card);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), card);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            card.setOnMouseEntered(e -> {
                card.setStyle(card.getStyle().replace(normalColor, hoverColor));
                // Shift text slightly to the right
                card.getChildren().forEach(child ->
                        child.setTranslateX(8));
                scaleUp.play();
            });

            card.setOnMouseExited(e -> {
                card.setStyle(card.getStyle().replace(hoverColor, normalColor));
                card.getChildren().forEach(child ->
                        child.setTranslateX(0));
                scaleDown.play();
            });
        }

        // ─── Navigation ───────────────────────────────────────────

        @FXML
        private void openProfile() {
            System.out.println("Open Profile");
        }

        @FXML
        private void openLeaderboard() {
            System.out.println("Open Leaderboard");
        }

        @FXML
        private void signOut() {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
                root.setOpacity(0);
                Stage stage = (Stage) storyCard.getScene().getWindow();
                stage.setScene(new Scene(root));
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ─── Card Launch Transitions ──────────────────────────────

        @FXML
        private void launchStoryMode() {
            transitionOverlay.setVisible(true);
            //playBookTransition(() -> navigateTo("StoryMode.fxml"));
        }

        @FXML
        private void launchDuelMode() {
            transitionOverlay.setVisible(true);
            //playSparkTransition(() -> navigateTo("DuelMode.fxml"));
        }

        @FXML
        private void launchMythQuiz() {
            transitionOverlay.setVisible(true);
            //playExamTransition(() -> navigateTo("MythQuiz.fxml"));
        }

        @FXML
        private void launchArcade() {
            transitionOverlay.setVisible(true);
            //playArcadeTransition(() -> navigateTo("Arcade.fxml"));
        }

        // ─── Transition Animations ────────────────────────────────

        // 📖 Story Mode — Book pages flipping
        private void playBookTransition(Runnable onComplete) {
            GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, 1000, 700);

            // Dark overlay
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRect(0, 0, 1000, 700);

            double[] progress = {0};

            Timeline bookFlip = new Timeline();
            bookFlip.setCycleCount(25);

            KeyFrame frame = new KeyFrame(Duration.millis(80), e -> {
                progress[0] += 0.04;
                gc.clearRect(0, 0, 1000, 700);

                // Dark background
                gc.setFill(Color.rgb(5, 5, 20, 0.95));
                gc.fillRect(0, 0, 1000, 700);

                // Draw book pages flipping
                double centerX = 500;
                double centerY = 350;
                double pageWidth = 300 + (progress[0] * 400);
                double pageHeight = 220;

                // Left page
                gc.setFill(Color.rgb(60, 0, 100, 0.9));
                gc.fillRoundRect(centerX - pageWidth, centerY - pageHeight / 2,
                        pageWidth, pageHeight, 10, 10);

                // Right page
                gc.setFill(Color.rgb(80, 0, 130, 0.9));
                gc.fillRoundRect(centerX, centerY - pageHeight / 2,
                        pageWidth, pageHeight, 10, 10);

                // Page lines
                gc.setStroke(Color.rgb(180, 140, 255, 0.4));
                gc.setLineWidth(1);
                for (int i = 1; i < 6; i++) {
                    double lineY = centerY - pageHeight / 2 + (i * pageHeight / 6);
                    gc.strokeLine(centerX - pageWidth + 20, lineY,
                            centerX - 20, lineY);
                    gc.strokeLine(centerX + 20, lineY,
                            centerX + pageWidth - 20, lineY);
                }

                // Spine
                gc.setFill(Color.rgb(100, 0, 160));
                gc.fillRect(centerX - 8, centerY - pageHeight / 2, 16, pageHeight);
            });

            bookFlip.getKeyFrames().add(frame);
            bookFlip.setOnFinished(e -> {
                // Fill entire screen
                gc.setFill(Color.rgb(20, 0, 40));
                gc.fillRect(0, 0, 1000, 700);
                fadeOutAndNavigate(onComplete);
            });
            bookFlip.play();
        }

        // ⚔️ Duel Mode — Spark clash
        private void playSparkTransition(Runnable onComplete) {
            GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, 1000, 700);

            double[] frame = {0};

            Timeline spark = new Timeline();
            spark.setCycleCount(30);

            KeyFrame kf = new KeyFrame(Duration.millis(70), e -> {
                frame[0]++;
                gc.clearRect(0, 0, 1000, 700);

                // Dark overlay
                gc.setFill(Color.rgb(0, 0, 10, 0.92));
                gc.fillRect(0, 0, 1000, 700);

                double cx = 500, cy = 350;

                // Two stars clashing from left and right
                double offset = Math.max(0, 200 - frame[0] * 14);
                gc.setFill(Color.DODGERBLUE);
                gc.fillOval(cx - offset - 20, cy - 20, 40, 40);
                gc.setFill(Color.GOLD);
                gc.fillOval(cx + offset - 20, cy - 20, 40, 40);

                // Sparks after clash
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

                    // Flash
                    gc.setFill(Color.rgb(255, 255, 200,
                            Math.max(0, 0.6 - frame[0] * 0.03)));
                    gc.fillRect(0, 0, 1000, 700);
                }

                // Screen shake simulation
                if (frame[0] > 14 && frame[0] < 20) {
                    transitionCanvas.setTranslateX(random.nextInt(10) - 5);
                    transitionCanvas.setTranslateY(random.nextInt(10) - 5);
                } else {
                    transitionCanvas.setTranslateX(0);
                    transitionCanvas.setTranslateY(0);
                }
            });

            spark.getKeyFrames().add(kf);
            spark.setOnFinished(e -> {
                transitionCanvas.setTranslateX(0);
                transitionCanvas.setTranslateY(0);
                gc.setFill(Color.rgb(0, 0, 20));
                gc.fillRect(0, 0, 1000, 700);
                fadeOutAndNavigate(onComplete);
            });
            spark.play();
        }

        // ❓ Myth Quiz — Exam paper + red X stamp
        private void playExamTransition(Runnable onComplete) {
            GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, 1000, 700);

            double[] progress = {0};

            Timeline exam = new Timeline();
            exam.setCycleCount(35);

            KeyFrame kf = new KeyFrame(Duration.millis(70), e -> {
                progress[0]++;
                gc.clearRect(0, 0, 1000, 700);

                gc.setFill(Color.rgb(0, 10, 10, 0.92));
                gc.fillRect(0, 0, 1000, 700);

                double paperY = Math.max(150, 700 - progress[0] * 22);
                double paperX = 250, paperW = 500, paperH = 400;

                // Paper sliding in from top
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(paperX, paperY, paperW, paperH, 8, 8);

                // Paper lines
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(1);
                for (int i = 1; i < 10; i++) {
                    gc.strokeLine(paperX + 30, paperY + i * 36,
                            paperX + paperW - 30, paperY + i * 36);
                }

                // Title on paper
                gc.setFill(Color.rgb(10, 74, 74));
                gc.fillText("MYTH QUIZ", paperX + 180, paperY + 40);

                // Red X stamp after paper settles
                if (progress[0] > 20) {
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(12);
                    gc.setGlobalAlpha(Math.min(1, (progress[0] - 20) * 0.15));
                    gc.strokeLine(paperX + 60, paperY + 80,
                            paperX + paperW - 60, paperY + paperH - 60);
                    gc.strokeLine(paperX + paperW - 60, paperY + 80,
                            paperX + 60, paperY + paperH - 60);
                    gc.setGlobalAlpha(1);
                }

                // Paper crumple — shrink
                if (progress[0] > 28) {
                    double shrink = (progress[0] - 28) * 0.05;
                    gc.clearRect(paperX, paperY, paperW, paperH);
                    gc.setFill(Color.rgb(200, 200, 200));
                    gc.fillOval(paperX + paperW / 2 - (paperW / 2) * (1 - shrink),
                            paperY + paperH / 2 - (paperH / 2) * (1 - shrink),
                            paperW * (1 - shrink),
                            paperH * (1 - shrink));
                }
            });

            exam.getKeyFrames().add(kf);
            exam.setOnFinished(e -> {
                gc.setFill(Color.rgb(0, 20, 20));
                gc.fillRect(0, 0, 1000, 700);
                fadeOutAndNavigate(onComplete);
            });
            exam.play();
        }

        // 🌠 Arcade — Funny message + shooting star
        private void playArcadeTransition(Runnable onComplete) {
            GraphicsContext gc = transitionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, 1000, 700);

            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRect(0, 0, 1000, 700);

            // Show funny message
            transitionLabel.setText("Catch stars, not feelings ⭐");
            transitionLabel.setVisible(true);

            PauseTransition msgPause = new PauseTransition(Duration.seconds(1.5));
            msgPause.setOnFinished(e -> {
                transitionLabel.setVisible(false);

                // Shooting star animation
                double[] starX = {-50};
                double[] starY = {100 + random.nextInt(200)};

                Timeline starAnim = new Timeline();
                starAnim.setCycleCount(40);

                KeyFrame kf = new KeyFrame(Duration.millis(25), ev -> {
                    gc.clearRect(0, 0, 1000, 700);
                    gc.setFill(Color.rgb(0, 0, 0, 0.85));
                    gc.fillRect(0, 0, 1000, 700);

                    starX[0] += 28;
                    starY[0] += 4;

                    // Star trail
                    gc.setStroke(Color.rgb(255, 255, 200, 0.6));
                    gc.setLineWidth(3);
                    gc.strokeLine(starX[0] - 80, starY[0] - 12,
                            starX[0], starY[0]);

                    // Star head
                    gc.setFill(Color.WHITE);
                    gc.fillOval(starX[0] - 8, starY[0] - 8, 16, 16);

                    // Sparkles around star
                    gc.setFill(Color.YELLOW);
                    for (int i = 0; i < 4; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        gc.fillOval(starX[0] + Math.cos(angle) * 15,
                                starY[0] + Math.sin(angle) * 15, 5, 5);
                    }
                });

                starAnim.getKeyFrames().add(kf);
                starAnim.setOnFinished(ev -> {
                    gc.setFill(Color.rgb(20, 0, 0));
                    gc.fillRect(0, 0, 1000, 700);
                    fadeOutAndNavigate(onComplete);
                });
                starAnim.play();
            });
            msgPause.play();
        }

        // ─── Helper — Fade out overlay then navigate ──────────────

        private void fadeOutAndNavigate(Runnable onComplete) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), transitionOverlay);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                transitionOverlay.setVisible(false);
                transitionOverlay.setOpacity(1);
                onComplete.run();
            });
            fade.play();
        }

        private void navigateTo(String fxmlFile) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
                Parent root = loader.load();
                root.setOpacity(0);
                Stage stage = (Stage) storyCard.getScene().getWindow();
                stage.setScene(new Scene(root));
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

}
