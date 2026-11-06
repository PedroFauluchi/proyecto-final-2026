package com.passlikeroman.screens;

public class Obstacle {

    public enum Type {
        MANNEQUIN,
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
            case MANNEQUIN:
                width = 34f;
                height = 78f;
                break;
            case GOALKEEPER:
                width = 40f;
                height = 90f;
                break;
            case INTERCEPTOR:
                width = 34f;
                height = 78f;
                speed = 95;
                break;
            case CONE_WALL:
                width = 74f;
                height = 44f;
                break;
            case TIRES:
                width = 74f;
                height = 31f;
                break;
            case WIND_GUST:
                width = 52f;
                height = 48f;
                break;
            default:
                width = 56f;
                height = 52f;
                break;
        }
    }

    public int reward() {
        switch (type) {
            case MANNEQUIN: return 60;
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
        if (type == Type.GOALKEEPER) {
            float jumpPhase = phase % 2.2f;
            float jumpTime = jumpPhase - 0.8f;
            float jumpOffset = 0f;
            if (jumpTime >= 0f && jumpTime <= 0.8f) {
                float progress = jumpTime / 0.8f;
                jumpOffset = 4f * 42f * progress * (1f - progress);
            }
            y = originY + jumpOffset;
        } else if (type == Type.INTERCEPTOR) {
            y = Math.max(150, Math.min(360, originY + (float) Math.sin(phase) * Math.min(46f, speed * 0.38f)));
        }
    }

    public float getPlatformY() {
        // The goalkeeper jumps above a fixed platform; vertically moving
        // obstacles carry their platform with them.
        return type == Type.GOALKEEPER ? originY : y;
    }

    public boolean isGoalkeeperJumping() {
        float jumpPhase = phase % 2.2f;
        return jumpPhase >= 0.8f && jumpPhase <= 1.6f;
    }
}
