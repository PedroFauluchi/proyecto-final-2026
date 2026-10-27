package com.passlikeroman.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.passlikeroman.data.PlayerStats;

public class Player {
    private Vector2 position;
    private Texture texture;
    private PlayerStats stats;
    private float radius;

    public Player(float x, float y, Texture texture, PlayerStats stats){

        this.position = new Vector2(x, y);
        this.texture = texture;
        this.stats = stats;
        this.radius = 32f;
    }

    public void render(SpriteBatch batch){
        batch.begin();

        batch.draw(
            texture,
            position.x - radius,
            position.y - radius,
            radius * 2,
            radius * 2
        );
        
        batch.end();
    }
}

