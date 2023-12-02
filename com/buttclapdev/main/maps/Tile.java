package com.buttclapdev.main.maps;

import com.buttclapdev.main.sprites.Sprite;

import java.awt.*;

public class Tile {
    private final Sprite sprite;
    private final int id;
    private boolean solid;
    public Tile(Sprite sprite, final int id){
        this.sprite = sprite;
        this.id = id;
        solid = false;
    }
    public Tile(Sprite sprite, final int id, final boolean solid){
        this.sprite = sprite;
        this.id = id;
        this.solid = solid;
    }
    public Sprite getSprite(){
        return sprite;
    }

    public int getId() {
        return id;
    }
    public void setSolid(final boolean solid){
        this.solid = solid;
    }
    public Rectangle getBounds(final int x, final int y){
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
}
