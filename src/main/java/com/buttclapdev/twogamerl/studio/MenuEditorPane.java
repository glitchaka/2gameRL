package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.runtime.MenuEffects;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.util.Collection;
import java.util.Locale;

final class MenuEditorPane extends SplitPane {
    private enum Selection { SCREEN, TITLE, BUTTON }
    private record TargetOption(String id, String label) { @Override public String toString(){ return label; } }

    private final StudioApp app;
    private final ComboBox<MenuScreen> picker = new ComboBox<>();
    private final Pane canvas = new Pane();
    private final VBox inspector = new VBox(10);
    private Selection selection = Selection.SCREEN;
    private com.buttclapdev.twogamerl.model.GameProject.MenuButton selectedButton;

    MenuEditorPane(StudioApp app) {
        this.app = app;
        getItems().addAll(left(), center(), right());
        setDividerPositions(.17, .75);
        refresh();
    }

    private Node left() {
        VBox box = new VBox(8, title("PANTALLAS"), picker);
        box.getStyleClass().add("side-panel"); box.setPadding(new Insets(12)); box.setPrefWidth(245);
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setOnAction(e -> { selection=Selection.SCREEN; selectedButton=null; render(); rebuildInspector(); });
        Button add=new Button("＋ Pantalla"),remove=new Button("Eliminar pantalla"),addButton=new Button("＋ Botón");
        remove.getStyleClass().add("danger-button");
        add.setOnAction(e->addMenu());remove.setOnAction(e->removeMenu());addButton.setOnAction(e->addButton());
        box.getChildren().addAll(new HBox(6,add,remove),new Separator(),title("ELEMENTOS"),addButton,hint("Haz clic en el fondo, título o botón para editarlo. Arrastra título y botones directamente sobre el lienzo."));
        return box;
    }

    private Node center() {
        canvas.getStyleClass().add("menu-canvas");
        canvas.setOnMousePressed(e->{if(e.getTarget()==canvas){selection=Selection.SCREEN;selectedButton=null;rebuildInspector();render();}});
        StackPane shell=new StackPane(canvas);shell.setPadding(new Insets(35));shell.getStyleClass().add("canvas-shell");
        ScrollPane scroll=new ScrollPane(shell);scroll.setFitToWidth(true);scroll.setFitToHeight(true);scroll.setPannable(true);return scroll;
    }

    private Node right() {
        ScrollPane scroll=new ScrollPane(inspector);scroll.setFitToWidth(true);scroll.getStyleClass().add("editor-scroll");
        inspector.getStyleClass().add("side-panel");inspector.setPadding(new Insets(12));inspector.setPrefWidth(390);return scroll;
    }

    private void refresh() {
        MenuScreen before=picker.getValue();
        picker.setItems(FXCollections.observableArrayList(app.project().getMenus().values()));
        if(before!=null&&app.project().getMenus().containsKey(before.id))picker.setValue(app.project().getMenus().get(before.id));
        else if(!picker.getItems().isEmpty()){MenuScreen start=app.project().getMenus().get(app.project().getStartMenu());picker.setValue(start==null?picker.getItems().getFirst():start);}
        selection=Selection.SCREEN;selectedButton=null;render();rebuildInspector();
    }

    private void render() {
        MenuScreen m=picker.getValue();canvas.getChildren().clear();if(m==null)return;
        canvas.setPrefSize(m.canvasWidth,m.canvasHeight);canvas.setMinSize(m.canvasWidth,m.canvasHeight);canvas.setMaxSize(m.canvasWidth,m.canvasHeight);
        java.awt.Color bg=m.background;canvas.setStyle(String.format(Locale.ROOT,"-fx-background-color:rgb(%d,%d,%d);",bg.getRed(),bg.getGreen(),bg.getBlue()));
        if(!m.backgroundAssetKey.isBlank()){Image image=app.image(m.backgroundAssetKey);if(image!=null){ImageView iv=imageView(image,m.canvasWidth,m.canvasHeight);iv.setMouseTransparent(true);canvas.getChildren().add(iv);}}
        Node titleNode=buildTitleNode(m);titleNode.setLayoutX(m.titleX);titleNode.setLayoutY(m.titleY);installTitleDrag(titleNode,m);canvas.getChildren().add(titleNode);
        for(com.buttclapdev.twogamerl.model.GameProject.MenuButton def:m.buttons){Node node=buildButtonNode(def);node.setLayoutX(def.x);node.setLayoutY(def.y);installButtonDrag(node,def,m);canvas.getChildren().add(node);}
    }

    private static ImageView imageView(Image image,double width,double height){ImageView iv=new ImageView(image);iv.setSmooth(false);iv.setCache(false);iv.setPreserveRatio(false);iv.setFitWidth(Math.rint(width));iv.setFitHeight(Math.rint(height));return iv;}

    private Node buildTitleNode(MenuScreen m){StackPane content=new StackPane();content.setPrefSize(m.titleWidth,m.titleHeight);content.setMinSize(m.titleWidth,m.titleHeight);content.setMaxSize(m.titleWidth,m.titleHeight);if(!m.titleAssetKey.isBlank()){Image image=app.image(m.titleAssetKey);if(image!=null)content.getChildren().add(imageView(image,m.titleWidth,m.titleHeight));}if(m.title!=null&&!m.title.isBlank()){Label label=new Label(m.title);label.setFont(Font.font(Math.max(8,m.titleFontSize)));label.setTextFill(fx(m.titleColor));label.setWrapText(true);label.setAlignment(Pos.CENTER);label.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);content.getChildren().add(label);}if(selection==Selection.TITLE)content.setStyle("-fx-border-color:#52d3ff;-fx-border-width:2;");return MenuEffects.decorate(content,m.titleAnimation,m.titleAnimationSpeed,MenuHoverEffect.NONE);}

    private Node buildButtonNode(com.buttclapdev.twogamerl.model.GameProject.MenuButton def){StackPane content=new StackPane();content.setPrefSize(def.width,def.height);content.setMinSize(def.width,def.height);content.setMaxSize(def.width,def.height);Image normal=def.assetKey.isBlank()?null:app.image(def.assetKey),hover=def.hoverAssetKey.isBlank()?null:app.image(def.hoverAssetKey);ImageView iv=normal==null?null:imageView(normal,def.width,def.height);if(iv!=null)content.getChildren().add(iv);else{java.awt.Color c=def.backgroundColor;content.setStyle(String.format(Locale.ROOT,"-fx-background-color:rgba(%d,%d,%d,%.3f);-fx-background-radius:7;-fx-border-color:#4b6a84;-fx-border-radius:7;",c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0));}Label label=new Label(def.text);label.setFont(Font.font(Math.max(8,def.fontSize)));label.setTextFill(fx(def.textColor));label.setMouseTransparent(true);content.getChildren().add(label);if(iv!=null&&hover!=null){content.setOnMouseEntered(e->iv.setImage(hover));content.setOnMouseExited(e->iv.setImage(normal));}if(selection==Selection.BUTTON&&selectedButton==def)content.setStyle(content.getStyle()+";-fx-border-color:#52d3ff;-fx-border-width:2;");return MenuEffects.decorate(content,def.animation,def.animationSpeed,def.hoverEffect);}

    private void installTitleDrag(Node node,MenuScreen m){double[]drag=new double[2];node.setOnMousePressed(e->{selection=Selection.TITLE;selectedButton=null;drag[0]=e.getSceneX()-m.titleX;drag[1]=e.getSceneY()-m.titleY;rebuildInspector();render();e.consume();});node.setOnMouseDragged(e->{m.titleX=clamp((int)Math.round(e.getSceneX()-canvas.localToScene(0,0).getX()-(drag[0]-canvas.localToScene(0,0).getX())),0,m.canvasWidth-m.titleWidth);m.titleY=clamp((int)Math.round(e.getSceneY()-canvas.localToScene(0,0).getY()-(drag[1]-canvas.localToScene(0,0).getY())),0,m.canvasHeight-m.titleHeight);app.changed();render();e.consume();});}
    private void installButtonDrag(Node node,com.buttclapdev.twogamerl.model.GameProject.MenuButton def,MenuScreen m){double[]offset=new double[2];node.setOnMousePressed(e->{selection=Selection.BUTTON;selectedButton=def;offset[0]=e.getX();offset[1]=e.getY();rebuildInspector();render();e.consume();});node.setOnMouseDragged(e->{double bx=canvas.localToScene(0,0).getX(),by=canvas.localToScene(0,0).getY();def.x=clamp((int)Math.round(e.getSceneX()-bx-offset[0]),0,m.canvasWidth-def.width);def.y=clamp((int)Math.round(e.getSceneY()-by-offset[1]),0,m.canvasHeight-def.height);app.changed();render();e.consume();});}

    private void rebuildInspector(){inspector.getChildren().clear();MenuScreen m=picker.getValue();if(m==null){inspector.getChildren().add(new Label("No hay menú."));return;}switch(selection){case SCREEN->screenInspector(m);case TITLE->titleInspector(m);case BUTTON->buttonInspector(m,selectedButton);}}

    private void screenInspector(MenuScreen m){TextField screenName=new TextField(m.title);ColorPicker background=pickerColor(m.background);ComboBox<String>backgroundSprite=assetPicker(m.backgroundAssetKey);TextField width=new TextField(Integer.toString(m.canvasWidth)),height=new TextField(Integer.toString(m.canvasHeight));CheckBox initial=new CheckBox("Pantalla inicial");initial.setSelected(m.id.equals(app.project().getStartMenu()));Runnable apply=()->{m.title=screenName.getText();m.background=awt(background.getValue());m.backgroundAssetKey=assetValue(backgroundSprite);m.canvasWidth=clamp(parseInt(width,m.canvasWidth),320,1920);m.canvasHeight=clamp(parseInt(height,m.canvasHeight),240,1080);if(initial.isSelected())app.project().setStartMenu(m.id);app.changed();render();};bind(screenName,apply);bind(width,apply);bind(height,apply);background.setOnAction(e->apply.run());backgroundSprite.setOnAction(e->apply.run());initial.setOnAction(e->apply.run());GridPane form=form();row(form,0,"Nombre/título",screenName);row(form,1,"Fondo",background);row(form,2,"Sprite fondo",backgroundSprite);row(form,3,"Ancho",width);row(form,4,"Alto",height);inspector.getChildren().addAll(title("PANTALLA · "+m.id),form,initial,hint("El ID se administra internamente. Los botones muestran nombres de escenas o menús; no tienes que escribir IDs."));}

    private void titleInspector(MenuScreen m){TextField text=new TextField(m.title),x=num(m.titleX),y=num(m.titleY),w=num(m.titleWidth),h=num(m.titleHeight),font=num(m.titleFontSize),speed=new TextField(format(m.titleAnimationSpeed));ColorPicker color=pickerColor(m.titleColor);ComboBox<String>sprite=assetPicker(m.titleAssetKey);ComboBox<MenuAnimation>animation=new ComboBox<>(FXCollections.observableArrayList(MenuAnimation.values()));animation.setValue(m.titleAnimation);Runnable apply=()->{m.title=text.getText();m.titleX=parseInt(x,m.titleX);m.titleY=parseInt(y,m.titleY);m.titleWidth=Math.max(1,parseInt(w,m.titleWidth));m.titleHeight=Math.max(1,parseInt(h,m.titleHeight));m.titleFontSize=Math.max(8,parseInt(font,m.titleFontSize));m.titleColor=awt(color.getValue());m.titleAssetKey=assetValue(sprite);m.titleAnimation=animation.getValue();m.titleAnimationSpeed=Math.max(.2,parseDouble(speed,m.titleAnimationSpeed));app.changed();render();};for(TextField f:new TextField[]{text,x,y,w,h,font,speed})bind(f,apply);color.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());animation.setOnAction(e->apply.run());GridPane form=form();row(form,0,"Texto",text);row(form,1,"X",x);row(form,2,"Y",y);row(form,3,"Ancho",w);row(form,4,"Alto",h);row(form,5,"Fuente",font);row(form,6,"Color",color);row(form,7,"Sprite/logo",sprite);row(form,8,"Animación",animation);row(form,9,"Velocidad",speed);inspector.getChildren().addAll(title("TÍTULO"),form,hint("Puedes usar texto, sprite/logo o ambos."));}

    private void buttonInspector(MenuScreen m,com.buttclapdev.twogamerl.model.GameProject.MenuButton b){if(b==null){selection=Selection.SCREEN;screenInspector(m);return;}TextField text=new TextField(b.text),x=num(b.x),y=num(b.y),w=num(b.width),h=num(b.height),font=num(b.fontSize),speed=new TextField(format(b.animationSpeed));ComboBox<MenuAction>action=actionPicker(b.action);ComboBox<TargetOption>target=new ComboBox<>();target.setMaxWidth(Double.MAX_VALUE);ComboBox<String>sprite=assetPicker(b.assetKey),hoverSprite=assetPicker(b.hoverAssetKey);ColorPicker textColor=pickerColor(b.textColor),bgColor=pickerColor(b.backgroundColor);ComboBox<MenuAnimation>animation=new ComboBox<>(FXCollections.observableArrayList(MenuAnimation.values()));animation.setValue(b.animation);ComboBox<MenuHoverEffect>hover=new ComboBox<>(FXCollections.observableArrayList(MenuHoverEffect.values()));hover.setValue(b.hoverEffect);
        Runnable rebuildTarget=()->populateTargets(target,action.getValue(),b.target);
        Runnable apply=()->{b.text=text.getText();b.x=parseInt(x,b.x);b.y=parseInt(y,b.y);b.width=Math.max(1,parseInt(w,b.width));b.height=Math.max(1,parseInt(h,b.height));b.fontSize=Math.max(8,parseInt(font,b.fontSize));b.action=action.getValue();TargetOption selected=target.getValue();b.target=selected==null?"":selected.id();b.assetKey=assetValue(sprite);b.hoverAssetKey=assetValue(hoverSprite);b.textColor=awt(textColor.getValue());b.backgroundColor=awt(bgColor.getValue());b.animation=animation.getValue();b.hoverEffect=hover.getValue();b.animationSpeed=Math.max(.2,parseDouble(speed,b.animationSpeed));app.changed();render();};
        for(TextField f:new TextField[]{text,x,y,w,h,font,speed})bind(f,apply);action.setOnAction(e->{String old=b.target;b.action=action.getValue();populateTargets(target,b.action,old);apply.run();});target.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());hoverSprite.setOnAction(e->apply.run());textColor.setOnAction(e->apply.run());bgColor.setOnAction(e->apply.run());animation.setOnAction(e->apply.run());hover.setOnAction(e->apply.run());rebuildTarget.run();
        Button delete=new Button("Eliminar botón");delete.getStyleClass().add("danger-button");delete.setOnAction(e->{m.buttons.remove(b);selectedButton=null;selection=Selection.SCREEN;app.changed();render();rebuildInspector();});GridPane form=form();row(form,0,"Texto",text);row(form,1,"X",x);row(form,2,"Y",y);row(form,3,"Ancho",w);row(form,4,"Alto",h);row(form,5,"Fuente",font);row(form,6,"Color texto",textColor);row(form,7,"Color base",bgColor);row(form,8,"Sprite",sprite);row(form,9,"Sprite hover",hoverSprite);row(form,10,"Acción",action);row(form,11,"Destino",target);row(form,12,"Animación",animation);row(form,13,"Hover",hover);row(form,14,"Velocidad",speed);inspector.getChildren().addAll(title("BOTÓN"),form,delete,hint("INICIAR JUEGO muestra las escenas creadas. ABRIR MENÚ muestra las pantallas creadas. El ID se guarda internamente."));}

    private ComboBox<MenuAction> actionPicker(MenuAction value){ComboBox<MenuAction>box=new ComboBox<>(FXCollections.observableArrayList(MenuAction.values()));box.setValue(value);box.setMaxWidth(Double.MAX_VALUE);box.setConverter(new StringConverter<>(){@Override public String toString(MenuAction a){if(a==null)return"";return switch(a){case START_GAME->"INICIAR JUEGO / ESCENA";case OPEN_MENU->"ABRIR MENÚ";case EXIT->"SALIR";};}@Override public MenuAction fromString(String s){return value;}});return box;}

    private void populateTargets(ComboBox<TargetOption> box,MenuAction action,String current){box.getItems().clear();if(action==MenuAction.EXIT){box.setDisable(true);box.setValue(null);return;}box.setDisable(false);if(action==MenuAction.START_GAME){for(Level level:app.project().getLevels().values())box.getItems().add(new TargetOption(level.id,level.name+"  ·  Escena"));}else{for(MenuScreen menu:app.project().getMenus().values())box.getItems().add(new TargetOption(menu.id,menu.title+"  ·  Menú"));}TargetOption selected=box.getItems().stream().filter(o->o.id().equals(current)).findFirst().orElse(null);if(selected==null&&!box.getItems().isEmpty()){if(action==MenuAction.START_GAME)selected=box.getItems().stream().filter(o->o.id().equals(app.project().getStartLevel())).findFirst().orElse(box.getItems().getFirst());}box.setValue(selected);}

    private void addMenu(){TextInputDialog d=new TextInputDialog("Nueva pantalla");d.setHeaderText("Nombre de la pantalla");d.showAndWait().ifPresent(name->{if(name.isBlank())return;String id=slug(name,app.project().getMenus().keySet());MenuScreen m=new MenuScreen(id,name.trim());app.project().getMenus().put(id,m);app.changed();picker.getItems().add(m);picker.setValue(m);selection=Selection.SCREEN;rebuildInspector();});}
    private void removeMenu(){MenuScreen m=picker.getValue();if(m==null||app.project().getMenus().size()<=1)return;app.project().getMenus().remove(m.id);if(m.id.equals(app.project().getStartMenu()))app.project().setStartMenu(app.project().getMenus().keySet().iterator().next());for(MenuScreen screen:app.project().getMenus().values())for(com.buttclapdev.twogamerl.model.GameProject.MenuButton b:screen.buttons)if(b.action==MenuAction.OPEN_MENU&&b.target.equals(m.id))b.target="";app.changed();refresh();}
    private void addButton(){MenuScreen m=picker.getValue();if(m==null)return;com.buttclapdev.twogamerl.model.GameProject.MenuButton b=new com.buttclapdev.twogamerl.model.GameProject.MenuButton("Botón",Math.max(0,m.canvasWidth/2-100),Math.max(0,m.canvasHeight/2-24),200,48,MenuAction.START_GAME,app.project().getStartLevel());m.buttons.add(b);selectedButton=b;selection=Selection.BUTTON;app.changed();render();rebuildInspector();}

    private ComboBox<String> assetPicker(String value){ComboBox<String>c=new ComboBox<>();c.getItems().add("(ninguno)");c.getItems().addAll(app.project().getAssets().keySet());c.setValue(value==null||value.isBlank()?"(ninguno)":value);c.setMaxWidth(Double.MAX_VALUE);return c;}
    private static String assetValue(ComboBox<String>c){return c.getValue()==null||"(ninguno)".equals(c.getValue())?"":c.getValue();}
    private static ColorPicker pickerColor(java.awt.Color c){return new ColorPicker(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0));}
    private static java.awt.Color awt(Color c){return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}
    private static Color fx(java.awt.Color c){return Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0);}
    private static void bind(TextField field,Runnable apply){field.setOnAction(e->apply.run());field.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});}
    private static TextField num(int n){return new TextField(Integer.toString(n));}
    private static int parseInt(TextField f,int def){try{return Integer.parseInt(f.getText().trim());}catch(Exception e){return def;}}
    private static double parseDouble(TextField f,double def){try{return Double.parseDouble(f.getText().trim());}catch(Exception e){return def;}}
    private static String format(double n){return String.format(Locale.ROOT,"%.2f",n);}
    private static int clamp(int n,int min,int max){return Math.max(min,Math.min(Math.max(min,max),n));}
    private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(95);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
    private static String slug(String name,Collection<String>used){String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="menu";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}
}
