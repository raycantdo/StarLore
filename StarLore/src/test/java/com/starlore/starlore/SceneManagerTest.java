package com.starlore.starlore;

import javafx.scene.layout.AnchorPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SceneManagerTest {

    @Test
    public void testScalePreservesAspectAtExactBaseSize() {
        double scale = SceneManager.calculateScale(950, 700, 950, 700);
        assertEquals(1.0, scale, 1e-6);
        double effW = SceneManager.calculateEffectiveWidth(950, 700, 950, 700);
        assertEquals(950.0, effW, 1e-6);
    }

    @Test
    public void testScale1080pStandard16by9() {
        // 1920x1080 with 950x700 base:
        double scale = SceneManager.calculateScale(1920, 1080, 950, 700);
        assertEquals(1080.0 / 700.0, scale, 1e-6);

        // Effective width spans full 1920 when scaled
        double effW = SceneManager.calculateEffectiveWidth(1920, 1080, 950, 700);
        double scaledWidth = effW * scale;
        assertEquals(1920.0, scaledWidth, 1e-4); // Matches width exactly with NO side gaps!
    }

    @Test
    public void testScaleUltrawide21by9() {
        // 2560x1080 with 950x700 base:
        double scale = SceneManager.calculateScale(2560, 1080, 950, 700);
        assertEquals(1080.0 / 700.0, scale, 1e-6);

        double effW = SceneManager.calculateEffectiveWidth(2560, 1080, 950, 700);
        double scaledWidth = effW * scale;
        assertEquals(2560.0, scaledWidth, 1e-4); // Matches ultrawide width with NO side gaps!
    }

    @Test
    public void testScale4by3Monitor() {
        double scale = SceneManager.calculateScale(1024, 768, 950, 700);
        assertEquals(1024.0 / 950.0, scale, 1e-6);

        double effW = SceneManager.calculateEffectiveWidth(1024, 768, 950, 700);
        assertEquals(950.0, effW, 1e-6);
    }

    @Test
    public void testBackgroundStyleExtraction() {
        AnchorPane pane = new AnchorPane();
        pane.setStyle("-fx-background-color: #0d1033;");
        String bg = SceneManager.extractBackgroundStyle(pane);
        assertEquals("-fx-background-color: #0d1033;", bg.trim());

        pane.setStyle("-fx-background-color: black; -fx-padding: 10;");
        bg = SceneManager.extractBackgroundStyle(pane);
        assertEquals("-fx-background-color: black;", bg.trim());
    }

    @Test
    public void testScaleZeroOrNegative() {
        assertEquals(1.0, SceneManager.calculateScale(0, 0, 950, 700));
        assertEquals(1.0, SceneManager.calculateScale(-100, 700, 950, 700));
    }
}
