package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceRef;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ResourceRename222 {
    private ResourceRename222(){}

    static void particle(GameProject project,String oldName,String newName,String stableKey){
        if(project==null||oldName==null||oldName.equals(newName))return;String replacement=ResourceRef.scriptName(newName);
        for(Level l:project.getLevels().values()){
            l.script=replaceParticle(l.script,oldName,stableKey,replacement);
            for(TileLayer layer:l.tileLayers)layer.script=replaceParticle(layer.script,oldName,stableKey,replacement);
            for(EntityDef e:l.entities)e.script=replaceParticle(e.script,oldName,stableKey,replacement);
        }
        for(PrefabDef p:project.getPrefabs().values())if(p.template!=null)p.template.script=replaceParticle(p.template.script,oldName,stableKey,replacement);
    }

    private static String replaceParticle(String script,String oldName,String key,String replacement){
        if(script==null||script.isBlank())return script;String old=Pattern.quote(oldName),stable=Pattern.quote(key==null?"":key);
        Pattern assignment=Pattern.compile("(?im)(\\bParticleEmitter2D\\.preset\\s*=\\s*)(?:\""+old+"\"|"+old+")(?=\\s*(?:#.*)?$)");Matcher a=assignment.matcher(script);String out=a.replaceAll(Matcher.quoteReplacement("$1"+replacement));
        if(key!=null&&!key.isBlank()&&!key.equalsIgnoreCase(oldName)){Pattern legacy=Pattern.compile("(?im)(\\bsetComponent\\s+\\S+\\s+ParticleEmitter2D\\s+preset\\s+)(?:\""+old+"\"|"+old+")(?=\\s*(?:#.*)?$)");out=legacy.matcher(out).replaceAll(Matcher.quoteReplacement("$1"+replacement));}
        return out;
    }
}
