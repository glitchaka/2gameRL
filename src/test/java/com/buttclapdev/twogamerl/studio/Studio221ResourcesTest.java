package com.buttclapdev.twogamerl.studio;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

final class Studio221ResourcesTest {
    @Test void canonicalBibleIsBundledAndSubstantial() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/manuals/2GAMESCRIPT_BIBLE.md")){
            assertNotNull(in,"La Biblia debe viajar dentro del Studio");
            String text=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(text.length()>25000,"La Biblia no puede degradarse a una referencia mínima");
            for(String required:new String[]{
                    "Biblia de 2GameScript 2.3.0-rc1","PlatformerController","Rigidbody2D.grounded","spawnPrefab","showText","playSound","cameraShake","ParticleEmitter2D",
                    "AnimationSet","UISkinAsset","BlendTree","while","Gramática práctica","Límites explícitos"
            }) assertTrue(text.contains(required),required);
        }
    }
    @Test void sixColorThemesAreBundled() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/com/buttclapdev/twogamerl/themes.css")){
            assertNotNull(in);String css=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            for(String theme:new String[]{"theme-midnight","theme-graphite","theme-light","theme-ember","theme-amethyst","theme-forest"})assertTrue(css.contains(theme),theme);
        }
    }
}
