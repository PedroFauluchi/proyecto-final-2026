package com.passlikeroman.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.passlikeroman.PassLikeRoman;

public class MenuScreen extends ScreenAdapter {

    private final PassLikeRoman game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;

    public MenuScreen(PassLikeRoman game) {
        this.game = game;
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float x = screenX * 900f / Gdx.graphics.getWidth();
                float y = 520f - screenY * 520f / Gdx.graphics.getHeight();

                // Único botón: Comenzar partido.
                if (x > 255 && x < 645 && y > 260 && y < 330) {
                    game.setScreen(new GameScreen(game));
                }
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.035f, 0.055f, 0.09f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, 900, 520);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawSimpleBackground();
        drawSideFigures();
        drawMenuPanel();
    }

    // ============================================================
    // FONDO SIMPLE
    //
    // Solamente cielo + una franja de cancha abajo.
    // Nada de tribunas, arcos, ni escudos.
    // ============================================================

    private void drawSimpleBackground() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Cielo, un degradé simple hecho con dos rectángulos.
        shapes.setColor(0.045f, 0.10f, 0.19f, 1);
        shapes.rect(0, 0, 900, 520);

        shapes.setColor(0.03f, 0.07f, 0.14f, 1);
        shapes.rect(0, 0, 900, 90);

        // Franja de cancha, solo como referencia visual abajo.
        shapes.setColor(0.06f, 0.30f, 0.13f, 1);
        shapes.rect(0, 0, 900, 55);

        shapes.setColor(0.9f, 0.95f, 0.9f, 0.25f);
        shapes.rect(0, 52, 900, 3);

        shapes.end();
    }

    // ============================================================
    // JUGADORES A LOS COSTADOS
    //
    // Siluetas simples, sin nombres ni escudos, apoyados
    // como decoración del menú.
    // ============================================================

    private void drawSideFigures() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawFigure(95, 60, 0.05f, 0.20f, 0.55f);
        drawFigure(805, 60, 0.55f, 0.10f, 0.12f);

        shapes.end();
    }

    private void drawFigure(float x, float baseY, float r, float g, float b) {

        // Cabeza
        shapes.setColor(0.82f, 0.61f, 0.45f, 1);
        shapes.circle(x, baseY + 92, 12);

        // Camiseta
        shapes.setColor(r, g, b, 1);
        shapes.rect(x - 19, baseY + 50, 38, 38);

        // Piernas
        shapes.setColor(0.9f, 0.9f, 0.9f, 1);
        shapes.rect(x - 14, baseY + 20, 11, 30);
        shapes.rect(x + 3, baseY + 20, 11, 30);

        // Botines
        shapes.setColor(0.04f, 0.04f, 0.04f, 1);
        shapes.rect(x - 16, baseY + 14, 15, 7);
        shapes.rect(x + 3, baseY + 14, 15, 7);
    }

    // ============================================================
    // PANEL DEL MENÚ
    //
    // Solamente título + botón de comenzar partido.
    // ============================================================

    private void drawMenuPanel() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Botón único.
        shapes.setColor(0.07f, 0.20f, 0.33f, 0.98f);
        shapes.rect(255, 260, 390, 70);

        shapes.end();

        batch.begin();

        font.getData().setScale(2.3f);
        font.setColor(1, 0.82f, 0.25f, 1);
        font.draw(batch, "PASS LIKE ROMÁN", 315, 400);

        font.getData().setScale(1.6f);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "COMENZAR PARTIDO", 335, 303);

        font.getData().setScale(0.9f);
        font.setColor(0.82f, 0.88f, 0.95f, 1);
        font.draw(batch, "Entrar al juego", 390, 275);

        batch.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
