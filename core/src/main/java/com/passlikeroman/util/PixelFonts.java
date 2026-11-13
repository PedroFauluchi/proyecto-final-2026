package com.passlikeroman.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/** Creates the game's UI font from the bundled Pixeloid Sans typeface. */
public final class PixelFonts {
    private static final String FONT_FILE = "fonts/pixeloid/PixeloidSans.ttf";
    private static final String EXTRA_SPANISH_CHARACTERS = "áéíóúÁÉÍÓÚñÑüÜ¿¡";

    private PixelFonts() {}

    public static BitmapFont create() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 18;
            parameter.characters += EXTRA_SPANISH_CHARACTERS;
            parameter.minFilter = Texture.TextureFilter.Nearest;
            parameter.magFilter = Texture.TextureFilter.Nearest;
            BitmapFont font = generator.generateFont(parameter);
            font.setUseIntegerPositions(true);
            return font;
        } finally {
            generator.dispose();
        }
    }
}
