package com.starlore.starlore;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * SceneManager provides an adaptive full-screen display system.
 * It extends screen backgrounds to fill the full monitor width (eliminating side gaps
 * and color tone discrepancies), while keeping all UI designs, elements, and gameplay
 * coordinates perfectly centered, unwarped, and proportional.
 */
public class SceneManager {

    private static Stage primaryStage;
    private static Scene primaryScene;
    private static StackPane rootContainer;
    private static Group contentGroup;
    private static Parent currentContent;

    private static double baseWidth = 950.0;
    private static double baseHeight = 700.0;
    private static double currentEffectiveWidth = 950.0;

    /**
     * Initializes the stage with full-size / maximized settings,
     * builds the adaptive root container, and displays the initial content.
     */
    public static void init(Stage stage, Parent initialContent) {
        primaryStage = stage;

        rootContainer = new StackPane();
        rootContainer.setStyle(extractBackgroundStyle(initialContent));
        rootContainer.setAlignment(Pos.CENTER);

        contentGroup = new Group();
        rootContainer.getChildren().add(contentGroup);

        updateContent(initialContent);

        primaryScene = new Scene(rootContainer, baseWidth, baseHeight);

        // Listen for container / scene dimension changes to adjust scale & width in real-time
        rootContainer.widthProperty().addListener((obs, oldVal, newVal) -> updateScale());
        rootContainer.heightProperty().addListener((obs, oldVal, newVal) -> updateScale());
        primaryScene.widthProperty().addListener((obs, oldVal, newVal) -> updateScale());
        primaryScene.heightProperty().addListener((obs, oldVal, newVal) -> updateScale());

        // F11 shortcut to toggle borderless full screen mode
        primaryScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                if (primaryStage != null) {
                    primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    event.consume();
                }
            }
        });

        primaryStage.setTitle("StarLore");
        primaryStage.setResizable(true);
        primaryStage.setScene(primaryScene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setMaximized(true);
        primaryStage.show();

        Platform.runLater(SceneManager::updateScale);
    }

    /**
     * Switches the active screen content inside the persistent container.
     */
    public static void switchScene(Stage stage, Parent newContent) {
        if (stage != null) {
            primaryStage = stage;
        }
        if (primaryStage == null && primaryScene != null && primaryScene.getWindow() instanceof Stage s) {
            primaryStage = s;
        }

        if (rootContainer == null || contentGroup == null || primaryScene == null) {
            if (primaryStage != null) {
                init(primaryStage, newContent);
            }
            return;
        }

        // Match container background to the incoming scene
        rootContainer.setStyle(extractBackgroundStyle(newContent));

        updateContent(newContent);

        if (primaryStage != null && primaryStage.getScene() != primaryScene) {
            primaryStage.setScene(primaryScene);
        }

        updateScale();
        Platform.runLater(SceneManager::updateScale);
    }

    /**
     * Switches the active screen content using the cached primary stage.
     */
    public static void switchScene(Parent newContent) {
        switchScene(primaryStage, newContent);
    }

    private static void updateContent(Parent newContent) {
        currentContent = newContent;

        double targetWidth = 950.0;
        double targetHeight = 700.0;

        if (newContent instanceof Region region) {
            if (region.getPrefWidth() > 0) {
                targetWidth = region.getPrefWidth();
            }
            if (region.getPrefHeight() > 0) {
                targetHeight = region.getPrefHeight();
            }
        }

        baseWidth = targetWidth;
        baseHeight = targetHeight;

        contentGroup.getChildren().setAll(newContent);
    }

    /**
     * Updates the scale and expands the effective width of the active screen
     * so that backgrounds completely span the display horizontally without side gaps,
     * while maintaining 1:1 element aspect ratio.
     */
    public static void updateScale() {
        if (rootContainer == null || contentGroup == null || currentContent == null) {
            return;
        }

        double containerWidth = rootContainer.getWidth();
        double containerHeight = rootContainer.getHeight();

        if (containerWidth <= 0 || containerHeight <= 0) {
            if (primaryScene != null) {
                containerWidth = primaryScene.getWidth();
                containerHeight = primaryScene.getHeight();
            }
        }

        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        double scale = calculateScale(containerWidth, containerHeight, baseWidth, baseHeight);
        double effectiveWidth = calculateEffectiveWidth(containerWidth, containerHeight, baseWidth, baseHeight);
        currentEffectiveWidth = effectiveWidth;

        if (currentContent instanceof Region region) {
            region.setMinWidth(effectiveWidth);
            region.setMaxWidth(effectiveWidth);
            region.setPrefWidth(effectiveWidth);
            region.setMinHeight(baseHeight);
            region.setMaxHeight(baseHeight);
            region.setPrefHeight(baseHeight);
        }

        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);
    }

    /**
     * Calculates the scale factor based on screen height to keep element ratio 1:1.
     */
    public static double calculateScale(double containerWidth, double containerHeight,
                                        double designWidth, double designHeight) {
        if (containerWidth <= 0 || containerHeight <= 0 || designWidth <= 0 || designHeight <= 0) {
            return 1.0;
        }
        double scaleY = containerHeight / designHeight;
        double scaleX = containerWidth / designWidth;
        // If container is narrower than design aspect ratio, fit by width:
        return Math.min(scaleX, scaleY);
    }

    /**
     * Calculates the wider width required to span the container horizontally with zero side gaps.
     */
    public static double calculateEffectiveWidth(double containerWidth, double containerHeight,
                                                 double designWidth, double designHeight) {
        if (containerWidth <= 0 || containerHeight <= 0 || designWidth <= 0 || designHeight <= 0) {
            return designWidth;
        }
        double scale = calculateScale(containerWidth, containerHeight, designWidth, designHeight);
        if (scale <= 0) return designWidth;
        return Math.max(designWidth, containerWidth / scale);
    }

    /**
     * Extracts -fx-background-color from a Node's style to sync container tone.
     */
    public static String extractBackgroundStyle(Parent content) {
        if (content != null && content.getStyle() != null) {
            for (String part : content.getStyle().split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("-fx-background-color:")) {
                    return trimmed + ";";
                }
            }
        }
        return "-fx-background-color: #020308;";
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static Scene getPrimaryScene() {
        return primaryScene;
    }

    public static double getBaseWidth() {
        return baseWidth;
    }

    public static double getBaseHeight() {
        return baseHeight;
    }

    public static double getCurrentEffectiveWidth() {
        return currentEffectiveWidth;
    }
}
