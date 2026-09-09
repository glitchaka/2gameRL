package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.FontAsset;
import com.buttclapdev.twogamerl.model.GameProject.TextStyle;
import com.buttclapdev.twogamerl.script.ScriptProgram.TextRequest;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Function;

final class TextOverlayLayer extends Pane {
    private final GameProject project;
    private final List<Entry>entries=new ArrayList<>();
    private final Function<String,Point2D>anchorResolver;
    private final Map<String,Image>skinCache=new HashMap<>();

    TextOverlayLayer(GameProject project,Function<String,Point2D>anchorResolver){this.project=project;this.anchorResolver=anchorResolver;setPickOnBounds(false);}

    void show(TextRequest raw,double now){
        if(raw==null)return;
        String type=raw.type()==null?"free":raw.type().toLowerCase(Locale.ROOT);
        TextStyle style=resolveStyle(type,raw.style());
        if(raw.color()!=null&&!raw.color().isBlank()){style=style.copy();try{style.textColor=awt(Color.web(raw.color()));}catch(Exception ignored){}}
        double duration=raw.duration();
        if(type.equals("bubble"))duration=Math.max(.25,Math.min(10,duration<=0?3:duration));
        else if(type.equals("free"))duration=Math.max(.25,duration<=0?3:duration);
        else if(type.equals("novel")&&"time".equalsIgnoreCase(raw.dismiss()))duration=Math.max(.25,duration);
        Node node=switch(type){case"bubble"->bubble(raw.text(),style);case"novel"->novel(raw.anchor(),raw.text(),style);default->free(raw.text(),style);};
        Entry e=new Entry(type,raw.anchor()==null?"screen":raw.anchor(),raw.dismiss()==null?"time":raw.dismiss().toLowerCase(Locale.ROOT),raw.key()==null?"":raw.key(),duration,now,node,style);
        entries.add(e);getChildren().add(node);layoutEntry(e);node.toFront();
    }

    void update(double now){
        for(Iterator<Entry>it=entries.iterator();it.hasNext();){
            Entry e=it.next();layoutEntry(e);double age=now-e.started;
            if(!e.dismissed&&e.dismiss.equals("time")&&e.duration>0&&age>=e.duration)e.dismiss(now);
            if(!e.dismissed&&(e.type.equals("free")||e.type.equals("bubble"))&&e.duration>0){double remaining=e.duration-age,fadeIn=e.style.fadeIn<=0?1:Math.min(1,age/e.style.fadeIn),fadeOut=e.style.fadeOut<=0?1:Math.min(1,Math.max(0,remaining)/e.style.fadeOut);e.node.setOpacity(Math.min(fadeIn,fadeOut));}
            else if(e.dismissed){double fade=Math.max(.01,e.style.fadeOut),p=1-(now-e.dismissedAt)/fade;e.node.setOpacity(Math.max(0,p));if(p<=0){getChildren().remove(e.node);it.remove();}}
        }
    }

    void click(double now){for(Entry e:entries)if(e.type.equals("novel")&&e.dismiss.equals("click")&&!e.dismissed)e.dismiss(now);}
    void key(KeyCode code,double now){for(Entry e:entries)if(e.type.equals("novel")&&e.dismiss.equals("key")&&!e.dismissed&&(e.key.isBlank()||code.name().equalsIgnoreCase(e.key)))e.dismiss(now);}
    void clear(){entries.clear();getChildren().clear();}
    int count(){return entries.size();}

    private TextStyle resolveStyle(String type,String requested){
        TextStyle style=requested==null||requested.isBlank()?null:project.getTextStyles().get(requested);
        if(style==null)style=project.getTextStyles().get("default-"+type);
        if(style!=null)return style;
        TextStyle fallback=new TextStyle("fallback",type);fallback.textColor=java.awt.Color.WHITE;fallback.fontSize=18;fallback.fadeIn=.15;fallback.fadeOut=.35;return fallback;
    }

    private Node free(String text,TextStyle s){Label l=label(text,s,s.fontSize,s.textColor,s.bold);l.setWrapText(true);l.setMaxWidth(Math.max(1,s.maxWidth));applyShadow(l,s);l.setMouseTransparent(true);return l;}

    private Node bubble(String text,TextStyle s){
        Label l=label(text,s,s.fontSize,s.textColor,s.bold);l.setWrapText(true);l.setMaxWidth(Math.max(1,s.maxWidth));applyShadow(l,s);
        StackPane box=new StackPane(l);box.setPadding(new Insets(s.paddingY,s.paddingX,s.paddingY,s.paddingX));applyBoxStyle(box,s);
        VBox v=new VBox(-1);v.setAlignment(Pos.CENTER);v.getChildren().add(box);
        if(s.tail){Polygon tail=new Polygon(0.0,0.0,18.0,0.0,9.0,12.0);tail.setFill(fx(s.backgroundColor));v.getChildren().add(tail);}
        v.setMouseTransparent(true);return v;
    }

    private Node novel(String speaker,String text,TextStyle s){
        Label name=label(speaker==null||speaker.equalsIgnoreCase("screen")?"":speaker,s,s.speakerFontSize,s.speakerColor,s.boldSpeaker);
        Label body=label(text,s,s.fontSize,s.textColor,s.bold);body.setWrapText(true);body.setMaxWidth(Math.max(1,s.maxWidth));
        VBox box=new VBox(7,name,body);box.setPadding(new Insets(s.paddingY,s.paddingX,s.paddingY,s.paddingX));applyBoxStyle(box,s);box.setMouseTransparent(true);return box;
    }

    private Label label(String text,TextStyle s,int size,java.awt.Color color,boolean bold){
        Label l=new Label(text==null?"":text);l.setTextFill(fx(color));l.setFont(font(s.fontKey,size,bold));
        try{l.setAlignment(Pos.valueOf(s.alignment==null?"CENTER":s.alignment.toUpperCase(Locale.ROOT)));}catch(Exception ignored){l.setAlignment(Pos.CENTER);}
        return l;
    }

    private Font font(String key,double size,boolean bold){
        Font base=null;if(key!=null&&!key.isBlank()){FontAsset f=project.getFonts().get(key);if(f!=null&&f.data!=null)try{base=Font.loadFont(new ByteArrayInputStream(f.data),Math.max(6,size));}catch(Exception ignored){}}
        if(base==null)return Font.font("System",bold?FontWeight.BOLD:FontWeight.NORMAL,Math.max(6,size));
        return bold?Font.font(base.getFamily(),FontWeight.BOLD,Math.max(6,size)):base;
    }

    private static void applyShadow(Node n,TextStyle s){if(!s.shadow)return;DropShadow d=new DropShadow();d.setColor(fx(s.shadowColor));d.setRadius(6);d.setOffsetY(2);n.setEffect(d);}
    private void applyBoxStyle(Region r,TextStyle s){
        Asset skin=s.skinAssetKey==null||s.skinAssetKey.isBlank()?null:project.getAssets().get(s.skinAssetKey);Image image=skin==null?null:skinImage(skin);
        if(skin!=null&&image!=null&&skin.isNineSlice()){
            BorderWidths widths=new BorderWidths(skin.sliceTop,skin.sliceRight,skin.sliceBottom,skin.sliceLeft);
            r.setBackground(Background.EMPTY);r.setBorder(new Border(new BorderImage(image,widths,Insets.EMPTY,widths,true,BorderRepeat.STRETCH,BorderRepeat.STRETCH)));return;
        }
        r.setBorder(Border.EMPTY);String bg=css(s.backgroundColor),border=css(s.borderColor);r.setStyle(String.format(Locale.ROOT,"-fx-background-color:%s;-fx-background-radius:%.2f;-fx-border-color:%s;-fx-border-width:%.2f;-fx-border-radius:%.2f;",bg,s.radius,border,s.borderWidth,s.radius));
    }
    private Image skinImage(Asset asset){Image cached=skinCache.get(asset.key);if(cached!=null)return cached;try{Asset physical=asset.isRegion()?project.getAssets().get(asset.sourceAssetKey):asset;if(physical==null||physical.data==null)return null;Image source=new Image(new ByteArrayInputStream(physical.data));Image out=source;if(asset.isRegion()&&source.getPixelReader()!=null)out=new WritableImage(source.getPixelReader(),asset.regionX,asset.regionY,asset.regionWidth,asset.regionHeight);skinCache.put(asset.key,out);return out;}catch(Exception ignored){return null;}}
    private static String css(java.awt.Color c){return String.format(Locale.ROOT,"rgba(%d,%d,%d,%.4f)",c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0);}
    private static Color fx(java.awt.Color c){return Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0);}
    private static java.awt.Color awt(Color c){return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}

    private void layoutEntry(Entry e){double w=getWidth(),h=getHeight();if(e.type.equals("novel")){double ph=Math.max(60,e.style.panelHeight);if(e.node instanceof Region r){r.resizeRelocate(0,Math.max(0,h-ph),w,ph);r.setPrefWidth(w);r.setMaxWidth(w);}else e.node.relocate(0,Math.max(0,h-ph));return;}Point2D p=e.anchor.equalsIgnoreCase("screen")?new Point2D(w/2,h*.22):anchorResolver.apply(e.anchor);if(p==null)p=new Point2D(w/2,h*.22);e.node.applyCss();double nw,nh;if(e.node instanceof Region r){nw=Math.max(1,r.prefWidth(-1));nh=Math.max(1,r.prefHeight(nw));}else{nw=Math.max(1,e.node.getBoundsInLocal().getWidth());nh=Math.max(1,e.node.getBoundsInLocal().getHeight());}double x=p.getX()-nw/2,y=e.type.equals("bubble")?p.getY()-nh-12:p.getY()-nh/2;x=Math.max(8,Math.min(Math.max(8,w-nw-8),x));y=Math.max(8,Math.min(Math.max(8,h-nh-8),y));e.node.relocate(Math.rint(x),Math.rint(y));}

    private static final class Entry{final String type,anchor,dismiss,key;final double duration,started;final Node node;final TextStyle style;boolean dismissed;double dismissedAt;Entry(String type,String anchor,String dismiss,String key,double duration,double started,Node node,TextStyle style){this.type=type;this.anchor=anchor;this.dismiss=dismiss;this.key=key;this.duration=duration;this.started=started;this.node=node;this.style=style;}void dismiss(double now){dismissed=true;dismissedAt=now;}}
}
