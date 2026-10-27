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
    private final float originY;
    public boolean active = true;

    public Obstacle(Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.originY = y;

        switch (type) {
            case GOALKEEPER:
                width = 27.2f;
                height = 51;
                break;
            case INTERCEPTOR:
                width = 25.5f;
                height = 51;
                speed = 95;
                break;
            case CONE_WALL:
                width = 59.5f;
                height = 23.8f;
                break;
            case TIRES:
                width = 52.7f;
                height = 35.7f;
                break;
            case WIND_GUST:
                width = 42.5f;
                height = 76.5f;
                break;
            default:
                width = 32.3f;
                height = 46.75f;
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
        phase += delta * (1.5f + speed * 0.01f);
        if (type == Type.INTERCEPTOR) {
            y = Math.max(150, Math.min(360, originY + (float) Math.sin(phase) * Math.min(46f, speed * 0.38f)));
        }
    }
}
