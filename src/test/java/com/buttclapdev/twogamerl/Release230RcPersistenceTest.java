package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Release230RcPersistenceTest {
    @TempDir Path temp;

    @Test void roundTripAllRcResources() throws Exception {
        GameProject p=GameProject.createDefault();
        Asset asset=p.getAssets().get("placeholder-player.png");asset.favorite=true;asset.folder="Personajes/Jugador";
        TileDef tile=p.getTiles().get(0);tile.autotileMode=TileAutotileMode.EIGHT_WAY;tile.connectTag="ground";tile.variants.add(new TileVariant(asset.key,2.5));tile.autotileAssets.put(15,asset.key);tile.autotileAssets.put(255,asset.key);
        AnimationClip clip=new AnimationClip("attack","Attack");clip.reverse=true;clip.fps=12;AnimationFrame frame=new AnimationFrame(asset.key,.25);frame.shapes.add(new FrameShape("blade",FrameShapeRole.HITBOX,.5,.1,.7,.3));frame.shapes.add(new FrameShape("body",FrameShapeRole.HURTBOX,.1,.1,.8,.8));clip.frames.add(frame);p.getAnimationClips().put(clip.key,clip);
        AnimatorController controller=new AnimatorController("hero","Hero");controller.parameters.put("speed",new AnimatorParameter("speed",AnimatorParameterType.FLOAT,"0"));controller.parameters.put("attack",new AnimatorParameter("attack",AnimatorParameterType.TRIGGER,"false"));AnimatorState state=new AnimatorState("Walk",clip.key);state.blendTree.type=BlendTreeType.TWO_D;state.blendTree.parameterX="speed";state.blendTree.parameterY="speed";state.blendTree.children.add(new BlendChild(clip.key,1,0));controller.states.put(state.name,state);p.getAnimatorControllers().put(controller.key,controller);
        UISkinAsset skin=new UISkinAsset("button","Button");skin.normalAssetKey=asset.key;skin.hoverAssetKey=asset.key;skin.pressedAssetKey=asset.key;skin.disabledAssetKey=asset.key;skin.textColor=Color.YELLOW;skin.radius=11;p.getUiSkins().put(skin.key,skin);p.getTextStyles().get("default-bubble").uiSkinKey=skin.key;p.getMenus().get("main").buttons.getFirst().uiSkinKey=skin.key;p.getMenus().get("main").buttons.getFirst().enabled=false;
        AnimationSet set=new AnimationSet("hero-set","Hero Set");set.directionMode=DirectionMode.EIGHT;set.useFlipX=true;set.clips.put("Walk|DOWN",clip.key);p.getAnimationSets().put(set.key,set);
        StampPattern stamp=new StampPattern("corner","Corner",2,2);stamp.set(0,0,0);stamp.set(1,0,1);stamp.set(0,1,-1);stamp.set(1,1,0);p.getStampPatterns().put(stamp.key,stamp);

        Path file=temp.resolve("rc.2grl");ProjectIO.save(p,file);GameProject q=ProjectIO.load(file);
        assertEquals(9,GameProject.FORMAT_VERSION);assertTrue(q.getAssets().get(asset.key).favorite);assertEquals("Personajes/Jugador",q.getAssets().get(asset.key).folder);
        TileDef qt=q.getTiles().get(0);assertEquals(TileAutotileMode.EIGHT_WAY,qt.autotileMode);assertEquals("ground",qt.connectTag);assertEquals(2.5,qt.variants.getFirst().weight,.0001);assertEquals(asset.key,qt.autotileAssets.get(255));
        AnimationClip qc=q.getAnimationClips().get("attack");assertTrue(qc.reverse);assertEquals(12,qc.fps,.0001);assertEquals(2,qc.frames.getFirst().shapes.size());assertEquals(FrameShapeRole.HITBOX,qc.frames.getFirst().shapes.getFirst().role);
        AnimatorController qctrl=q.getAnimatorControllers().get("hero");assertEquals(AnimatorParameterType.TRIGGER,qctrl.parameters.get("attack").type);assertEquals(BlendTreeType.TWO_D,qctrl.states.get("Walk").blendTree.type);assertEquals(1,qctrl.states.get("Walk").blendTree.children.size());
        assertEquals(Color.YELLOW,q.getUiSkins().get("button").textColor);assertEquals("button",q.getTextStyles().get("default-bubble").uiSkinKey);assertFalse(q.getMenus().get("main").buttons.getFirst().enabled);
        assertEquals(DirectionMode.EIGHT,q.getAnimationSets().get("hero-set").directionMode);assertEquals("attack",q.getAnimationSets().get("hero-set").clip("Walk","DOWN"));
        assertArrayEquals(new int[]{0,1,-1,0},q.getStampPatterns().get("corner").cells());
    }

    @Test void rcDefaultsRemainSafeWhenNoSidecarExists() throws Exception {
        GameProject p=GameProject.createDefault();Path file=temp.resolve("plain.2grl");ProjectIO.save(p,file);GameProject q=ProjectIO.load(file);
        assertNotNull(q);assertTrue(q.getUiSkins().isEmpty());assertTrue(q.getAnimationSets().isEmpty());assertTrue(q.getStampPatterns().isEmpty());assertEquals(TileAutotileMode.NONE,q.getTiles().get(0).autotileMode);
    }
}
