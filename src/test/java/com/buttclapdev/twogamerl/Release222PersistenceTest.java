package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class Release222PersistenceTest {
    @TempDir Path temp;

    @Test void release222ResourcesRoundTripWithoutLosingNewFields() throws Exception {
        GameProject p=GameProject.createDefault();
        Asset skin=p.getAssets().get("placeholder-player.png");skin.sliceTop=2;skin.sliceRight=3;skin.sliceBottom=4;skin.sliceLeft=5;
        TextStyle style=p.getTextStyles().get("default-bubble");style.skinAssetKey=skin.key;
        SoundAsset sound=new SoundAsset("hit","Hit",new byte[]{82,73,70,70,0,0,0,0});sound.volume=.35;sound.loop=true;sound.category=SoundCategory.SFX;p.getSounds().put(sound.key,sound);
        ParticlePreset particles=new ParticlePreset("test_particle","test_particle");particles.assetKey=skin.key;particles.rate=37;particles.lifetime=2.5;particles.speed=4.2;particles.direction=135;particles.spread=22;particles.gravity=-1.5;particles.startScale=1.7;particles.endScale=.4;particles.startOpacity=.85;particles.endOpacity=.1;particles.burst=true;particles.burstCount=44;particles.localSpace=false;p.getParticlePresets().put(particles.key,particles);
        Path file=temp.resolve("release222.2grl");ProjectIO.save(p,file);GameProject loaded=ProjectIO.load(file);
        Asset loadedSkin=loaded.getAssets().get(skin.key);assertEquals(2,loadedSkin.sliceTop);assertEquals(3,loadedSkin.sliceRight);assertEquals(4,loadedSkin.sliceBottom);assertEquals(5,loadedSkin.sliceLeft);assertEquals(skin.key,loaded.getTextStyles().get("default-bubble").skinAssetKey);
        SoundAsset loadedSound=loaded.getSounds().get("hit");assertNotNull(loadedSound);assertEquals(.35,loadedSound.volume,.0001);assertTrue(loadedSound.loop);assertEquals(SoundCategory.SFX,loadedSound.category);assertArrayEquals(sound.data,loadedSound.data);
        ParticlePreset q=loaded.getParticlePresets().get("test_particle");assertNotNull(q);assertEquals(135,q.direction,.0001);assertEquals(22,q.spread,.0001);assertEquals(.85,q.startOpacity,.0001);assertEquals(.1,q.endOpacity,.0001);assertEquals(44,q.burstCount);assertFalse(q.localSpace);
    }

    @Test void particleRenameMigratesComponentsPrefabsAndScripts(){
        GameProject p=GameProject.createDefault();ParticlePreset preset=new ParticlePreset("particles","particles");p.getParticlePresets().put(preset.key,preset);Level l=p.getLevels().get("level-1");EntityDef e=new EntityDef("fx","FX",1,1);ComponentDef emitter=ComponentDef.preset("ParticleEmitter2D");emitter.properties.put("preset","particles");e.components.add(emitter);e.script="on start\n  ParticleEmitter2D.preset = particles\nend\n";l.entities.add(e);EntityDef template=e.copy();template.prefabKey="";PrefabDef pf=PrefabDef.entity("fx-prefab","FX Prefab",template);p.getPrefabs().put(pf.key,pf);
        String renamed=p.renameParticlePreset("particles","test_particle");assertEquals("test_particle",renamed);assertFalse(p.getParticlePresets().containsKey("particles"));assertNotNull(p.getParticlePresets().get("test_particle"));assertEquals("test_particle",e.component("ParticleEmitter2D").get("preset",""));assertTrue(e.script.contains("ParticleEmitter2D.preset = test_particle"));assertEquals("test_particle",pf.template.component("ParticleEmitter2D").get("preset",""));
    }

    @Test void new2GameScriptCommandsAreRealParserInstructions(){
        String source="""
                on start
                  playSound hit 0.7
                  stopSound hit
                  playMusic theme 0.5
                  stopMusic
                  cameraShake 8 0.3
                end
                """;
        ScriptProgram.Validation validation=ScriptProgram.compile(source).validation();assertTrue(validation.valid(),()->String.join(" | ",validation.errors()));
        assertFalse(ScriptProgram.compile("on start\n  playSound hit 2\nend\n").validation().valid());
        assertFalse(ScriptProgram.compile("on start\n  cameraShake -1 1\nend\n").validation().valid());
    }

    @Test void cameraAndParticleDefaultsExpose222Properties(){
        ComponentDef camera=ComponentDef.preset("Camera2D");assertTrue(camera.properties.keySet().containsAll(java.util.Set.of("smoothSpeed","deadZoneX","deadZoneY","shakeIntensity","shakeDuration")));
        GameProject p=GameProject.createDefault();ParticlePreset preset=new ParticlePreset("fx","fx");p.getParticlePresets().put(preset.key,preset);assertEquals(-90,preset.direction,.0001);assertSame(preset,p.particlePreset("fx"));
    }
}
