package com.buttclapdev.main.graphics;

import com.buttclapdev.main.control.Keyboard;

import java.awt.*;
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
    }

}
