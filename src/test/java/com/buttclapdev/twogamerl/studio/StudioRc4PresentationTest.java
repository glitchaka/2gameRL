package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.FilterMode;
import com.buttclapdev.twogamerl.model.GameProject.ScaleMode;
import com.buttclapdev.twogamerl.model.GameProject.ScreenMode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StudioRc4PresentationTest {
    @Test void enumValuesAreNeverPresentedAsImplementationConstants(){
        assertEquals("Pantalla completa sin bordes",StudioControlPolish.enumLabel(ScreenMode.FULLSCREEN_BORDERLESS));
        assertEquals("Píxel perfecto",StudioControlPolish.enumLabel(ScaleMode.PIXEL_PERFECT));
        assertEquals("Píxel (sin suavizado)",StudioControlPolish.enumLabel(FilterMode.PIXEL));
        String help=StudioControlPolish.humanizeVisibleConstants("PIXEL_PERFECT · FULLSCREEN_BORDERLESS · AUTO→TILEMAP");
        assertFalse(help.contains("PIXEL_PERFECT"));
        assertFalse(help.contains("FULLSCREEN_BORDERLESS"));
        assertEquals("Píxel perfecto · Pantalla completa sin bordes · Automático → Tilemap",help);
    }

    @Test void rc4DesktopSkinContainsRealBrowserAndToolControlRules() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/com/buttclapdev/twogamerl/studio-rc4.css")){
            assertNotNull(in);
            String css=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            for(String required:new String[]{"resource-sidebar","resource-details","resource-folder-strip","resource-asset-tile","scene-tool-button","studio-glyph"})
                assertTrue(css.contains(required),required);
        }
    }
}
