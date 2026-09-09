package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceRef;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.util.*;

/** 2.2.2 typed component editors layered over the generic inspector. */
final class SceneInspector222 {
    private static final String INSTALLED="2rl.scene.inspector.2.2.2";
    private record EntityOption(String id,String label){@Override public String toString(){return label;}}
    private SceneInspector222(){}

    static void install(SceneEditorPane pane,StudioApp app){
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;
        pane.getProperties().put(INSTALLED,true);
        VBox inspector=field(pane,"inspector",VBox.class);if(inspector==null)return;
        final boolean[]scheduled={false};Runnable schedule=()->{if(scheduled[0])return;scheduled[0]=true;Platform.runLater(()->{scheduled[0]=false;upgrade(pane,app,inspector);});};
        inspector.getChildren().addListener((ListChangeListener<Node>)c->schedule.run());schedule.run();
    }

    private static void upgrade(SceneEditorPane pane,StudioApp app,VBox inspector){
        EntityDef entity=field(pane,"selected",EntityDef.class);if(entity==null)return;
        for(Node node:List.copyOf(inspector.getChildren()))if(node instanceof VBox card){String type=componentType(card);if(type==null)continue;ComponentDef c=entity.component(type);if(c==null)continue;
            switch(type){
                case "Camera2D"->{replace(card,"target",cameraTarget(app,entity,c));replace(card,"follow",null);}
                case "ParticleEmitter2D"->replace(card,"preset",particlePreset(app,c));
                case "Animator"->{replace(card,"controller",animatorController(app,c));replace(card,"clip",animationClip(app,c));}
                case "ScenePortal"->replace(card,"targetScene",scenePicker(app,c,"targetScene"));
                case "SpriteRenderer"->replace(card,"palette",palettePicker(app,c));
                case "PlatformerController"->{replace(card,"leftAction",actionPicker(app,c,"leftAction"));replace(card,"rightAction",actionPicker(app,c,"rightAction"));replace(card,"jumpAction",actionPicker(app,c,"jumpAction"));}
            }
        }
    }

    private static Node cameraTarget(StudioApp app,EntityDef owner,ComponentDef c){
        ComboBox<EntityOption>box=new ComboBox<>();box.setMaxWidth(Double.MAX_VALUE);box.getItems().add(new EntityOption("","(sin objetivo / cámara fija)"));
        Level level=currentLevel(app,owner);if(level!=null)for(EntityDef e:level.entities)box.getItems().add(new EntityOption(e.id,e.name+"  ·  "+e.id));
        String current=c.get("target","");EntityOption selected=box.getItems().stream().filter(o->o.id.equalsIgnoreCase(current)||o.label.equalsIgnoreCase(current)).findFirst().orElse(box.getItems().getFirst());box.setValue(selected);box.setOnAction(e->{EntityOption v=box.getValue();c.properties.put("target",v==null?"":v.id);c.properties.put("follow",Boolean.toString(v!=null&&!v.id.isBlank()));app.changed();});box.setTooltip(new Tooltip("Entidad que seguirá Camera2D. La referencia almacenada es estable aunque cambies el nombre visible."));return box;
    }

    private static Node particlePreset(StudioApp app,ComponentDef c){
        ComboBox<ParticlePreset>box=new ComboBox<>();box.setMaxWidth(Double.MAX_VALUE);box.getItems().addAll(unique(app.project().getParticlePresets().values()));ParticlePreset current=ResourceRef.particlePreset(app.project(),c.get("preset",""));box.setValue(current);box.setPromptText("Selecciona un ParticlePreset…");box.setOnAction(e->{ParticlePreset p=box.getValue();c.properties.put("preset",p==null?"":p.key);app.changed();});box.setTooltip(new Tooltip("El Studio muestra el nombre del preset; el ID técnico permanece interno."));return box;
    }

    private static Node animatorController(StudioApp app,ComponentDef c){ComboBox<AnimatorController>b=new ComboBox<>();b.getItems().addAll(unique(app.project().getAnimatorControllers().values()));b.setValue(ResourceRef.animatorController(app.project(),c.get("controller","")));b.setPromptText("Animator Controller…");b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{AnimatorController v=b.getValue();c.properties.put("controller",v==null?"":v.key);app.changed();});return b;}
    private static Node animationClip(StudioApp app,ComponentDef c){ComboBox<AnimationClip>b=new ComboBox<>();b.getItems().addAll(unique(app.project().getAnimationClips().values()));b.setValue(ResourceRef.animationClip(app.project(),c.get("clip","")));b.setPromptText("AnimationClip…");b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{AnimationClip v=b.getValue();c.properties.put("clip",v==null?"":v.key);app.changed();});return b;}
    private static Node palettePicker(StudioApp app,ComponentDef c){ComboBox<PaletteAsset>b=new ComboBox<>();b.getItems().add(null);b.getItems().addAll(unique(app.project().getPalettes().values()));b.setValue(ResourceRef.palette(app.project(),c.get("palette","")));b.setPromptText("(sin paleta)");b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{PaletteAsset v=b.getValue();c.properties.put("palette",v==null?"":v.key);app.changed();});return b;}
    private static Node scenePicker(StudioApp app,ComponentDef c,String prop){ComboBox<Level>b=new ComboBox<>();b.getItems().addAll(unique(app.project().getLevels().values()));b.setValue(ResourceRef.level(app.project(),c.get(prop,"")));b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{Level v=b.getValue();c.properties.put(prop,v==null?"":v.id);app.changed();});return b;}
    private static Node actionPicker(StudioApp app,ComponentDef c,String prop){ComboBox<InputAction>b=new ComboBox<>();b.getItems().addAll(unique(app.project().getInputActions().values()));b.setValue(ResourceRef.inputAction(app.project(),c.get(prop,"")));b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{InputAction v=b.getValue();c.properties.put(prop,v==null?"":v.key);app.changed();});return b;}

    private static void replace(VBox card,String property,Node replacement){if(replacement==null)return;for(Node row:card.getChildren())if(row instanceof HBox h){Label label=findFirst(h,Label.class);if(label==null||!property.equals(label.getText()))continue;for(int i=0;i<h.getChildren().size();i++){Node old=h.getChildren().get(i);if(old instanceof TextField){replacement.getProperties().put(INSTALLED,true);h.getChildren().set(i,replacement);return;}}}}
    private static String componentType(VBox card){for(Node n:card.getChildren())if(n instanceof HBox h){Label l=findFirst(h,Label.class);if(l!=null&&l.getText()!=null&&l.getText().contains(" · "))return l.getText().substring(0,l.getText().indexOf(" · ")).trim();}return null;}
    private static Level currentLevel(StudioApp app,EntityDef owner){for(Level l:app.project().getLevels().values())if(l.entities.contains(owner))return l;return null;}
    private static <T>List<T>unique(Collection<T>values){Set<T>seen=Collections.newSetFromMap(new IdentityHashMap<>());ArrayList<T>out=new ArrayList<>();for(T v:values)if(v!=null&&seen.add(v))out.add(v);return out;}
    private static <T> T field(Object owner,String name,Class<T>type){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);Object v=f.get(owner);return type.isInstance(v)?type.cast(v):null;}catch(Exception e){return null;}}
    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Parent p)for(Node child:p.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}return null;}
}
