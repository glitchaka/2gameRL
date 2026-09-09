package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Project-aware semantic validation layered on top of ScriptProgram syntax validation. */
final class ScriptSemantic222 {
    private record Rule(Pattern pattern,String kind){}
    private static final List<Rule> RULES=List.of(
        rule("(?i)^\\s*loadScene\\s+([^\\s#]+)","scene"),rule("(?i)^\\s*showMenu\\s+([^\\s#]+)","menu"),rule("(?i)^\\s*spawnPrefab\\s+([^\\s#]+)","prefab"),
        rule("(?i)^\\s*(?:playAnimation|queueAnimation)\\s+([^\\s#]+)","animation"),rule("(?i)^\\s*(?:ifAction|ifActionPressed)\\s+([^\\s#]+)","input"),
        rule("(?i)^\\s*(?:setSprite)\\s+([^\\s#]+)","asset"),rule("(?i)^\\s*(?:playSound|stopSound|playMusic)\\s+([^\\s#]+)","sound"),
        rule("(?i)^\\s*ParticleEmitter2D\\.preset\\s*=\\s*([^\\s#]+)","particle")
    );
    private ScriptSemantic222(){}
    private static Rule rule(String regex,String kind){return new Rule(Pattern.compile(regex),kind);}

    static List<String> validate(GameProject project,String source){ArrayList<String>errors=new ArrayList<>();if(project==null||source==null)return errors;String[]lines=source.replace("\r","").split("\n",-1);for(int i=0;i<lines.length;i++){String line=lines[i];for(Rule rule:RULES){Matcher m=rule.pattern.matcher(line);if(!m.find())continue;String ref=unquote(m.group(1));if(dynamic(ref))continue;if(!exists(project,rule.kind,ref))errors.add("Línea "+(i+1)+": "+label(rule.kind)+" '"+ref+"' no existe en el proyecto");}}
        Matcher styles=Pattern.compile("(?i)\\bstyle\\s+([^\\s#]+)").matcher(source);while(styles.find()){String ref=unquote(styles.group(1));if(!dynamic(ref)&&!project.getTextStyles().containsKey(ref))errors.add("TextStyle '"+ref+"' no existe en el proyecto");}
        return errors.stream().distinct().toList();}

    private static boolean exists(GameProject p,String kind,String ref){return switch(kind){case"scene"->p.getLevels().containsKey(ref)||p.getLevels().values().stream().anyMatch(x->x.name.equalsIgnoreCase(ref));case"menu"->p.getMenus().containsKey(ref)||p.getMenus().values().stream().anyMatch(x->x.title.equalsIgnoreCase(ref));case"prefab"->p.getPrefabs().containsKey(ref)||p.getPrefabs().values().stream().anyMatch(x->x.name.equalsIgnoreCase(ref));case"animation"->p.getAnimationClips().containsKey(ref)||p.getAnimationClips().values().stream().anyMatch(x->x.name.equalsIgnoreCase(ref));case"input"->p.action(ref)!=null;case"asset"->p.getAssets().containsKey(ref);case"sound"->p.sound(ref)!=null;case"particle"->p.particlePreset(ref)!=null;default->true;};}
    private static String label(String kind){return switch(kind){case"scene"->"Escena";case"menu"->"Menú";case"prefab"->"Prefab";case"animation"->"AnimationClip";case"input"->"InputAction";case"asset"->"Asset";case"sound"->"Sonido";case"particle"->"ParticlePreset";default->"Recurso";};}
    private static boolean dynamic(String ref){return ref==null||ref.isBlank()||ref.contains("${");}
    private static String unquote(String s){return s!=null&&s.length()>=2&&s.startsWith("\"")&&s.endsWith("\"")?s.substring(1,s.length()-1):Objects.toString(s,"");}
}
