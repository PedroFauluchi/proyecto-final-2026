package com.passlikeroman.screens;

public class Platform {

    public float x;
    public float y;
    public final float width;
    public final float height;
    public boolean active = true;

    public Platform(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getPlayerX() {
        return x + width / 2f;
    }

    public float getPlayerY() {
        return y + height + 18f;
    }

    public void move(float dx) {
        x += dx;
    }

    public boolean isOffScreen() {
        return x + width < -80;
    }
}
