package com.buttclapdev.main.control;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Keyboard implements KeyListener {
    private final static  int NUMBER_KEY = 256;
    private boolean[] keys = new boolean[NUMBER_KEY];
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    public boolean faster;
    public boolean quit;
    public void update(){
        up = keys[KeyEvent.VK_W];
        down = keys[KeyEvent.VK_S];
        left = keys[KeyEvent.VK_A];
        right = keys[KeyEvent.VK_D];

        faster = keys[KeyEvent.VK_SHIFT];

        quit = keys[KeyEvent.VK_ESCAPE];
    }
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }
}
