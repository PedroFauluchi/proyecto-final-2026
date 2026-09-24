package com.passlikeroman.screens;

public class Obstacle {

    public enum Type {
        GOALKEEPER,
        INTERCEPTOR,
        CONE_WALL,
        TIRES,
        WIND_GUST,
        CARDBOARD
    }

    public final Type type;
    public float x;
    public float y;
    public float width;
    public float height;
    public float speed;
    public float phase;
    public boolean active = true;

    public Obstacle(Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;

        switch (type) {
            case GOALKEEPER:
                width = 32;
                height = 60;
                break;
            case INTERCEPTOR:
                width = 30;
                height = 60;
                speed = 95;
                break;
            case CONE_WALL:
                width = 70;
                height = 28;
                break;
            case TIRES:
                width = 62;
                height = 42;
                break;
            case WIND_GUST:
                width = 50;
                height = 90;
                break;
            default:
                width = 38;
                height = 55;
                break;
        }
    }

    public int reward() {
        switch (type) {
            case GOALKEEPER: return 60;
            case INTERCEPTOR: return 90;
            case CONE_WALL: return 15;
            case TIRES: return 45;
            case WIND_GUST: return 75;
            case CARDBOARD: return 120;
            default: return 15;
        }
    }

    public void update(float delta) {
        if (type == Type.INTERCEPTOR) {
            phase += delta * 2.2f;
            y += (float) Math.sin(phase) * speed * delta;
            y = Math.max(150, Math.min(360, y));
        }
    }
}
