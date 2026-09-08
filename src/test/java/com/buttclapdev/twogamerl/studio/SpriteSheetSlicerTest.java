package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SpriteSheetSlicerTest {
    @Test
    void bundledSheetCutsIntoOneHundredExact32pxSpritesWithoutFiltering() throws Exception {
        byte[] bytes;
        try (InputStream in=getClass().getResourceAsStream("/samples/TexturesSheet/test.png")) {
            assertNotNull(in); bytes=in.readAllBytes();
        }
        Asset source=new Asset("sheet.png","sheet.png",bytes);
        var original=source.image();
        assertEquals(320,original.getWidth());assertEquals(320,original.getHeight());
        var slices=SpriteSheetSlicer.slice(source,32,32,0,0,0,0,Set.of(),true);
        assertEquals(100,slices.size());
        var first=slices.getFirst().image();var last=slices.getLast().image();
        assertEquals(32,first.getWidth());assertEquals(32,first.getHeight());
        for(int y=0;y<32;y++)for(int x=0;x<32;x++)assertEquals(original.getRGB(x,y),first.getRGB(x,y),"El slicer alteró el píxel "+x+","+y);
        for(int y=0;y<32;y++)for(int x=0;x<32;x++)assertEquals(original.getRGB(288+x,288+y),last.getRGB(x,y),"El slicer alteró el último sprite");
    }

    @Test
    void marginsSpacingAndSelectionUseExactCellCoordinates() throws Exception {
        var image=new java.awt.image.BufferedImage(7,4,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<4;y++)for(int x=0;x<7;x++)image.setRGB(x,y,0xff000000|(x<<16)|(y<<8)|(x+y));
        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        Asset source=new Asset("tiny.png","tiny.png",out.toByteArray());
        var slices=SpriteSheetSlicer.slice(source,2,2,1,1,1,0,Set.of(1),false);
        assertEquals(1,slices.size());var sprite=slices.getFirst().image();
        assertEquals(image.getRGB(4,1),sprite.getRGB(0,0));assertEquals(image.getRGB(5,2),sprite.getRGB(1,1));
    }
}
