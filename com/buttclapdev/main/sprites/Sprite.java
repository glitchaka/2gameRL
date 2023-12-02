package com.buttclapdev.main.sprites;

import java.awt.image.BufferedImage;

public class Sprite {

    private final BufferedImage image;

    private final int width;
    private final int height;

    public Sprite(final BufferedImage image){
        this.image = image;

        width = image.getWidth();
        height = image.getHeight();
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
