package com.passlikeroman.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.passlikeroman.data.GameProfile;
import com.passlikeroman.PassLikeRoman;

public class MenuScreen extends ScreenAdapter {

    private final PassLikeRoman game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Texture menuBackground;
    private final Texture[] stadiumPreviews = new Texture[GameProfile.STADIUMS.length];
    private static final String[] KIT_PREVIEW_FILES = {
        "players/boca.png", "players/river.png", "players/racing.png",
        "players/independiente.png", "players/san-lorenzo.png",
        "players/rosario-central.png", "players/estudiantes.png", "players/newells.png",
        "players/argentinos-juniors.png", "players/velez.png", "players/huracan.png",
        "players/gimnasia.png", "players/neutro.png"
    };
    private final Texture[] kitPreviews = new Texture[KIT_PREVIEW_FILES.length];
    private final Music menuMusic;
    private float transition = 0.35f;
    private boolean startingGame = false;
    private int section = 0;
    private String notice = "Elegí un artículo";

    public MenuScreen(PassLikeRoman game) {
        this.game = game;
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        menuBackground = new Texture(Gdx.files.internal("menu-background.png"));
        String[] stadiumFiles = {
            "stadiums/bombonera.png",
            "stadiums/monumental.png",
            "stadiums/cilindro.png",
            "stadiums/libertadores.png",
            "stadiums/nuevo-gasometro.png"
        };
        for (int i = 0; i < stadiumFiles.length; i++) {
            stadiumPreviews[i] = new Texture(Gdx.files.internal(stadiumFiles[i]));
        }
        for (int i = 0; i < KIT_PREVIEW_FILES.length; i++) {
            kitPreviews[i] = new Texture(Gdx.files.internal(KIT_PREVIEW_FILES[i]));
        }
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("menu-theme.wav"));
        menuMusic.setLooping(true);
        menuMusic.setVolume(0.22f);
        menuMusic.play();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float x = screenX * 900f / Gdx.graphics.getWidth();
                float y = 520f - screenY * 520f / Gdx.graphics.getHeight();

                if (startingGame) return true;
                if (section == 0) {
                    if (x > 330 && x < 570 && y > 365 && y < 415) startingGame = true;
                    else if (x > 375 && x < 525 && y > 305 && y < 350) section = 1;
                    else if (x > 375 && x < 525 && y > 250 && y < 295) section = 2;
                    else if (x < 95 && y < 65) Gdx.app.exit();
                } else if (section == 1 && x > 205 && x < 695 && y > 88 && y < 440) {
                    int column = x < 450 ? 0 : 1;
                    int row = Math.min(6, Math.max(0, Math.round((400f - y) / 45f)));
                    int index = row * 2 + column;
                    boolean changed = GameProfile.buyOrSelectKit(index);
                    notice = changed ? "Seleccionado" : "No alcanza el saldo";
                } else if (section == 2 && x > 205 && x < 695 && y > 105 && y < 398) {
                    int column = x < 450 ? 0 : 1;
                    int row = Math.min(2, Math.max(0, Math.round((350f - y) / 105f)));
                    int index = row * 2 + column;
                    boolean changed = GameProfile.buyOrSelectStadium(index);
                    notice = changed ? "Seleccionado" : "No alcanza el saldo";
                } else if (y < 145) {
                    section = 0;
                }
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        if (startingGame) {
            transition = Math.min(1f, transition + delta * 3.5f);
            if (transition >= 1f) {
                game.setScreen(new GameScreen(game));
                return;
            }
        } else {
            transition = Math.max(0f, transition - delta * 3.5f);
        }
        Gdx.gl.glClearColor(0.035f, 0.055f, 0.09f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, 900, 520);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawSimpleBackground();
        if (section == 0) drawMenuPanel(); else drawCollectionPanel();
        if (transition > 0f) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.01f, 0.02f, 0.04f, transition);
            shapes.rect(0, 0, 900, 520);
            shapes.end();
        }
    }

    // ============================================================
    // FONDO SIMPLE
    //
    // Solamente cielo + una franja de cancha abajo.
    // Nada de tribunas, arcos, ni escudos.
    // ============================================================

    private void drawSimpleBackground() {
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(menuBackground, 0, 0, 900, 520);
        batch.end();
    }

    private void drawMenuPanel() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawPixelButton(330, 365, 240, 50);
        drawPixelButton(375, 305, 150, 45);
        drawPixelButton(375, 250, 150, 45);
        drawPixelButton(18, 15, 77, 32);
        drawPixelButton(18, 474, 150, 30);
        shapes.end();

        batch.begin();
        font.getData().setScale(0.82f);
        drawButtonLabel("COMENZAR PARTIDO", 365, 395);
        font.getData().setScale(0.72f);
        drawButtonLabel("CAMISETAS", 401, 332);
        drawButtonLabel("ESTADIOS", 408, 277);
        font.getData().setScale(0.62f);
        drawButtonLabel("Salir", 39, 36);
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, "Coins: " + GameProfile.coins(), 28, 494);
        batch.end();
    }

    /** Layered pixel-art style frame inspired by the supplied blue metal button. */
    private void drawPixelButton(float x, float y, float width, float height) {
        shapes.setColor(0.035f, 0.045f, 0.075f, 0.98f);
        shapes.rect(x, y, width, height);
        shapes.setColor(0.63f, 0.69f, 0.76f, 1f);
        shapes.rect(x + 2f, y + 2f, width - 4f, height - 4f);
        shapes.setColor(0.20f, 0.25f, 0.34f, 1f);
        shapes.rect(x + 5f, y + 5f, width - 10f, height - 10f);
        shapes.setColor(0.075f, 0.19f, 0.39f, 1f);
        shapes.rect(x + 8f, y + 8f, width - 16f, height - 16f);
        shapes.setColor(0.12f, 0.29f, 0.55f, 1f);
        shapes.rect(x + 11f, y + 11f, width - 22f, height - 22f);
        shapes.setColor(0.25f, 0.43f, 0.69f, 0.9f);
        shapes.rect(x + 13f, y + height - 15f, width - 26f, 2f);
        shapes.setColor(0.035f, 0.10f, 0.23f, 1f);
        shapes.rect(x + 13f, y + 12f, width - 26f, 2f);
        // Four square rivets echo the corners of the reference frame.
        shapes.setColor(0.82f, 0.85f, 0.88f, 1f);
        shapes.rect(x + 4f, y + 4f, 3f, 3f);
        shapes.rect(x + width - 7f, y + 4f, 3f, 3f);
        shapes.rect(x + 4f, y + height - 7f, 3f, 3f);
        shapes.rect(x + width - 7f, y + height - 7f, 3f, 3f);
    }

    private void drawButtonLabel(String text, float x, float y) {
        font.setColor(0.015f, 0.035f, 0.09f, 1f);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(0.94f, 0.96f, 1f, 1f);
        font.draw(batch, text, x, y);
    }

    private void drawCollectionPanel() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.02f, 0.94f);
        shapes.rect(175, 68, 550, 390);
        shapes.setColor(1f, 1f, 1f, 0.96f);
        shapes.rect(178, 71, 544, 384);
        drawPixelButton(25, 15, 88, 32);
        if (section == 1) {
            for (int i = 0; i < GameProfile.CLUBS.length; i++) {
                float x = 195 + (i % 2) * 255f;
                float y = 400 - (i / 2) * 45f;
                drawStoreCard(x, y - 12, 245, 44);
            }
        } else {
            for (int i = 0; i < GameProfile.STADIUMS.length; i++) {
                float x = 195 + (i % 2) * 255f;
                float y = 350 - (i / 2) * 105f;
                drawStoreCard(x, y - 38, 245, 92);
            }
        }
        shapes.end();

        batch.begin();
        font.setColor(0.03f, 0.03f, 0.03f, 1f);
        font.getData().setScale(1.05f);
        font.draw(batch, section == 1 ? "CAMISETAS" : "ESTADIOS", 370, 435);
        font.getData().setScale(0.48f);

        if (section == 1) {
            for (int i = 0; i < GameProfile.CLUBS.length; i++) {
                int column = i % 2;
                int row = i / 2;
                float x = 195 + column * 255f;
                float y = 400 - row * 45f;
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(kitPreviews[i], x + 5, y - 10, 25, 40);
                boolean owned = GameProfile.isKitOwned(i);
                boolean selected = i == GameProfile.kit();
                String state = "REC " + GameProfile.receptionLevel(i) + "  PASE " + GameProfile.passLevel(i)
                    + "  |  " + (selected ? "EQUIPADO" : owned ? "EQUIPAR" : GameProfile.COSTS[i] + " coins");
                font.draw(batch, GameProfile.CLUBS[i], x + 35, y + 20);
                font.draw(batch, state, x + 35, y + 7);
            }
        } else {
            for (int i = 0; i < GameProfile.STADIUMS.length; i++) {
                int column = i % 2;
                int row = i / 2;
                float x = 195 + column * 255f;
                float y = 350 - row * 105f;
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(stadiumPreviews[i], x + 5, y - 30, 92, 68);
                boolean owned = GameProfile.isStadiumOwned(i);
                boolean selected = i == GameProfile.stadium();
                String state = selected ? "EQUIPADO" : owned ? "EQUIPAR" : GameProfile.STADIUM_COSTS[i] + " coins";
                font.draw(batch, GameProfile.STADIUMS[i], x + 102, y + 8);
                font.draw(batch, state, x + 102, y - 8);
            }
        }

        font.getData().setScale(0.55f);
        font.draw(batch, "Coins: " + GameProfile.coins(), 195, 88);
        font.draw(batch, notice + "   |   Volver", 420, 88);
        font.getData().setScale(0.62f);
        font.draw(batch, "Volver", 48, 36);
        batch.end();
    }

    private void drawStoreCard(float x, float y, float width, float height) {
        shapes.setColor(0.04f, 0.04f, 0.04f, 0.9f);
        shapes.rect(x, y, width, height);
        shapes.setColor(1f, 1f, 1f, 0.98f);
        shapes.rect(x + 1f, y + 1f, width - 2f, height - 2f);
    }

    @Override
    public void hide() {
        menuMusic.stop();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        menuMusic.dispose();
        menuBackground.dispose();
        for (Texture preview : stadiumPreviews) preview.dispose();
        for (Texture preview : kitPreviews) preview.dispose();
    }
}
