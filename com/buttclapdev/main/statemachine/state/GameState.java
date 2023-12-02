package com.buttclapdev.main.statemachine.state;

import java.awt.*;

public interface GameState {
    void update();
    void draw(final Graphics g);
}
