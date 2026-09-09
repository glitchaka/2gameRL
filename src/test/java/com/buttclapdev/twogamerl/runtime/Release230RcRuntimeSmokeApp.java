package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Windows/JavaFX gameplay smoke for 2.3.0-rc1 features. */
public final class Release230RcRuntimeSmokeApp {
    private Release230RcRuntimeSmokeApp(){}
    public static void main(String[]args)throws Exception{CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);ready.await();CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable>failure=new AtomicReference<>();Platform.runLater(()->{try{run();}catch(Throwable t){failure.set(t);}finally{done.countDown();}});done.await();Platform.exit();if(failure.get()!=null){failure.get().printStackTrace();System.exit(1);}}

    private static GameProject base(){GameProject p=GameProject.createDefault();p.getMenus().clear();p.setStartMenu("");p.getLevels().get("level-1").entities.clear();return p;}
    private static AnimationClip clip(GameProject p,String key,int frames){AnimationClip c=new AnimationClip(key,key);c.loop=true;for(int i=0;i<frames;i++)c.frames.add(new AnimationFrame("sample-00-00",.1));p.getAnimationClips().put(key,c);return c;}
    private static ComponentDef animator(String controller,String clip){ComponentDef a=ComponentDef.preset("Animator");a.properties.put("controller",controller);a.properties.put("clip",clip);return a;}

    private static void run(){parameterTransitionAndBlend();directionalSetAndReverseFps();animationHitboxes();cameraSmoothingRegression();}

    private static void parameterTransitionAndBlend(){
        GameProject p=base();Level l=p.getLevels().get("level-1");clip(p,"idle",1);clip(p,"slow",1);clip(p,"fast",1);
        AnimatorController c=new AnimatorController("movement","Movement");c.parameters.put("speed",new AnimatorParameter("speed",AnimatorParameterType.FLOAT,"0"));AnimatorState idle=new AnimatorState("Idle","idle"),walk=new AnimatorState("Walk","slow");walk.blendTree.type=BlendTreeType.ONE_D;walk.blendTree.parameterX="speed";walk.blendTree.children.add(new BlendChild("slow",.5,0));walk.blendTree.children.add(new BlendChild("fast",2,0));c.states.put("Idle",idle);c.states.put("Walk",walk);c.defaultState="Idle";AnimatorTransition tr=new AnimatorTransition("Idle","Walk");tr.path="param.speed";tr.operator=">";tr.value="0.1";c.transitions.add(tr);p.getAnimatorControllers().put(c.key,c);
        EntityDef e=new EntityDef("hero","Hero",2,2);e.components.add(animator(c.key,""));e.script="on start\n  Animator.param.speed = 2\nend\n";l.entities.add(e);GameView v=new GameView(p);v.stop();v.debugAdvance(.02);require("2".equals(v.debugAnimatorParameter("hero","speed")),"Animator.param no conservó valor tipado.");require("fast".equals(v.debugAnimationClip("hero")),"Transición/blend tree no eligió el clip esperado: "+v.debugAnimationClip("hero"));System.out.println("RC_ANIMATOR_PARAM_BLEND_OK");
    }

    private static void directionalSetAndReverseFps(){
        GameProject p=base();Level l=p.getLevels().get("level-1");AnimationClip left=clip(p,"walk-left",2),right=clip(p,"walk-right",2),reverse=clip(p,"reverse",3);reverse.reverse=true;reverse.fps=20;AnimationSet set=new AnimationSet("hero-set","Hero Set");set.directionMode=DirectionMode.FOUR;set.clips.put("Walk|LEFT",left.key);set.clips.put("Walk|RIGHT",right.key);p.getAnimationSets().put(set.key,set);AnimatorController c=new AnimatorController("dir","Dir");c.states.put("Walk",new AnimatorState("Walk",left.key));c.defaultState="Walk";p.getAnimatorControllers().put(c.key,c);
        EntityDef e=new EntityDef("directional","Directional",2,2);ComponentDef a=animator(c.key,"");a.properties.put("animationSet",set.key);a.properties.put("state","Walk");e.components.add(a);e.script="on start\n self.vx = 1\nend\n";l.entities.add(e);EntityDef r=new EntityDef("reverse","Reverse",5,2);r.components.add(animator("",reverse.key));l.entities.add(r);GameView v=new GameView(p);v.stop();v.debugAdvance(.01);require("RIGHT".equals(v.debugAnimatorDirection("directional")),"AnimationSet no resolvió RIGHT.");require("walk-right".equals(v.debugAnimationClip("directional")),"AnimationSet no aplicó clip RIGHT.");require(v.debugAnimationFrame("reverse")==2,"Reverse no comenzó en último frame.");v.debugAdvance(.055);require(v.debugAnimationFrame("reverse")<=1,"FPS global/reverse no avanzó hacia atrás.");System.out.println("RC_DIRECTIONAL_REVERSE_FPS_OK");
    }

    private static void animationHitboxes(){
        GameProject p=base();Level l=p.getLevels().get("level-1");AnimationClip attack=clip(p,"attack",1);attack.frames.getFirst().shapes.add(new FrameShape("blade",FrameShapeRole.HITBOX,.3,.1,.8,.8));AnimationClip body=clip(p,"body",1);body.frames.getFirst().shapes.add(new FrameShape("body",FrameShapeRole.HURTBOX,0,0,1,1));
        EntityDef attacker=new EntityDef("attacker","Attacker",2,2);attacker.width=1;attacker.height=1;attacker.components.add(animator("",attack.key));ComponentDef damage=ComponentDef.preset("DamageOnContact");damage.properties.put("damage","7");attacker.components.add(damage);attacker.script="on event hitbox\n setVar connected yes\nend\n";l.entities.add(attacker);
        EntityDef target=new EntityDef("target","Target",2.5,2);target.width=1;target.height=1;target.components.add(animator("",body.key));ComponentDef health=ComponentDef.preset("Health");health.properties.put("max","20");health.properties.put("current","20");target.components.add(health);target.script="on event hurtbox\n setVar hurt yes\nend\n";l.entities.add(target);
        GameView v=new GameView(p);v.stop();v.debugAdvance(.02);require(Math.abs(v.debugHealth("target")-13)<.001,"Hitbox/Hurtbox no aplicó DamageOnContact una vez: "+v.debugHealth("target"));require("yes".equals(v.debugEntityVariable("attacker","connected")),"Evento hitbox no llegó al atacante.");require("yes".equals(v.debugEntityVariable("target","hurt")),"Evento hurtbox no llegó al objetivo.");v.debugAdvance(.02);require(Math.abs(v.debugHealth("target")-13)<.001,"Hitbox golpeó múltiples veces dentro del mismo frame.");System.out.println("RC_FRAME_HITBOX_HURTBOX_OK");
    }

    private static void cameraSmoothingRegression(){
        GameProject p=base();Level l=p.getLevels().get("level-1");EntityDef target=new EntityDef("target","Target",2,8);target.script="on start\n timer 0.10 self.x = 12\nend\n";target.components.add(ComponentDef.preset("PlayerController"));l.entities.add(target);EntityDef camera=new EntityDef("camera","Camera",0,0);ComponentDef c=ComponentDef.preset("Camera2D");c.properties.put("target","target");c.properties.put("smoothSpeed","2");c.properties.put("zoom","2");c.properties.put("zoomSpeed","3");c.properties.put("minX","1");c.properties.put("maxX","18");camera.components.add(c);l.entities.add(camera);GameView v=new GameView(p);v.stop();v.debugAdvance(.05);double before=v.debugCameraCenter()[0];v.debugAdvance(.25);double after=v.debugCameraCenter()[0];require(after>before,"Camera2D dejó de seguir objetivo bajo bounds/zoomSpeed.");System.out.println("RC_CAMERA_ADVANCED_OK");
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
