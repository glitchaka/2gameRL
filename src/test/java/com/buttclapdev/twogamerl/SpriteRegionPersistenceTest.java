package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.Asset;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

final class SpriteRegionPersistenceTest {
    @Test void projectStoresOneSheetAndVirtualRegionsOnly() throws Exception {
        BufferedImage image=new BufferedImage(16,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++)for(int x=0;x<16;x++)image.setRGB(x,y,0xFF000000 | (x<<16) | (y<<8));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);

        GameProject project=GameProject.createDefault();
        project.getAssets().clear();
        project.getAssets().put("sheet.png",new Asset("sheet.png","sheet.png",bytes.toByteArray(),true));
        project.getAssets().put("hero",new Asset("hero","hero","sheet.png",4,2,6,5));

        var temp=Files.createTempFile("2gamerl-region-",".2grl");
        ProjectIO.save(project,temp);
        GameProject loaded=ProjectIO.load(temp);

        Asset source=loaded.getAssets().get("sheet.png"),region=loaded.getAssets().get("hero");
        assertNotNull(source);assertNotNull(source.data);assertTrue(source.sourceOnly);
        assertNotNull(region);assertTrue(region.isRegion());assertNull(region.data,"La región no debe duplicar bytes de la spritesheet");
        assertEquals("sheet.png",region.sourceAssetKey);assertEquals(4,region.regionX);assertEquals(2,region.regionY);assertEquals(6,region.regionWidth);assertEquals(5,region.regionHeight);
        assertArrayEquals(bytes.toByteArray(),source.data);
        Files.deleteIfExists(temp);
    }
}
