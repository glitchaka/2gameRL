package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ScriptAutocomplete {
    private record Suggestion(String insert,String label,String group){}
    private static final Pattern NAMED_EVENT=Pattern.compile("(?im)^\\s*on\\s+(?:event|animationEvent)\\s+([A-Za-z_][A-Za-z0-9_-]*)");
    private static final Pattern GLOBAL=Pattern.compile("(?i)\\bglobal[.:]([A-Za-z_][A-Za-z0-9_-]*)");
    private static final Pattern SAVE=Pattern.compile("(?i)\\bsave[.:]([A-Za-z_][A-Za-z0-9_-]*)");
    private static final Pattern LOCAL=Pattern.compile("(?im)^\\s*(?:setVar|addVar|mulVar|divVar|randomVar)\\s+([A-Za-z_][A-Za-z0-9_-]*)");
    private static final List<String> COMMANDS=List.of("log","print","move","velocity","teleport","bounce","destroy","destroyEntity","wait","timer","every","create","spawn","spawnPrefab","playAnimation","queueAnimation","stopAnimation","playSound","stopSound","playMusic","stopMusic","cameraShake","loadScene","showMenu","restartScene","setSprite","setEntitySprite","emit","showText","setVar","addVar","mulVar","divVar","randomVar","setGlobal","addGlobal","set","setEntity","moveEntity","teleportEntity","setEntityVar","addEntityVar","addComponent","removeComponent","setComponent","enableComponent","damage","heal","ifKey","ifPressed","ifAction","ifActionPressed","ifVar","ifGlobal","ifProperty","ifEntity","ifComponent","ifOther","chance","while","repeat","stop","return","else","end");
    private static final List<String> EVENTS=List.of("start","update","click","doubleClick","collision","trigger","destroy","animationStart","animationEnd","animationLoop","event","animationEvent");
    private static final List<String> SELF_PATHS=List.of("id","name","x","y","width","height","enabled","layer","group","renderLayer","physicsLayer","sprite","vx","vy","prefab");
    private static final List<String> SCENE_PATHS=List.of("name","background","backgroundMode","boundary.left","boundary.right","boundary.top","boundary.bottom","camera.target","camera.zoom");
    private static final List<String> LAYER_PATHS=List.of("name","enabled","visible","locked","collision","physicsLayer","renderLayer","opacity","parallaxX","parallaxY","ySort");

    private final TextArea editor;private final StudioApp app;private final Supplier<Collection<String>> localVariables;private final ContextMenu menu=new ContextMenu();
    private ScriptAutocomplete(TextArea editor,StudioApp app,Supplier<Collection<String>>localVariables){this.editor=editor;this.app=app;this.localVariables=localVariables==null?List::of:localVariables;install();}
    static void install(TextArea editor,StudioApp app,Supplier<Collection<String>>localVariables){new ScriptAutocomplete(editor,app,localVariables);}

    private void install(){
        editor.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.SPACE&&e.isControlDown()){refresh(true);e.consume();}else if(e.getCode()==KeyCode.ESCAPE&&menu.isShowing()){menu.hide();e.consume();}});
        editor.addEventFilter(KeyEvent.KEY_TYPED,e->{if(e.isControlDown()||e.isAltDown()||e.isMetaDown())return;String ch=e.getCharacter();if(ch!=null&&!ch.isEmpty()&&(Character.isLetterOrDigit(ch.charAt(0))||"._-".contains(ch)))javafx.application.Platform.runLater(()->refresh(false));});
        editor.focusedProperty().addListener((o,a,b)->{if(!b)menu.hide();});
    }

    private List<Suggestion>suggestions(String lineBefore,String prefix){String lower=lineBefore.stripLeading().toLowerCase(Locale.ROOT),p=prefix.toLowerCase(Locale.ROOT);LinkedHashMap<String,Suggestion>out=new LinkedHashMap<>();
        if(lower.matches("on\\s+[^ ]*$")){for(String e:EVENTS)add(out,e,e,"Evento",p);return limit(out);}
        if(afterCommand(lower,"loadscene")){for(Level l:app.project().getLevels().values())add(out,l.id,l.id+" — "+l.name,"Escena",p);return limit(out);}
        if(afterCommand(lower,"showmenu")){for(MenuScreen m:app.project().getMenus().values())add(out,m.id,m.id+" — "+m.title,"Menú",p);return limit(out);}
        if(afterCommand(lower,"spawnprefab")){for(PrefabDef x:app.project().getPrefabs().values())add(out,x.key,x.key+" — "+x.name,"Prefab",p);return limit(out);}
        if(afterCommand(lower,"playanimation")||afterCommand(lower,"queueanimation")){for(AnimationClip x:app.project().getAnimationClips().values())add(out,x.key,x.key+" — "+x.name,"Animación",p);return limit(out);}
        if(afterCommand(lower,"playsound")||afterCommand(lower,"stopsound")||afterCommand(lower,"playmusic")){for(SoundAsset x:app.project().getSounds().values())add(out,x.key,x.key+" — "+x.name+" · "+x.category,"Audio",p);return limit(out);}
        if(afterCommand(lower,"ifaction")||afterCommand(lower,"ifactionpressed")){for(InputAction x:app.project().getInputActions().values())add(out,x.key,x.key+" — "+x.name,"Input",p);return limit(out);}
        if(afterCommand(lower,"emit")){for(String x:knownNamedEvents())add(out,x,x,"Señal",p);return limit(out);}
        if(afterCommand(lower,"setsprite")){for(Asset x:app.project().getAssets().values())add(out,x.key,x.key,"Asset",p);return limit(out);}
        if(lower.matches(".*particleemitter2d\\.preset\\s*=\\s*[^ ]*$")){for(ParticlePreset x:app.project().getParticlePresets().values())add(out,x.key,x.key,"ParticlePreset",p);return limit(out);}
        if(lower.contains(" style ")||lower.endsWith(" style")){for(TextStyle x:app.project().getTextStyles().values())add(out,x.key,x.key+" — "+x.name,"TextStyle",p);return limit(out);}
        if(prefix.toLowerCase(Locale.ROOT).startsWith("animator.param.")){for(AnimatorController c:app.project().getAnimatorControllers().values())for(String x:c.parameters.keySet())add(out,"Animator.param."+x,"Animator.param."+x,"Parámetro Animator",p);return limit(out);}if(prefix.toLowerCase(Locale.ROOT).startsWith("self.")){for(String x:SELF_PATHS)add(out,"self."+x,"self."+x,"Ruta",p);return limit(out);}
        if(prefix.toLowerCase(Locale.ROOT).startsWith("scene.")){for(String x:SCENE_PATHS)add(out,"scene."+x,"scene."+x,"Ruta escena",p);return limit(out);}
        if(prefix.toLowerCase(Locale.ROOT).startsWith("layer.")){for(String x:LAYER_PATHS)add(out,"layer."+x,"layer."+x,"Ruta capa",p);return limit(out);}
        if(prefix.toLowerCase(Locale.ROOT).startsWith("global.")){for(String x:knownGlobals())add(out,"global."+x,"global."+x,"Global",p);return limit(out);}
        if(prefix.toLowerCase(Locale.ROOT).startsWith("save.")){for(String x:knownSaves())add(out,"save."+x,"save."+x,"Persistente",p);return limit(out);}
        int dot=prefix.indexOf('.');if(dot>0){String head=prefix.substring(0,dot);ComponentDef preset=componentPreset(head);if(preset!=null){for(String prop:preset.properties.keySet())add(out,head+"."+prop,head+"."+prop,"Componente",p);return limit(out);}for(EntityDef e:allEntities())if(e.id.equalsIgnoreCase(head)||e.name.equalsIgnoreCase(head)){for(String x:SELF_PATHS)add(out,head+"."+x,head+"."+x,"Entidad",p);for(ComponentDef c:e.components)for(String prop:c.properties.keySet())add(out,head+"."+c.type+"."+prop,head+"."+c.type+"."+prop,"Entidad",p);return limit(out);}}
        for(String c:COMMANDS)add(out,c,c,"Comando",p);for(String c:GameProject.BUILTIN_COMPONENTS)add(out,c+".",c+" — "+GameProject.componentDescription(c),"Componente",p);for(String v:knownLocals())add(out,v,v,"Variable local",p);for(Level l:app.project().getLevels().values())add(out,l.id,l.id+" — "+l.name,"Escena",p);for(MenuScreen m:app.project().getMenus().values())add(out,m.id,m.id+" — "+m.title,"Menú",p);for(PrefabDef x:app.project().getPrefabs().values())add(out,x.key,x.key+" — "+x.name,"Prefab",p);return limit(out);}

    private void refresh(boolean forced){int caret=editor.getCaretPosition();String text=editor.getText();if(caret<0||caret>text.length())return;int lineStart=text.lastIndexOf('\n',Math.max(0,caret-1))+1;String before=text.substring(lineStart,caret);int tokenStart=caret;while(tokenStart>lineStart&&isTokenChar(text.charAt(tokenStart-1)))tokenStart--;String prefix=text.substring(tokenStart,caret);if(!forced&&prefix.isBlank()){menu.hide();return;}List<Suggestion>suggestions=suggestions(before,prefix);if(suggestions.isEmpty()){menu.hide();return;}show(suggestions,tokenStart,caret);}
    private void show(List<Suggestion>suggestions,int start,int end){menu.getItems().clear();String group="";for(Suggestion s:suggestions){if(!Objects.equals(group,s.group)){if(!menu.getItems().isEmpty())menu.getItems().add(new SeparatorMenuItem());MenuItem heading=new MenuItem(s.group.toUpperCase(Locale.ROOT));heading.setDisable(true);menu.getItems().add(heading);group=s.group;}MenuItem item=new MenuItem(s.label);item.setOnAction(e->{editor.replaceText(start,end,s.insert);editor.positionCaret(start+s.insert.length());menu.hide();editor.requestFocus();});menu.getItems().add(item);}Bounds b=editor.localToScreen(editor.getBoundsInLocal());if(b!=null){if(menu.isShowing())menu.hide();menu.show(editor,b.getMinX()+18,Math.min(b.getMaxY()-24,b.getMinY()+220));}}

    private Collection<String>knownLocals(){LinkedHashSet<String>r=new LinkedHashSet<>(localVariables.get());for(Level l:app.project().getLevels().values()){r.addAll(l.variables.keySet());for(TileLayer layer:l.tileLayers)r.addAll(layer.variables.keySet());for(EntityDef e:l.entities)r.addAll(e.variables.keySet());}for(PrefabDef p:app.project().getPrefabs().values())if(p.template!=null)r.addAll(p.template.variables.keySet());Matcher m=LOCAL.matcher(editor.getText());while(m.find())r.add(m.group(1));return r;}
    private Collection<String>knownGlobals(){return scanScripts(GLOBAL);}private Collection<String>knownSaves(){return scanScripts(SAVE);}private Collection<String>knownNamedEvents(){LinkedHashSet<String>r=new LinkedHashSet<>();for(String s:allScripts()){Matcher m=NAMED_EVENT.matcher(s);while(m.find())r.add(m.group(1));}return r;}
    private Collection<String>scanScripts(Pattern pattern){LinkedHashSet<String>r=new LinkedHashSet<>();for(String s:allScripts()){Matcher m=pattern.matcher(s);while(m.find())r.add(m.group(1));}return r;}
    private Collection<String>allScripts(){ArrayList<String>r=new ArrayList<>();for(Level l:app.project().getLevels().values()){r.add(l.script);for(TileLayer x:l.tileLayers)r.add(x.script);for(EntityDef e:l.entities)r.add(e.script);}for(PrefabDef p:app.project().getPrefabs().values())if(p.template!=null)r.add(p.template.script);return r;}
    private Collection<EntityDef>allEntities(){ArrayList<EntityDef>r=new ArrayList<>();for(Level l:app.project().getLevels().values())r.addAll(l.entities);return r;}
    private static ComponentDef componentPreset(String type){for(String c:GameProject.BUILTIN_COMPONENTS)if(c.equalsIgnoreCase(type))return ComponentDef.preset(c);return null;}
    private static boolean afterCommand(String lower,String command){String t=lower.stripLeading();return t.startsWith(command+" ")&&t.indexOf(' ',command.length()+1)<0;}
    private static boolean isTokenChar(char ch){return Character.isLetterOrDigit(ch)||ch=='_'||ch=='-'||ch=='.'||ch=='#';}
    private static void add(Map<String,Suggestion>out,String insert,String label,String group,String prefix){if(insert==null||insert.isBlank())return;if(!prefix.isBlank()&&!insert.toLowerCase(Locale.ROOT).startsWith(prefix)&&!label.toLowerCase(Locale.ROOT).contains(prefix))return;out.putIfAbsent(group+'\0'+insert.toLowerCase(Locale.ROOT),new Suggestion(insert,label,group));}
    private static List<Suggestion>limit(LinkedHashMap<String,Suggestion>m){return m.values().stream().limit(18).toList();}
}
