package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.script.ScriptProgram.TextRequest;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import java.util.*;
import java.util.function.Function;

final class TextOverlayLayer extends Pane {
    private static final double FADE=.35;
    private final List<Entry> entries=new ArrayList<>();
    private final Function<String,Point2D> anchorResolver;

    TextOverlayLayer(Function<String,Point2D> anchorResolver){this.anchorResolver=anchorResolver;setPickOnBounds(false);}

    void show(TextRequest raw,double now){
        if(raw==null)return;String type=raw.type()==null?"free":raw.type().toLowerCase(Locale.ROOT);double duration=raw.duration();if(type.equals("bubble"))duration=Math.max(.25,Math.min(10,duration<=0?3:duration));else if(type.equals("free"))duration=Math.max(.25,duration<=0?3:duration);else if(type.equals("novel")&&raw.dismiss().equalsIgnoreCase("time"))duration=Math.max(.25,duration);
        Node node=switch(type){case"bubble"->bubble(raw.text());case"novel"->novel(raw.anchor(),raw.text());default->free(raw.text());};
        Entry e=new Entry(type,raw.anchor()==null?"screen":raw.anchor(),raw.dismiss()==null?"time":raw.dismiss().toLowerCase(Locale.ROOT),raw.key()==null?"":raw.key(),duration,now,node);entries.add(e);getChildren().add(node);layoutEntry(e);node.toFront();
    }

    void update(double now){for(Iterator<Entry>it=entries.iterator();it.hasNext();){Entry e=it.next();layoutEntry(e);double age=now-e.started;if(!e.dismissed&&e.dismiss.equals("time")&&e.duration>0&&age>=e.duration)e.dismiss(now);if(!e.dismissed&&(e.type.equals("free")||e.type.equals("bubble"))&&e.duration>0){double remaining=e.duration-age;double fadeIn=Math.min(1,age/.15),fadeOut=Math.min(1,Math.max(0,remaining)/FADE);e.node.setOpacity(Math.min(fadeIn,fadeOut));}else if(e.dismissed){double p=1-(now-e.dismissedAt)/FADE;e.node.setOpacity(Math.max(0,p));if(p<=0){getChildren().remove(e.node);it.remove();}}}}
    void click(double now){for(Entry e:entries)if(e.type.equals("novel")&&e.dismiss.equals("click")&&!e.dismissed)e.dismiss(now);}
    void key(KeyCode code,double now){for(Entry e:entries)if(e.type.equals("novel")&&e.dismiss.equals("key")&&!e.dismissed&&(e.key.isBlank()||code.name().equalsIgnoreCase(e.key)))e.dismiss(now);}
    void clear(){entries.clear();getChildren().clear();}

    private Node free(String text){Label l=new Label(text==null?"":text);l.setWrapText(true);l.setMaxWidth(720);l.setTextFill(Color.WHITE);l.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-effect:dropshadow(gaussian,rgba(0,0,0,.85),8,.4,0,2);");l.setMouseTransparent(true);return l;}
    private Node bubble(String text){Label l=new Label(text==null?"":text);l.setWrapText(true);l.setMaxWidth(360);l.setTextFill(Color.web("#111318"));l.setStyle("-fx-font-size:16px;");StackPane box=new StackPane(l);box.setPadding(new Insets(10,14,10,14));box.setStyle("-fx-background-color:rgba(255,255,255,.96);-fx-background-radius:14;-fx-border-color:rgba(20,25,32,.8);-fx-border-width:2;-fx-border-radius:14;");Polygon tail=new Polygon(0.0,0.0,18.0,0.0,9.0,12.0);tail.setFill(Color.rgb(245,245,245));VBox v=new VBox(-1,box,tail);v.setAlignment(Pos.CENTER);v.setMouseTransparent(true);return v;}
    private Node novel(String speaker,String text){Label name=new Label(speaker==null||speaker.equalsIgnoreCase("screen")?"":speaker);name.setTextFill(Color.web("#8fcfff"));name.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");Label body=new Label(text==null?"":text);body.setTextFill(Color.WHITE);body.setWrapText(true);body.setStyle("-fx-font-size:18px;");VBox box=new VBox(7,name,body);box.setPadding(new Insets(18,28,22,28));box.setStyle("-fx-background-color:rgba(8,12,18,.86);-fx-border-color:rgba(120,160,195,.45);-fx-border-width:1 0 0 0;");box.setMouseTransparent(true);return box;}
    private void layoutEntry(Entry e){double w=getWidth(),h=getHeight();if(e.type.equals("novel")){e.node.resizeRelocate(0,Math.max(0,h-190),w,190);if(e.node instanceof Region r){r.setPrefWidth(w);r.setMaxWidth(w);}return;}Point2D p=e.anchor.equalsIgnoreCase("screen")?new Point2D(w/2,h*.22):anchorResolver.apply(e.anchor);if(p==null)p=new Point2D(w/2,h*.22);e.node.applyCss();double nw=Math.max(1,e.node.prefWidth(-1)),nh=Math.max(1,e.node.prefHeight(nw));double x=p.getX()-nw/2,y=e.type.equals("bubble")?p.getY()-nh-12:p.getY()-nh/2;x=Math.max(8,Math.min(Math.max(8,w-nw-8),x));y=Math.max(8,Math.min(Math.max(8,h-nh-8),y));e.node.relocate(Math.rint(x),Math.rint(y));}

    private static final class Entry {final String type,anchor,dismiss,key;final double duration,started;final Node node;boolean dismissed;double dismissedAt;Entry(String type,String anchor,String dismiss,String key,double duration,double started,Node node){this.type=type;this.anchor=anchor;this.dismiss=dismiss;this.key=key;this.duration=duration;this.started=started;this.node=node;}void dismiss(double now){dismissed=true;dismissedAt=now;}}
}
