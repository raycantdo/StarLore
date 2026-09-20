package com.starlore.starlore;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LeaderboardController {

    @FXML private StackPane rootStackPane;
    @FXML private Canvas bgCanvas;
    @FXML private VBox leaderboardList; // Replaced ListView with VBox for heavy UI

    private final PlayerDAO playerDAO = new PlayerDAO();

    private AnimationTimer bgTimer;
    private double timeElapsed = 0;
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();

    private static class Particle {
        double x, y, vy, life;
        Particle(double x, double y, double vy) {
            this.x = x; this.y = y; this.vy = vy; this.life = 1.0;
        }
    }

    @FXML
    public void initialize() {
        if (rootStackPane != null) {
            bgCanvas.widthProperty().bind(rootStackPane.widthProperty());
            bgCanvas.heightProperty().bind(rootStackPane.heightProperty());
        }

        startBackgroundAnimation();
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        leaderboardList.getChildren().clear();

        // Using your exact PlayerDAO logic
        List<String[]> leaderboard = playerDAO.getLeaderboard();

        int position = 1;
        for (String[] player : leaderboard) {
            String username = player[0];
            int score = 0;

            try {
                // Remove commas/spaces just in case they were formatted as string previously
                score = Integer.parseInt(player[1].replaceAll("[^\\d]", ""));
            } catch (NumberFormatException e) {
                System.err.println("Score format error for player: " + username);
            }

            // Generate titles dynamically based on rank for the visual flex
            String title = (position == 1) ? "MASTER OF THE COSMOS" :
                    (position <= 3) ? "NEBULA SCHOLAR" : "ASTRAL VOYAGER";

            // Generate the heavy stylized row
            StackPane row = createStyledRow(position, username, title, score);

            // Cascading entry animation
            row.setOpacity(0);
            row.setTranslateY(30);
            leaderboardList.getChildren().add(row);

            FadeTransition ft = new FadeTransition(Duration.millis(500), row);
            ft.setToValue(1.0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), row);
            tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(150 + (position * 80)));
            pt.play();

            position++;
        }
    }

    private StackPane createStyledRow(int rank, String username, String title, int score) {
        StackPane container = new StackPane();
        container.setMinWidth(800); container.setMaxWidth(800); container.setPrefWidth(800);

        String bgColor = (rank == 1) ? "rgba(30, 25, 5, 0.85)" : (rank == 2) ? "rgba(5, 20, 30, 0.85)" : (rank == 3) ? "rgba(5, 25, 15, 0.85)" : "rgba(10, 15, 25, 0.7)";
        String borderColor = (rank == 1) ? "#facc15" : (rank == 2) ? "#22d3ee" : (rank == 3) ? "#34d399" : "rgba(56, 189, 248, 0.2)";
        double borderWidth = (rank <= 3) ? (3.0 - (rank * 0.5)) : 1.0;

        container.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: " + borderWidth + "; -fx-padding: 15 30;");

        HBox content = new HBox();
        content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label rankLbl = new Label(String.format("#%02d", rank));
        rankLbl.setMinWidth(80); rankLbl.setPrefWidth(80); rankLbl.setAlignment(javafx.geometry.Pos.CENTER);
        rankLbl.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 24px; -fx-text-fill: " + (rank <= 3 ? borderColor : "#94a3b8") + ";");

        Region spacer1 = new Region(); spacer1.setMinWidth(30); spacer1.setPrefWidth(30);

        VBox nameBox = new VBox(2);
        nameBox.setMinWidth(300); nameBox.setPrefWidth(300); nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLbl = new Label(username.toUpperCase());
        nameLbl.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-letter-spacing: 2;");
        nameBox.getChildren().addAll(nameLbl, titleLbl);

        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label scoreLbl = new Label(String.format("%,d", score));
        scoreLbl.setMinWidth(150); scoreLbl.setPrefWidth(150); scoreLbl.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        scoreLbl.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 28px; -fx-text-fill: #ffffff;");

        content.getChildren().addAll(rankLbl, spacer1, nameBox, spacer2, scoreLbl);
        container.getChildren().add(content);

        // Hover physics
        container.setOnMouseEntered(e -> container.setStyle(container.getStyle().replace("-fx-background-color: " + bgColor, "-fx-background-color: rgba(30, 40, 60, 0.95)")));
        container.setOnMouseExited(e -> container.setStyle(container.getStyle().replace("-fx-background-color: rgba(30, 40, 60, 0.95)", "-fx-background-color: " + bgColor)));

        return container;
    }

    @FXML
    private void goBack() {
        if (bgTimer != null) bgTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameHubView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) leaderboardList.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("StarLore");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundAnimation() {
        bgTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                timeElapsed += 0.016;
                if (bgCanvas == null) return;
                GraphicsContext gc = bgCanvas.getGraphicsContext2D();
                double w = bgCanvas.getWidth();
                double h = bgCanvas.getHeight();
                if (w <= 0 || h <= 0) return;

                gc.clearRect(0, 0, w, h);
                gc.setFill(Color.web("#020504"));
                gc.fillRect(0, 0, w, h);

                gc.setStroke(Color.rgb(56, 189, 248, 0.03));
                gc.setLineWidth(1);
                for(int x = 0; x < w; x += 50) gc.strokeLine(x, 0, x, h);
                for(int y = 0; y < h; y += 50) gc.strokeLine(0, y, w, y);

                RadialGradient nebula = new RadialGradient(
                        0, 0, w / 2, h / 2, w * 0.7, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.rgb(14, 165, 233, 0.1)),
                        new Stop(0.5, Color.rgb(30, 58, 138, 0.05)),
                        new Stop(1.0, Color.TRANSPARENT)
                );
                gc.setFill(nebula);
                gc.fillOval(w/2 - w*0.7, h/2 - h*0.7, w*1.4, h*1.4);

                if (random.nextDouble() < 0.1) {
                    particles.add(new Particle(random.nextDouble() * w, h + 10, -random.nextDouble() * 1.5 - 0.5));
                }

                for (int i = particles.size() - 1; i >= 0; i--) {
                    Particle p = particles.get(i);
                    p.y += p.vy;
                    p.life -= 0.005;
                    if (p.life <= 0 || p.y < 0) {
                        particles.remove(i);
                    } else {
                        gc.setFill(Color.color(0.22, 0.74, 0.97, p.life * 0.5));
                        gc.fillOval(p.x, p.y, 3, 3);
                    }
                }
            }
        };
        bgTimer.start();
    }
}