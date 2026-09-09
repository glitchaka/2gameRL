package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceIntegrity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceIntegrityTest {
    @Test void replacementMigratesAllMajorAssetReferences(){
        GameProject p=GameProject.createDefault();String old="placeholder-player.png",next="replacement.png";Asset base=p.getAssets().get(old);p.getAssets().put(next,new Asset(next,next,base.data.clone()));
        TileDef tile=p.getTiles().get(0);tile.assetKey=old;tile.variants.add(new TileVariant(old,1));tile.autotileAssets.put(15,old);
        AnimationClip clip=new AnimationClip("clip","Clip");clip.frames.add(new AnimationFrame(old,.1));p.getAnimationClips().put(clip.key,clip);
        ParticlePreset particle=new ParticlePreset("fx","FX");particle.assetKey=old;p.getParticlePresets().put(particle.key,particle);
        UISkinAsset skin=new UISkinAsset("skin","Skin");skin.normalAssetKey=old;skin.hoverAssetKey=old;skin.pressedAssetKey=old;skin.disabledAssetKey=old;p.getUiSkins().put(skin.key,skin);
        TextStyle style=p.getTextStyles().get("default-bubble");style.skinAssetKey=old;
        Level level=p.getLevels().get("level-1");level.backgroundAssetKey=old;EntityDef e=new EntityDef("hero","Hero",1,1);e.assetKey=old;e.script="on start\n setSprite "+old+"\nend\n";level.entities.add(e);
        PrefabDef prefab=PrefabDef.entity("prefab","Prefab",e.copy());p.getPrefabs().put(prefab.key,prefab);
        MenuScreen menu=p.getMenus().get("main");menu.backgroundAssetKey=old;menu.titleAssetKey=old;menu.buttons.getFirst().assetKey=old;menu.buttons.getFirst().hoverAssetKey=old;
        assertFalse(ResourceIntegrity.assetUses(p,old).isEmpty());int changed=ResourceIntegrity.replaceAsset(p,old,next);assertTrue(changed>=12);assertTrue(ResourceIntegrity.assetUses(p,old).isEmpty());assertTrue(ResourceIntegrity.brokenAssetReferences(p).isEmpty());assertEquals(next,e.assetKey);assertTrue(e.script.contains(next));
    }

    @Test void brokenReferenceAuditFindsMissingRcReferences(){GameProject p=GameProject.createDefault();TileDef t=p.getTiles().get(0);t.variants.add(new TileVariant("missing-variant",1));t.autotileAssets.put(0,"missing-auto");UISkinAsset s=new UISkinAsset("skin","Skin");s.pressedAssetKey="missing-pressed";p.getUiSkins().put(s.key,s);ParticlePreset fx=new ParticlePreset("fx","FX");fx.assetKey="missing-particle";p.getParticlePresets().put(fx.key,fx);List<String>b=ResourceIntegrity.brokenAssetReferences(p);assertTrue(b.stream().anyMatch(x->x.contains("missing-variant")));assertTrue(b.stream().anyMatch(x->x.contains("missing-auto")));assertTrue(b.stream().anyMatch(x->x.contains("missing-pressed")));assertTrue(b.stream().anyMatch(x->x.contains("missing-particle")));}
}
