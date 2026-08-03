package com.starlore.starlore;

public class ShootingStar {
    private double x, y, speed;
    private String constellationName; // Adds educational value for StarLore!

    public ShootingStar(double x, double y, double speed, String constellationName) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.constellationName = constellationName;
    }

    public void update() {
        this.y += speed;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getConstellationName() { return constellationName; }
}