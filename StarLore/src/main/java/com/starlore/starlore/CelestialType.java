package com.starlore.starlore;

import javafx.scene.paint.Color;

/**
 * Every kind of object that can appear in the Shooting Star Catcher mini-game.
 * Each type carries its point value, its effect on the player's lives, how big
 * and fast it is, and the colors used to draw it (no image assets needed —
 * everything is rendered procedurally so it stays crisp at any resolution).
 */
public enum CelestialType {

    NORMAL_STAR("Normal Star", "+10",  10,  0, 13, 1.00, Color.web("#ffe066"), Color.web("#fff6cc")),
    GOLDEN_STAR("Golden Star", "+25",  25,  0, 15, 1.05, Color.web("#ffb300"), Color.web("#ffe08a")),
    COSMIC_STAR("Cosmic Star", "+50",  50,  0, 17, 1.15, Color.web("#c77dff"), Color.web("#f0dcff")),
    STARDUST("Stardust",       "+5",    5,  0,  7, 0.85, Color.web("#8ecbff"), Color.web("#e2f4ff")),
    PLANET("Planet",           "+15",  15,  0, 19, 0.75, Color.web("#e0a458"), Color.web("#f6dcb0")),
    COMET("Comet",             "+30",  30,  0, 12, 1.85, Color.web("#4fd8ff"), Color.web("#d3faff")),
    METEOR("Meteor",           "-1 life", 0, -1, 16, 1.60, Color.web("#ff5c33"), Color.web("#ffb199")),
    BLACK_HOLE("Black Hole",   "-2 lives", 0, -2, 21, 0.85, Color.web("#8b5cf6"), Color.web("#2a1746")),
    DARK_MATTER("Dark Matter", "-10 pts", -10, 0, 14, 1.00, Color.web("#7c3aed"), Color.web("#2d1352"));

    public final String label;
    public final String effectLabel;
    public final int points;
    public final int livesDelta;
    public final double baseRadius;
    public final double speedFactor;
    public final Color primaryColor;
    public final Color glowColor;

    CelestialType(String label, String effectLabel, int points, int livesDelta, double baseRadius,
                  double speedFactor, Color primaryColor, Color glowColor) {
        this.label = label;
        this.effectLabel = effectLabel;
        this.points = points;
        this.livesDelta = livesDelta;
        this.baseRadius = baseRadius;
        this.speedFactor = speedFactor;
        this.primaryColor = primaryColor;
        this.glowColor = glowColor;
    }

    /** True for anything the player should avoid clicking. */
    public boolean isHazard() {
        return livesDelta < 0 || points < 0;
    }
}
