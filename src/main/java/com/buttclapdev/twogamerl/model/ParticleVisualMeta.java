package com.buttclapdev.twogamerl.model;

import com.buttclapdev.twogamerl.model.GameProject.Asset;

import java.util.Locale;

/** Metadata for particle visuals stored on generated virtual assets. */
public final class ParticleVisualMeta {
    public static final String INTERNAL_TAG="2rl-particle-internal";
    private static final String DIRECTION="particle-direction:";
    private static final String MODE="particle-mode:";
    private static final String SOURCE="particle-source:";
    private static final String SHAPE="particle-shape:";
    private static final String COLOR="particle-color:";
    private ParticleVisualMeta(){}

    public static boolean internal(Asset a){return a!=null&&a.tags.contains(INTERNAL_TAG);}
    public static double direction(Asset a){String v=tag(a,DIRECTION);try{return v.isBlank()?270:Double.parseDouble(v);}catch(Exception e){return 270;}}
    public static String mode(Asset a){String v=tag(a,MODE);return v.isBlank()?"SPRITE":v.toUpperCase(Locale.ROOT);}
    public static String source(Asset a){return tag(a,SOURCE);}
    public static String shape(Asset a){String v=tag(a,SHAPE);return v.isBlank()?"SQUARE":v.toUpperCase(Locale.ROOT);}
    public static String color(Asset a){String v=tag(a,COLOR);return v.isBlank()?"#FFFFFFFF":v;}

    public static void mark(Asset a,double direction,String mode,String source,String shape,String color){
        if(a==null)return;a.tags.removeIf(t->t.equals(INTERNAL_TAG)||t.startsWith(DIRECTION)||t.startsWith(MODE)||t.startsWith(SOURCE)||t.startsWith(SHAPE)||t.startsWith(COLOR));a.tags.add(INTERNAL_TAG);a.tags.add(DIRECTION+normalize(direction));a.tags.add(MODE+(mode==null?"SPRITE":mode.toUpperCase(Locale.ROOT)));if(source!=null&&!source.isBlank())a.tags.add(SOURCE+source);if(shape!=null&&!shape.isBlank())a.tags.add(SHAPE+shape.toUpperCase(Locale.ROOT));if(color!=null&&!color.isBlank())a.tags.add(COLOR+color.toUpperCase(Locale.ROOT));
    }
    private static String tag(Asset a,String prefix){if(a==null)return"";return a.tags.stream().filter(t->t.startsWith(prefix)).map(t->t.substring(prefix.length())).findFirst().orElse("");}
    private static String normalize(double d){double v=((d%360)+360)%360;return Math.abs(v-Math.rint(v))<1e-9?Long.toString(Math.round(v)):String.format(Locale.ROOT,"%.3f",v).replaceAll("0+$","").replaceAll("\\.$","");}
}
