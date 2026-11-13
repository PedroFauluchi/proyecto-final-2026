package com.passlikeroman;

import com.badlogic.gdx.Game;
import com.passlikeroman.screens.MenuScreen;

public class PassLikeRoman extends Game {

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }
}
