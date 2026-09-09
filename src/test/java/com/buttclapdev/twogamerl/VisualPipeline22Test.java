package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VisualPipeline22Test {
    @Test void defaultsArePixelFirstAndExposeInputTextAndBrushes(){
        GameProject p=GameProject.createDefault();
        assertEquals(320,p.getLogicalWidth());
        assertEquals(180,p.getLogicalHeight());
        assertEquals(ScreenMode.FULLSCREEN_BORDERLESS,p.getScreenMode());
        assertEquals(ScaleMode.PIXEL_PERFECT,p.getScaleMode());
        assertEquals(FilterMode.PIXEL,p.getDefaultFilter());
        assertTrue(p.isIntegerScale());
        assertTrue(p.action("Jump").bindings.contains("SPACE"));
        assertNotNull(p.getTextStyles().get("default-free"));
        assertNotNull(p.getTextStyles().get("default-bubble"));
        assertNotNull(p.getTextStyles().get("default-novel"));
        assertEquals(PlacementMode.TILEMAP,p.getPrefabs().get("suelo").placement);
        assertTrue(p.getLevels().get("level-1").entities.isEmpty());
    }

    @Test void roundTripPreservesAllNewVisualResources() throws Exception {
        GameProject p=GameProject.createDefault();
        p.setLogicalWidth(426);p.setLogicalHeight(240);p.setScreenMode(ScreenMode.FULLSCREEN);p.setScaleMode(ScaleMode.KEEP_ASPECT);p.setDefaultFilter(FilterMode.LINEAR);p.setIntegerScale(false);p.setUiSmooth(false);p.setLetterboxColor(new Color(3,4,5));

        TextStyle warning=new TextStyle("warning","Advertencia");warning.fontSize=31;warning.textColor=new Color(255,32,48);warning.backgroundColor=new Color(10,11,12,210);warning.borderWidth=3;warning.fadeOut=.8;p.getTextStyles().put(warning.key,warning);
        AnimationClip walk=new AnimationClip("walk","Walk");AnimationFrame f0=new AnimationFrame("sample-00-00",.08),f1=new AnimationFrame("sample-01-00",.12);f1.event="footstep";walk.frames.add(f0);walk.frames.add(f1);walk.pingPong=true;walk.speed=1.25;p.getAnimationClips().put(walk.key,walk);
        AnimatorController controller=new AnimatorController("hero-controller","Hero");controller.states.put("Walk",new AnimatorState("Walk","walk"));controller.defaultState="Walk";AnimatorTransition tr=new AnimatorTransition("AnyState","Walk");tr.path="Rigidbody2D.grounded";tr.operator="==";tr.value="true";controller.transitions.add(tr);p.getAnimatorControllers().put(controller.key,controller);
        EntityDef template=new EntityDef("slime","Slime",0,0);template.assetKey="sample-00-00";template.components.add(ComponentDef.preset("Animator"));template.components.add(ComponentDef.preset("Health"));PrefabDef prefab=PrefabDef.entity("slime","Slime",template);prefab.variantOverrides.put("Health.max","150");p.getPrefabs().put(prefab.key,prefab);
        p.getInputActions().put("Dash",new InputAction("Dash",List.of("LSHIFT","X")));
        PaletteAsset palette=new PaletteAsset("red","Red");palette.colors.put(Color.WHITE.getRGB(),Color.RED.getRGB());p.getPalettes().put(palette.key,palette);
        ParticlePreset sparks=new ParticlePreset("sparks","Sparks");sparks.assetKey="sample-00-00";sparks.rate=42;sparks.lifetime=.6;sparks.burst=true;sparks.burstCount=9;p.getParticlePresets().put(sparks.key,sparks);

        TileDef ground=p.getTiles().get(0);ground.walkable=false;ground.oneWay=true;ground.friction=.25;ground.damage=3;ground.animationClip="walk";
        Level level=p.getLevels().get("level-1");level.cameraTarget="hero";level.cameraZoom=1.5;level.cameraOffsetX=2;level.cameraOffsetY=-1;level.cameraPixelSnap=false;TileLayer layer=level.baseLayer();layer.opacity=.65;layer.parallaxX=.4;layer.parallaxY=.7;layer.ySort=true;
        EntityDef instance=new EntityDef("slime-1","Slime especial",5,6);instance.prefabKey="slime";instance.prefabOverrides.put("Health.current","22");level.entities.add(instance);

        Path file=Files.createTempFile("2gamerl-22-",".2grl");
        try{
            ProjectIO.save(p,file);GameProject q=ProjectIO.load(file);
            assertEquals(426,q.getLogicalWidth());assertEquals(240,q.getLogicalHeight());assertEquals(ScreenMode.FULLSCREEN,q.getScreenMode());assertEquals(ScaleMode.KEEP_ASPECT,q.getScaleMode());assertEquals(FilterMode.LINEAR,q.getDefaultFilter());assertFalse(q.isIntegerScale());assertFalse(q.isUiSmooth());assertEquals(new Color(3,4,5).getRGB(),q.getLetterboxColor().getRGB());
            assertEquals(31,q.getTextStyles().get("warning").fontSize);assertEquals(new Color(255,32,48).getRGB(),q.getTextStyles().get("warning").textColor.getRGB());
            AnimationClip loadedClip=q.getAnimationClips().get("walk");assertEquals(2,loadedClip.frames.size());assertEquals("footstep",loadedClip.frames.get(1).event);assertTrue(loadedClip.pingPong);assertEquals(1.25,loadedClip.speed,.0001);
            AnimatorController loadedController=q.getAnimatorControllers().get("hero-controller");assertEquals("Walk",loadedController.defaultState);assertEquals("walk",loadedController.states.get("Walk").clipKey);assertEquals("Rigidbody2D.grounded",loadedController.transitions.getFirst().path);
            PrefabDef loadedPrefab=q.getPrefabs().get("slime");assertNotNull(loadedPrefab.template.component("Animator"));assertEquals("150",loadedPrefab.variantOverrides.get("Health.max"));
            assertEquals(List.of("LSHIFT","X"),q.getInputActions().get("Dash").bindings);assertEquals(Color.RED.getRGB(),q.getPalettes().get("red").colors.get(Color.WHITE.getRGB()));assertEquals(42,q.getParticlePresets().get("sparks").rate,.001);assertEquals(9,q.getParticlePresets().get("sparks").burstCount);
            TileDef loadedGround=q.getTiles().get(0);assertTrue(loadedGround.solid());assertTrue(loadedGround.oneWay);assertEquals(.25,loadedGround.friction,.001);assertEquals(3,loadedGround.damage,.001);assertEquals("walk",loadedGround.animationClip);
            Level loadedLevel=q.getLevels().get("level-1");assertEquals("hero",loadedLevel.cameraTarget);assertEquals(1.5,loadedLevel.cameraZoom,.001);assertFalse(loadedLevel.cameraPixelSnap);assertEquals(.65,loadedLevel.baseLayer().opacity,.001);assertEquals(.4,loadedLevel.baseLayer().parallaxX,.001);assertTrue(loadedLevel.baseLayer().ySort);assertEquals("slime",loadedLevel.entity("slime-1").prefabKey);assertEquals("22",loadedLevel.entity("slime-1").prefabOverrides.get("Health.current"));
        }finally{Files.deleteIfExists(file);}
    }

    @Test void twoGameScriptSupportsActionsPrefabsAnimationEventsAndStyledText(){
        String source="""
                on update
                  ifAction MoveLeft
                    log "left"
                  end
                  ifActionPressed Jump
                    log "jump"
                  end
                  spawnPrefab slime 2 3
                  playAnimation Walk
                  queueAnimation Attack
                  showText free screen "PELIGRO" 2 style warning color #FF2030
                  stopAnimation
                end

                on animationEvent footstep
                  log "step"
                end
                """;
        ScriptProgram program=ScriptProgram.compile(source);assertTrue(program.validation().valid(),String.join("\n",program.validation().errors()));
        Recorder c=new Recorder();program.fire(ScriptProgram.Event.UPDATE,c);
        assertEquals(List.of("left","jump"),c.logs);assertEquals("slime",c.prefab);assertEquals(2,c.x);assertEquals(3,c.y);assertEquals("Walk",c.played);assertEquals("Attack",c.queued);assertTrue(c.stopped);assertNotNull(c.text);assertEquals("warning",c.text.style());assertEquals("#FF2030",c.text.color());
        program.fireEvent("animation:footstep",c);assertEquals("step",c.logs.getLast());
    }

    private static final class Recorder implements ScriptProgram.Context {
        final List<String>logs=new ArrayList<>();String prefab,played,queued;Double x,y;boolean stopped;ScriptProgram.TextRequest text;
        @Override public boolean keyDown(String key){return false;}@Override public boolean keyPressed(String key){return false;}@Override public boolean actionDown(String action){return action.equals("MoveLeft");}@Override public boolean actionPressed(String action){return action.equals("Jump");}
        @Override public void move(double dx,double dy){}@Override public void setVelocity(double vx,double vy){}@Override public void teleport(double x,double y){}@Override public void bounce(){}@Override public void destroy(){}@Override public void loadScene(String id){}@Override public void setSprite(String assetKey){}@Override public void createEntity(String template,Double x,Double y){}@Override public void schedule(double seconds,Runnable action){}@Override public void log(String message){logs.add(message);}@Override public void setVariable(String name,String value){}@Override public String variable(String name){return "0";}@Override public void setProperty(String name,String value){}
        @Override public void createPrefab(String prefab,Double x,Double y){this.prefab=prefab;this.x=x;this.y=y;}@Override public void playAnimation(String clip){played=clip;}@Override public void queueAnimation(String clip){queued=clip;}@Override public void stopAnimation(){stopped=true;}@Override public void showText(ScriptProgram.TextRequest request){text=request;}
    }
}
