package com.passlikeroman.screens;

import com.badlogic.gdx.math.Vector2;

public class Ball {

    public final Vector2 position = new Vector2();
    public final Vector2 velocity = new Vector2();

    public boolean flying = false;

    private static final float GRAVITY = -520f;

    public void reset(float x, float y) {
        position.set(x, y);
        velocity.set(0, 0);
        flying = false;
    }

    public void launch(Vector2 initialVelocity) {
        velocity.set(initialVelocity);
        flying = true;
    }

    public void update(float delta, float windForce, float curveForce) {
        if (!flying) {
            return;
        }

        position.x += velocity.x * delta;
        position.y += velocity.y * delta;

        velocity.y += GRAVITY * delta;
        velocity.x += (windForce + curveForce) * delta;
    }
}
