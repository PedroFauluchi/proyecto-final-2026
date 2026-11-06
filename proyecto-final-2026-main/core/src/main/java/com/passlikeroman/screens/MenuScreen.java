package com.passlikeroman.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.passlikeroman.data.GameProfile;
import com.passlikeroman.PassLikeRoman;
import com.passlikeroman.util.PixelFonts;

public class MenuScreen extends ScreenAdapter {

    private final PassLikeRoman game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Texture menuBackground;
    private final Texture menuArtwork;
    private final Texture stadiumShopArtwork;
    private final Texture shirtsShopArtwork;
    private final Texture coinSign;
    private final Texture playButton;
    private final Texture shirtsButton;
    private final Texture stadiumsButton;
    private final Texture[] stadiumCards = new Texture[GameProfile.STADIUMS.length];
    private final Texture[] kitCards = new Texture[GameProfile.CLUBS.length];
    private final Music menuMusic;
    private float transition = 0.35f;
    private boolean startingGame = false;
    private int section = 0;
    private int stadiumScrollIndex = 0;
    private int kitScrollIndex = 0;

    public MenuScreen(PassLikeRoman game) {
        this.game = game;
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = PixelFonts.create();
        menuBackground = new Texture(Gdx.files.internal("menu-background.png"));
        menuArtwork = new Texture(Gdx.files.internal("menu-screen.png"));
        stadiumShopArtwork = new Texture(Gdx.files.internal("menu-elements/stadium-shop-background.png"));
        shirtsShopArtwork = new Texture(Gdx.files.internal("menu-elements/shirts-shop-background.png"));
        coinSign = new Texture(Gdx.files.internal("menu-elements/coin-sign.png"));
        playButton = new Texture(Gdx.files.internal("menu-elements/button-play.png"));
        shirtsButton = new Texture(Gdx.files.internal("menu-elements/button-shirts.png"));
        stadiumsButton = new Texture(Gdx.files.internal("menu-elements/button-stadiums.png"));
        for (int i = 0; i < stadiumCards.length; i++) {
            stadiumCards[i] = new Texture(Gdx.files.internal("stadium-cards/stadium-" + i + ".png"));
            stadiumCards[i].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        for (int i = 0; i < kitCards.length; i++) {
            kitCards[i] = new Texture(Gdx.files.internal("kit-cards/kit-" + i + ".png"));
            kitCards[i].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("music/menu.mp3"));
        menuMusic.setLooping(true);
        menuMusic.setVolume(0.22f);
        menuMusic.play();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float x = screenX * 900f / Gdx.graphics.getWidth();
                float viewHeight = section == 0 ? 506.25f : 520f;
                float y = viewHeight - screenY * viewHeight / Gdx.graphics.getHeight();

                if (startingGame) return true;
                if (section == 0) {
                    if (x > 295 && x < 605 && y > 242 && y < 316) startingGame = true;
                    else if (x > 295 && x < 605 && y > 157 && y < 230) section = 1;
                    else if (x > 295 && x < 605 && y > 70 && y < 148) section = 2;
                    else if (x < 112 && y < 145) Gdx.app.exit();
                } else if (section == 1) {
                    if (x < 112 && y < 145) {
                        section = 0;
                    } else if (y > 205 && y < 320 && x < 130) {
                        kitScrollIndex = Math.max(0, kitScrollIndex - 1);
                    } else if (y > 205 && y < 320 && x > 785) {
                        kitScrollIndex = Math.min(kitCards.length - 5, kitScrollIndex + 1);
                    } else if (y > 145 && y < 390) {
                        for (int slot = 0; slot < 5; slot++) {
                            float cardX = 130f + slot * 130f;
                            if (x >= cardX && x <= cardX + 122f) {
                                int index = kitScrollIndex + slot;
                                if (index < kitCards.length) {
                                    GameProfile.buyOrSelectKit(index);
                                }
                                break;
                            }
                        }
                    }
                } else if (section == 2) {
                    if (x < 112 && y < 145) {
                        section = 0;
                    } else if (y > 205 && y < 300 && x < 100) {
                        stadiumScrollIndex = Math.max(0, stadiumScrollIndex - 1);
                    } else if (y > 205 && y < 300 && x > 800) {
                        stadiumScrollIndex = Math.min(stadiumCards.length - 3, stadiumScrollIndex + 1);
                    } else if (y > 130 && y < 380) {
                        for (int slot = 0; slot < 3; slot++) {
                            float cardX = 136f + slot * 219f;
                            if (x >= cardX && x <= cardX + 180f) {
                                int index = stadiumScrollIndex + slot;
                                if (index < stadiumCards.length) {
                                    GameProfile.buyOrSelectStadium(index);
                                }
                                break;
                            }
                        }
                    }
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

        camera.setToOrtho(false, 900, section == 0 ? 506.25f : 520f);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        if (section == 0) drawMenuPanel();
        else if (section == 1) drawKitShop();
        else if (section == 2) drawStadiumShop();
        else {
            drawSimpleBackground();
        }
        if (transition > 0f) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.01f, 0.02f, 0.04f, transition);
            shapes.rect(0, 0, 900, section == 0 ? 506.25f : 520f);
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
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(menuArtwork, 0, 0, 900, 506.25f);
        drawMenuElement(coinSign, 34f, 26f);
        drawMenuElement(playButton, 475f, 309f);
        drawMenuElement(shirtsButton, 475f, 448f);
        drawMenuElement(stadiumsButton, 475f, 584f);
        batch.end();

        drawCoinsAmount(menuArtwork, 506.25f);
    }

    private void drawCoinsAmount(Texture artwork, float viewHeight) {
        float scaleX = 900f / artwork.getWidth();
        float scaleY = viewHeight / artwork.getHeight();
        float amountX = 145f * scaleX;
        float amountY = (artwork.getHeight() - 127f) * scaleY;
        float amountWidth = 137f * scaleX;
        float amountHeight = 37f * scaleY;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.59f, 0.17f, 0.16f, 1f);
        shapes.rect(amountX, amountY, amountWidth, amountHeight);
        shapes.end();

        String coinsText = Integer.toString(GameProfile.coins());
        font.getData().setScale(1f);
        GlyphLayout layout = new GlyphLayout(font, coinsText);
        float textScale = Math.min(1.1f, amountWidth / (layout.width + 8f));
        font.getData().setScale(textScale);
        layout.setText(font, coinsText);
        float textX = amountX + (amountWidth - layout.width) / 2f;
        float textY = amountY + (amountHeight + layout.height) / 2f - 2f;
        batch.begin();
        font.setColor(0.10f, 0.025f, 0.025f, 1f);
        font.draw(batch, coinsText, textX + 1f, textY - 2f);
        font.setColor(1f, 0.78f, 0.05f, 1f);
        font.draw(batch, coinsText, textX, textY);
        batch.end();
        font.getData().setScale(0.55f);
    }

    private void drawMenuElement(Texture texture, float sourceX, float sourceY) {
        float scaleX = 900f / menuArtwork.getWidth();
        float scaleY = 506.25f / menuArtwork.getHeight();
        float x = sourceX * scaleX;
        float y = (menuArtwork.getHeight() - sourceY - texture.getHeight()) * scaleY;
        batch.draw(texture, x, y, texture.getWidth() * scaleX, texture.getHeight() * scaleY);
    }

    private void drawStadiumShop() {
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(stadiumShopArtwork, 0f, 0f, 900f, 520f);
        batch.end();

        drawCoinsAmount(stadiumShopArtwork, 520f);

        float[] cardX = {136f, 355f, 574f};
        float cardWidth = 180f;
        float cardY = 135f;
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        for (int slot = 0; slot < 3; slot++) {
            Texture card = stadiumCards[stadiumScrollIndex + slot];
            float cardHeight = cardWidth * card.getHeight() / card.getWidth();
            batch.draw(card, cardX[slot], cardY, cardWidth, cardHeight);
        }
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.08f, 0.17f, 1f);
        for (int slot = 0; slot < 3; slot++) {
            shapes.rect(cardX[slot] + 12f, cardY + 10f, cardWidth - 42f, 22f);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(0.48f);
        font.setColor(1f, 0.82f, 0.18f, 1f);
        for (int slot = 0; slot < 3; slot++) {
            int index = stadiumScrollIndex + slot;
            String status = index == GameProfile.stadium()
                ? "SELECCIONADO"
                : GameProfile.isStadiumOwned(index)
                    ? "COMPRADO" : GameProfile.STADIUM_COSTS[index] + " COINS";
            font.draw(batch, status, cardX[slot] + 18f, cardY + 26f);
        }
        font.getData().setScale(0.55f);
        batch.end();
    }

    private void drawKitShop() {
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(shirtsShopArtwork, 0f, 0f, 900f, 520f);
        batch.end();

        drawCoinsAmount(shirtsShopArtwork, 520f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.08f, 0.17f, 1f);
        for (int slot = 0; slot < 5; slot++) {
            shapes.rect(140f + slot * 130f, 145f, 104f, 22f);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(0.48f);
        for (int slot = 0; slot < 5; slot++) {
            int index = kitScrollIndex + slot;
            Texture card = kitCards[index];
            float x = 132f + slot * 130f;
            float cardWidth = 120f;
            float cardHeight = cardWidth * card.getHeight() / card.getWidth();
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(card, x, 177f, cardWidth, cardHeight);

            boolean selected = index == GameProfile.kit();
            boolean owned = GameProfile.isKitOwned(index);
            String status = selected ? "SELECCIONADA"
                : owned ? "COMPRADA" : GameProfile.COSTS[index] + " COINS";
            GlyphLayout statusLayout = new GlyphLayout(font, status);
            font.setColor(selected ? 0.48f : 1f, selected ? 1f : 0.83f, 0.20f, 1f);
            font.draw(batch, status, x + (120f - statusLayout.width) / 2f, 162f);
        }
        font.getData().setScale(0.55f);
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
        menuArtwork.dispose();
        stadiumShopArtwork.dispose();
        shirtsShopArtwork.dispose();
        coinSign.dispose();
        playButton.dispose();
        shirtsButton.dispose();
        stadiumsButton.dispose();
        for (Texture card : stadiumCards) card.dispose();
        for (Texture card : kitCards) card.dispose();
    }
}
