package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceRef;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight semantic diagnostics for author-facing project references. */
final class ScriptSemantic222 {
    private static final String INSTALLED="2rl.script.semantic.2.2.2";
    private static final Pattern ASSIGN=Pattern.compile("(?i)^\\s*([A-Za-z_][A-Za-z0-9_.-]*)\\s*(?:=|\\+=|-=|\\*=|/=)\\s*(.+?)\\s*$");
    private static final Pattern STYLE=Pattern.compile("(?i)\\bstyle\\s+(\"[^\"]+\"|\\S+)");
    private ScriptSemantic222(){}

    static void install(TextArea area,StudioApp app){if(Boolean.TRUE.equals(area.getProperties().get(INSTALLED)))return;area.getProperties().put(INSTALLED,true);final boolean[]scheduled={false};Runnable schedule=()->{if(scheduled[0])return;scheduled[0]=true;Platform.runLater(()->{scheduled[0]=false;check(area,app);});};area.textProperty().addListener((o,a,b)->schedule.run());schedule.run();}

    private static void check(TextArea area,StudioApp app){List<String>errors=new ArrayList<>();String[]lines=area.getText().split("\\R",-1);for(int i=0;i<lines.length;i++){String raw=stripComment(lines[i]).trim();if(raw.isBlank())continue;String[]t=tokens(raw);if(t.length>1){String cmd=t[0].toLowerCase(Locale.ROOT),ref=t[1];switch(cmd){case"loadscene"->{if(ResourceRef.level(app.project(),ref)==null)errors.add(msg(i,"escena",ref));}case"showmenu"->{if(ResourceRef.menu(app.project(),ref)==null)errors.add(msg(i,"menú",ref));}case"spawnprefab"->{if(ResourceRef.prefab(app.project(),ref)==null)errors.add(msg(i,"Prefab",ref));}case"playanimation","queueanimation"->{if(ResourceRef.animationClip(app.project(),ref)==null&&!knownAnimatorState(app,ref))errors.add(msg(i,"animación/estado",ref));}case"ifaction","ifactionpressed"->{if(ResourceRef.inputAction(app.project(),ref)==null)errors.add(msg(i,"Input Action",ref));}default->{}}}
                Matcher sm=STYLE.matcher(raw);while(sm.find())if(ResourceRef.textStyle(app.project(),sm.group(1))==null)errors.add(msg(i,"TextStyle",sm.group(1)));}
            Matcher a=ASSIGN.matcher(raw);if(a.matches()){String path=a.group(1),value=firstValue(a.group(2));if(dynamic(value))continue;String lower=path.toLowerCase(Locale.ROOT);if(lower.endsWith("particleemitter2d.preset")||lower.equals("particleemitter2d.preset")){if(ResourceRef.particlePreset(app.project(),value)==null)errors.add(msg(i,"ParticlePreset",value));}else if(lower.endsWith("animator.controller")||lower.equals("animator.controller")){if(ResourceRef.animatorController(app.project(),value)==null)errors.add(msg(i,"AnimatorController",value));}else if(lower.endsWith("animator.clip")||lower.equals("animator.clip")){if(ResourceRef.animationClip(app.project(),value)==null)errors.add(msg(i,"AnimationClip",value));}else if(lower.endsWith("spriterenderer.palette")||lower.equals("spriterenderer.palette")){if(!ResourceRef.clean(value).isBlank()&&ResourceRef.palette(app.project(),value)==null)errors.add(msg(i,"paleta",value));}else if(lower.endsWith("sceneportal.targetscene")||lower.equals("sceneportal.targetscene")){if(ResourceRef.level(app.project(),value)==null)errors.add(msg(i,"escena",value));}else if(lower.endsWith("camera2d.target")||lower.equals("camera2d.target")){if(!entityExists(app,ResourceRef.clean(value)))errors.add(msg(i,"entidad objetivo",value));}}
        }
        area.getProperties().put(INSTALLED+".errors",List.copyOf(errors));String base="-fx-font-family:'Consolas','JetBrains Mono',monospace;-fx-font-size:13px;";if(errors.isEmpty()){area.setStyle(base);Tooltip.install(area,new Tooltip("2GameScript · referencias del proyecto válidas. Ctrl+Espacio abre autocompletado."));}else{area.setStyle(base+"-fx-border-color:#d77a48;-fx-border-width:1;");Tooltip.install(area,new Tooltip("2GameScript · "+errors.size()+" referencia(s) no resuelta(s):\n"+String.join("\n",errors.stream().limit(8).toList())));}}

    private static boolean knownAnimatorState(StudioApp app,String ref){String r=ResourceRef.clean(ref);return app.project().getAnimatorControllers().values().stream().anyMatch(c->c.states.keySet().stream().anyMatch(s->s.equalsIgnoreCase(r)));}
    private static boolean entityExists(StudioApp app,String ref){if(ref.isBlank()||ref.equalsIgnoreCase("self")||ref.equalsIgnoreCase("other"))return true;for(Level l:app.project().getLevels().values())if(l.entities.stream().anyMatch(e->e.id.equalsIgnoreCase(ref)||e.name.equalsIgnoreCase(ref)))return true;return false;}
    private static String msg(int line,String type,String ref){return"L"+(line+1)+": no existe "+type+" «"+ResourceRef.clean(ref)+"»";}
    private static boolean dynamic(String value){String v=value.trim();return v.contains("${")||v.startsWith("global.")||v.startsWith("save.");}
    private static String firstValue(String raw){String s=raw.trim();if(s.startsWith("\"")){int e=s.indexOf('"',1);return e>0?s.substring(0,e+1):s;}int sp=s.indexOf(' ');return sp<0?s:s.substring(0,sp);}
    private static String[]tokens(String raw){ArrayList<String>out=new ArrayList<>();Matcher m=Pattern.compile("\"[^\"]*\"|\\S+").matcher(raw);while(m.find())out.add(m.group());return out.toArray(String[]::new);}
    private static String stripComment(String line){boolean quote=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='\"')quote=!quote;if(c=='#'&&!quote){String tail=line.substring(i);if(tail.matches("#[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?(?:\\s.*)?"))continue;return line.substring(0,i);}}return line;}
}
