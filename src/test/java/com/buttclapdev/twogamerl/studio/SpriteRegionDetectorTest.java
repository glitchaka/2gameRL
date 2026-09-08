package com.buttclapdev.twogamerl.studio;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SpriteRegionDetectorTest {
    @Test void detectsIndependentTransparentSpritesByContour() {
        BufferedImage image=new BufferedImage(24,14,BufferedImage.TYPE_INT_ARGB);
        fill(image,1,2,4,5,0xFFFF0000);
        fill(image,13,3,6,7,0xFF00FF00);

        List<SpriteRegionDetector.Box> boxes=SpriteRegionDetector.detect(image,8,12,1,0,0);

        assertEquals(2,boxes.size());
        assertEquals(new SpriteRegionDetector.Box(1,2,4,5,20),boxes.get(0));
        assertEquals(new SpriteRegionDetector.Box(13,3,6,7,42),boxes.get(1));
    }

    @Test void detectsSpritesAgainstOpaqueBackgroundColor() {
        BufferedImage image=new BufferedImage(30,18,BufferedImage.TYPE_INT_ARGB);
        fill(image,0,0,30,18,0xFF5B6078);
        fill(image,2,3,7,8,0xFFB7B9C8);
        fill(image,18,4,8,10,0xFF242633);

        List<SpriteRegionDetector.Box> boxes=SpriteRegionDetector.detect(image,8,10,4,0,0);

        assertEquals(2,boxes.size());
        assertEquals(2,boxes.get(0).x());
        assertEquals(3,boxes.get(0).y());
        assertEquals(7,boxes.get(0).width());
        assertEquals(8,boxes.get(0).height());
        assertEquals(18,boxes.get(1).x());
        assertEquals(4,boxes.get(1).y());
        assertEquals(8,boxes.get(1).width());
        assertEquals(10,boxes.get(1).height());
    }

    @Test void mergeGapCanJoinDetachedPartsOfOneSprite() {
        BufferedImage image=new BufferedImage(20,12,BufferedImage.TYPE_INT_ARGB);
        fill(image,2,2,3,5,0xFFFFFFFF);
        fill(image,6,3,2,3,0xFFFFFFFF);

        List<SpriteRegionDetector.Box> split=SpriteRegionDetector.detect(image,8,12,1,0,0);
        List<SpriteRegionDetector.Box> merged=SpriteRegionDetector.detect(image,8,12,1,1,0);

        assertEquals(2,split.size());
        assertEquals(1,merged.size());
        assertEquals(2,merged.getFirst().x());
        assertEquals(2,merged.getFirst().y());
        assertEquals(6,merged.getFirst().width());
        assertEquals(5,merged.getFirst().height());
    }

    private static void fill(BufferedImage image,int x,int y,int w,int h,int argb){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)image.setRGB(xx,yy,argb);}
}
