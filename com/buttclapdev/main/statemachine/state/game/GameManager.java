package com.buttclapdev.main.statemachine.state.game;

import com.buttclapdev.main.sprites.SpriteSheet;
import com.buttclapdev.main.statemachine.state.GameState;

import java.awt.*;

public class GameManager implements GameState {
    private MapManager mapManager;

    SpriteSheet spriteSheet = new SpriteSheet("resources/Images/TexturesSheet/test.png", 32, true);

    public void update() {

    }

    public void draw(Graphics g) {

    }
}
