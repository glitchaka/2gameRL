package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.AppIcon;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Consistent undecorated chrome for every Studio-owned secondary Stage. */
final class StudioStageChrome {
    private static final double EDGE=6;
    private StudioStageChrome(){}

    static void install(Stage stage, Scene scene, String title){
        if(stage==null||scene==null)return;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(title==null?"2gameRL Studio":title);
        try{stage.getIcons().setAll(AppIcon.create(256));}catch(Exception ignored){}

        Parent original=scene.getRoot();
        BorderPane frame=new BorderPane();frame.getStyleClass().add("secondary-window-root");
        frame.setTop(titleBar(stage));frame.setCenter(original);scene.setRoot(frame);
        ThemeManager.apply(scene);installResize(stage,scene);
    }

    private static Node titleBar(Stage stage){
        ImageView mark=new ImageView(AppIcon.create(20));mark.setFitWidth(20);mark.setFitHeight(20);mark.setPreserveRatio(true);
        Label title=new Label();title.textProperty().bind(stage.titleProperty());title.getStyleClass().add("secondary-title-text");
        Region spacer=new Region();HBox.setHgrow(spacer, Priority.ALWAYS);
        Button min=control("—","Minimizar"),max=control("▢","Maximizar / restaurar"),close=control("×","Cerrar");close.getStyleClass().add("secondary-window-close");
        min.setOnAction(e->stage.setIconified(true));max.setOnAction(e->stage.setMaximized(!stage.isMaximized()));close.setOnAction(e->stage.close());
        stage.maximizedProperty().addListener((o,a,b)->max.setText(b?"❐":"▢"));
        HBox bar=new HBox(8,mark,title,spacer,min,max,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("secondary-title-bar");
        final double[]drag=new double[2];
        bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||isControl(e.getTarget()))return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});
        bar.setOnMouseDragged(e->{if(e.getButton()!=MouseButton.PRIMARY||stage.isMaximized()||isControl(e.getTarget()))return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});
        bar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!isControl(e.getTarget()))stage.setMaximized(!stage.isMaximized());});
        return bar;
    }

    private static Button control(String text,String tip){Button b=new Button(text);b.getStyleClass().add("secondary-window-control");b.setTooltip(new Tooltip(tip));return b;}
    private static boolean isControl(Object target){if(!(target instanceof Node n))return false;for(Node c=n;c!=null;c=c.getParent())if(c.getStyleClass().contains("secondary-window-control"))return true;return false;}

    private static void installResize(Stage stage,Scene scene){
        final boolean[]resizing={false};final Cursor[]cursor={Cursor.DEFAULT};final double[]origin=new double[6];
        scene.addEventFilter(MouseEvent.MOUSE_MOVED,e->{if(resizing[0]||stage.isMaximized())return;scene.setCursor(edge(e.getSceneX(),e.getSceneY(),scene.getWidth(),scene.getHeight()));});
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{if(e.getButton()!=MouseButton.PRIMARY||stage.isMaximized())return;Cursor c=edge(e.getSceneX(),e.getSceneY(),scene.getWidth(),scene.getHeight());if(c==Cursor.DEFAULT)return;resizing[0]=true;cursor[0]=c;origin[0]=e.getScreenX();origin[1]=e.getScreenY();origin[2]=stage.getX();origin[3]=stage.getY();origin[4]=stage.getWidth();origin[5]=stage.getHeight();e.consume();});
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(!resizing[0])return;double dx=e.getScreenX()-origin[0],dy=e.getScreenY()-origin[1],minW=Math.max(420,stage.getMinWidth()),minH=Math.max(280,stage.getMinHeight());boolean west=cursor[0]==Cursor.W_RESIZE||cursor[0]==Cursor.NW_RESIZE||cursor[0]==Cursor.SW_RESIZE,east=cursor[0]==Cursor.E_RESIZE||cursor[0]==Cursor.NE_RESIZE||cursor[0]==Cursor.SE_RESIZE,north=cursor[0]==Cursor.N_RESIZE||cursor[0]==Cursor.NW_RESIZE||cursor[0]==Cursor.NE_RESIZE,south=cursor[0]==Cursor.S_RESIZE||cursor[0]==Cursor.SW_RESIZE||cursor[0]==Cursor.SE_RESIZE;if(east)stage.setWidth(Math.max(minW,origin[4]+dx));if(south)stage.setHeight(Math.max(minH,origin[5]+dy));if(west){double w=Math.max(minW,origin[4]-dx);stage.setX(origin[2]+origin[4]-w);stage.setWidth(w);}if(north){double h=Math.max(minH,origin[5]-dy);stage.setY(origin[3]+origin[5]-h);stage.setHeight(h);}e.consume();});
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(!resizing[0])return;resizing[0]=false;cursor[0]=Cursor.DEFAULT;e.consume();});
    }
    private static Cursor edge(double x,double y,double w,double h){boolean l=x<=EDGE,r=x>=w-EDGE,t=y<=EDGE,b=y>=h-EDGE;if(t&&l)return Cursor.NW_RESIZE;if(t&&r)return Cursor.NE_RESIZE;if(b&&l)return Cursor.SW_RESIZE;if(b&&r)return Cursor.SE_RESIZE;if(l)return Cursor.W_RESIZE;if(r)return Cursor.E_RESIZE;if(t)return Cursor.N_RESIZE;if(b)return Cursor.S_RESIZE;return Cursor.DEFAULT;}
}
