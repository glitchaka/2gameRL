package com.buttclapdev.main.sprites;

import com.buttclapdev.main.tools.ResourceLoader;

import java.awt.image.BufferedImage;

public class SpriteSheet {

    final private int widthSheetInPixels;
    final private int heightSheetInPixels;

    final private int widthSheetInSprites;
    final private int heightSheetInSprites;

    final private int widthSprite;
    final private int heightSprite;

    final private Sprite[] sprite;

    public SpriteSheet(final String route, final int sizeSprite, final boolean sheetOpaque){
        final BufferedImage image;

        if (sheetOpaque){
            image = ResourceLoader.loadCompatibleImageOpaque(route);
        }else {
            image = ResourceLoader.loadCompatibleImageTranslucent(route);
        }
        widthSheetInPixels = image.getWidth();
        heightSheetInPixels = image.getHeight();

        widthSheetInSprites = widthSheetInPixels / sizeSprite;
        heightSheetInSprites = heightSheetInPixels / sizeSprite;

        widthSprite = sizeSprite;
        heightSprite = sizeSprite;

        sprite = new Sprite[widthSheetInSprites * heightSheetInSprites];

        fillSpriteFromImage(image);
    }
    public SpriteSheet(final String route, final int widthSprite, final int heightSprite, final boolean sheetOpaque){
        final BufferedImage image;

        if (sheetOpaque){
            image = ResourceLoader.loadCompatibleImageOpaque(route);
        }else {
            image = ResourceLoader.loadCompatibleImageTranslucent(route);
        }
        widthSheetInPixels = image.getWidth();
        heightSheetInPixels = image.getHeight();

        widthSheetInSprites = widthSheetInPixels / widthSprite;
        heightSheetInSprites = heightSheetInPixels / heightSprite;

        this.widthSprite = widthSprite;
        this.heightSprite = heightSprite;

        sprite = new Sprite[widthSheetInSprites * heightSheetInSprites];

        fillSpriteFromImage(image);
    }

    private void fillSpriteFromImage(final BufferedImage image){
        for (int y = 0; y < heightSheetInSprites; y++){
            for (int x = 0; x < widthSheetInSprites; x++){
                final int positionX = x * widthSprite;
                final int positionY = y * heightSprite;
                sprite[x + y * widthSheetInSprites] = new Sprite(image.getSubimage(positionX, positionY, widthSprite,
                        heightSprite));
            }
        }
    }

    public Sprite getSprite(final int index){
        return sprite[index];
    }
    public Sprite getSprite(final int x, final int y){
        return sprite[x + y * widthSheetInSprites];
    }
}
