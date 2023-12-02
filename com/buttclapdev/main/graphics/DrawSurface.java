package com.buttclapdev.main.graphics;

import com.buttclapdev.main.control.Keyboard;
import com.buttclapdev.main.statemachine.state.StateManager;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.Serializable;

public class DrawSurface extends Canvas implements Serializable {
    //atributos
    private static final long serialVersionUID = -2284879212465893870L;

    private  int width;
    private int height;

    private Keyboard keyboard;

    //constructores
    public DrawSurface(final int width, final int height){
        this.width = width;
        this.height = height;

        keyboard = new Keyboard();

        setIgnoreRepaint(true);
        setPreferredSize(new Dimension(width, height));
        addKeyListener(keyboard);
        setFocusable(true);
        requestFocus();
    }
    public void draw(final StateManager stateManager){
        BufferStrategy buffer = getBufferStrategy();

        if (buffer == null){
            createBufferStrategy(3);
            return;
        }
        Graphics g = buffer.getDrawGraphics();
        g.setColor(Color.decode("#3d2b3d"));
        g.fillRect(0, 0, width, height);

        stateManager.draw(g);

        Toolkit.getDefaultToolkit().sync();
        g.dispose();
        buffer.show();
    }

    public Keyboard getKeyboard(){
        return keyboard;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
