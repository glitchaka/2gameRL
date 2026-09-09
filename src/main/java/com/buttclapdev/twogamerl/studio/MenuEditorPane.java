package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.GameProject.MenuButton;
import com.buttclapdev.twogamerl.runtime.MenuEffects;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.*;

final class MenuEditorPane extends SplitPane {
    private enum Selection { SCREEN,TITLE,BUTTON }
    private enum Edge { N,NE,E,SE,S,SW,W,NW }
    private record TargetOption(String id,String label){@Override public String toString(){return label;}}
    private record TitleStyle(int x,int y,int width,int height,int fontSize,String fontKey,String textStyleKey,java.awt.Color color,String asset,MenuAnimation animation,double speed){}
    private record Geometry(int x,int y,int w,int h){}
    private interface GeometryAccess {Geometry get();void set(int x,int y,int w,int h);int canvasWidth();int canvasHeight();}

    private final StudioApp app;
    private final ComboBox<MenuScreen> picker=new ComboBox<>();
    private final Pane canvas=new Pane();
    private final VBox inspector=new VBox(10);
    private final LinkedHashSet<MenuButton> selectedButtons=new LinkedHashSet<>();
    private Selection selection=Selection.SCREEN;
    private MenuButton selectedButton;
    private MenuScreen screenClipboard;
    private List<MenuButton> buttonClipboard=List.of();
    private TitleStyle titleClipboard;
    private boolean rendering;

    MenuEditorPane(StudioApp app){
        this.app=app;
        getItems().addAll(left(),center(),right());
        setDividerPositions(.18,.75);
        EditorShortcuts.install(this,this::copySelection,this::pasteSelection,this::deleteSelection);
        refresh();
    }

    private Node left(){
        VBox box=new VBox(8,title("PANTALLAS"),picker);box.getStyleClass().add("side-panel");box.setPadding(new Insets(12));box.setPrefWidth(260);
        picker.setMaxWidth(Double.MAX_VALUE);picker.setOnAction(e->{if(rendering)return;selection=Selection.SCREEN;selectedButton=null;selectedButtons.clear();render();rebuildInspector();});
        Button add=new Button("＋ Pantalla"),remove=new Button("Eliminar pantalla"),addButton=new Button("＋ Botón"),font=new Button("Importar fuente…");
        remove.getStyleClass().add("danger-button");add.setOnAction(e->addMenu());remove.setOnAction(e->removeMenu());addButton.setOnAction(e->addButton());font.setOnAction(e->importFont());font.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(new HBox(6,add,remove),new Separator(),title("ELEMENTOS"),addButton,new Separator(),title("FUENTES"),font,
                hint("Doble clic edita el texto en el propio lienzo. Arrastra para mover. Usa los ocho tiradores para redimensionar."),
                new Separator(),hint("Ctrl+A en el lienzo selecciona todos los botones. Ctrl/Shift+Clic permite selección múltiple. Supr elimina la selección."));
        return box;
    }

    private Node center(){
        canvas.getStyleClass().add("menu-canvas");canvas.setFocusTraversable(true);
        canvas.setOnMousePressed(e->{if(isTextInputTarget(e.getTarget()))return;canvas.requestFocus();if(e.getTarget()==canvas){selection=Selection.SCREEN;selectedButton=null;selectedButtons.clear();syncFrameSelection();rebuildInspector();}});
        canvas.addEventFilter(KeyEvent.KEY_PRESSED,this::canvasKeyPressed);
        StackPane shell=new StackPane(canvas);shell.setPadding(new Insets(35));shell.getStyleClass().add("canvas-shell");
        ScrollPane scroll=new ScrollPane(shell);scroll.setFitToWidth(true);scroll.setFitToHeight(true);scroll.setPannable(true);return scroll;
    }

    private Node right(){ScrollPane scroll=new ScrollPane(inspector);scroll.setFitToWidth(true);scroll.getStyleClass().add("editor-scroll");inspector.getStyleClass().add("side-panel");inspector.setPadding(new Insets(12));inspector.setPrefWidth(405);return scroll;}

    private void refresh(){
        rendering=true;try{
            MenuScreen before=picker.getValue();picker.setItems(FXCollections.observableArrayList(app.project().getMenus().values()));
            if(before!=null&&app.project().getMenus().containsKey(before.id))picker.setValue(app.project().getMenus().get(before.id));
            else if(!picker.getItems().isEmpty()){MenuScreen start=app.project().getMenus().get(app.project().getStartMenu());picker.setValue(start==null?picker.getItems().getFirst():start);}
        }finally{rendering=false;}
        selection=Selection.SCREEN;selectedButton=null;selectedButtons.clear();render();rebuildInspector();
    }

    private void render(){
        MenuScreen m=picker.getValue();rendering=true;try{
            canvas.getChildren().clear();if(m==null)return;
            canvas.setPrefSize(m.canvasWidth,m.canvasHeight);canvas.setMinSize(m.canvasWidth,m.canvasHeight);canvas.setMaxSize(m.canvasWidth,m.canvasHeight);
            java.awt.Color bg=m.background;canvas.setStyle(String.format(Locale.ROOT,"-fx-background-color:rgb(%d,%d,%d);",bg.getRed(),bg.getGreen(),bg.getBlue()));
            if(!m.backgroundAssetKey.isBlank()){ImageView iv=assetImageView(m.backgroundAssetKey,m.canvasWidth,m.canvasHeight);if(iv!=null){iv.setMouseTransparent(true);canvas.getChildren().add(iv);}}

            Node titleContent=buildTitleContent(m);VisualFrame titleFrame=new VisualFrame("title",titleContent,titleGeometry(m),selection==Selection.TITLE);
            installTitleInteractions(titleFrame,m);canvas.getChildren().add(titleFrame);

            for(MenuButton def:m.buttons){
                Node content=buildButtonContent(def);VisualFrame frame=new VisualFrame(def,content,buttonGeometry(m,def),selection==Selection.BUTTON&&selectedButtons.contains(def));
                installButtonInteractions(frame,def,m);canvas.getChildren().add(frame);
            }
        }finally{rendering=false;}
    }

    private ImageView assetImageView(String key,double width,double height){
        Image image=app.image(key);Asset asset=app.project().getAssets().get(key);if(image==null||asset==null)return null;
        ImageView iv=new ImageView(image);iv.setSmooth(asset.filterMode==FilterMode.LINEAR);iv.setCache(false);iv.setPreserveRatio(false);iv.setFitWidth(Math.max(1,Math.rint(width)));iv.setFitHeight(Math.max(1,Math.rint(height)));
        if(asset.isRegion())iv.setViewport(new Rectangle2D(asset.regionX,asset.regionY,asset.regionWidth,asset.regionHeight));return iv;
    }
    private void configureImageView(ImageView iv,String key){Image image=app.image(key);Asset asset=app.project().getAssets().get(key);if(iv==null||image==null||asset==null)return;iv.setImage(image);iv.setSmooth(asset.filterMode==FilterMode.LINEAR);iv.setCache(false);iv.setViewport(asset.isRegion()?new Rectangle2D(asset.regionX,asset.regionY,asset.regionWidth,asset.regionHeight):null);}
    private Font projectFont(String key,double size){if(key==null||key.isBlank())return Font.font(Math.max(8,size));FontAsset a=app.project().getFonts().get(key);if(a==null||a.data==null)return Font.font(Math.max(8,size));try{Font f=Font.loadFont(new ByteArrayInputStream(a.data),Math.max(8,size));return f==null?Font.font(Math.max(8,size)):f;}catch(Exception e){return Font.font(Math.max(8,size));}}

    private Node buildTitleContent(MenuScreen m){
        StackPane content=new StackPane();content.setPrefSize(m.titleWidth,m.titleHeight);content.setMinSize(1,1);content.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        if(!m.titleAssetKey.isBlank()){ImageView iv=assetImageView(m.titleAssetKey,m.titleWidth,m.titleHeight);if(iv!=null)content.getChildren().add(iv);}
        TextStyle style=app.project().getTextStyles().get(m.titleTextStyleKey);if(m.title!=null&&!m.title.isBlank()){
            Label label=new Label(m.title);label.setFont(projectFont(style==null?m.titleFontKey:style.fontKey,style==null?m.titleFontSize:style.fontSize));label.setTextFill(fx(style==null?m.titleColor:style.textColor));label.setWrapText(true);label.setAlignment(Pos.CENTER);label.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);label.setMouseTransparent(true);content.getChildren().add(label);
        }
        return MenuEffects.decorate(content,m.titleAnimation,m.titleAnimationSpeed,MenuHoverEffect.NONE);
    }

    private Node buildButtonContent(MenuButton def){
        UISkinAsset skin=app.project().getUiSkins().get(def.uiSkinKey);TextStyle style=app.project().getTextStyles().get(def.textStyleKey);
        String normal=skin!=null&&!skin.normalAssetKey.isBlank()?skin.normalAssetKey:def.assetKey,hover=skin!=null&&!skin.hoverAssetKey.isBlank()?skin.hoverAssetKey:def.hoverAssetKey,pressed=skin==null?"":skin.pressedAssetKey,disabled=skin==null?"":skin.disabledAssetKey;
        StackPane content=new StackPane();content.setPrefSize(def.width,def.height);content.setMinSize(1,1);content.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        String initial=!def.enabled&&!disabled.isBlank()?disabled:normal;ImageView iv=initial.isBlank()?null:assetImageView(initial,def.width,def.height);
        if(iv!=null)content.getChildren().add(iv);else{
            java.awt.Color bg=skin==null?def.backgroundColor:skin.backgroundColor,border=skin==null?new java.awt.Color(75,106,132):skin.borderColor;double radius=skin==null?7:skin.radius,bw=skin==null?1:skin.borderWidth;
            content.setStyle(String.format(Locale.ROOT,"-fx-background-color:rgba(%d,%d,%d,%.3f);-fx-background-radius:%.2f;-fx-border-color:rgba(%d,%d,%d,%.3f);-fx-border-width:%.2f;-fx-border-radius:%.2f;",bg.getRed(),bg.getGreen(),bg.getBlue(),bg.getAlpha()/255.0,radius,border.getRed(),border.getGreen(),border.getBlue(),border.getAlpha()/255.0,bw,radius));
        }
        String fontKey=style==null?def.fontKey:style.fontKey;if(skin!=null&&!skin.fontKey.isBlank())fontKey=skin.fontKey;
        java.awt.Color text=style==null?def.textColor:style.textColor;if(skin!=null)text=def.enabled?skin.textColor:skin.disabledTextColor;
        Label label=new Label(def.text);label.setFont(projectFont(fontKey,style==null?def.fontSize:style.fontSize));label.setTextFill(fx(text));label.setMouseTransparent(true);label.setWrapText(true);label.setAlignment(Pos.CENTER);label.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);content.getChildren().add(label);
        if(iv!=null&&def.enabled){if(!hover.isBlank()){content.setOnMouseEntered(e->configureImageView(iv,hover));content.setOnMouseExited(e->configureImageView(iv,normal));}if(!pressed.isBlank()){content.setOnMousePressed(e->configureImageView(iv,pressed));content.setOnMouseReleased(e->configureImageView(iv,hover.isBlank()?normal:hover));}}
        if(!def.enabled)content.setOpacity(.72);
        return MenuEffects.decorate(content,skin!=null&&skin.animation!=MenuAnimation.NONE?skin.animation:def.animation,def.animationSpeed,def.hoverEffect);
    }

    private GeometryAccess titleGeometry(MenuScreen m){return new GeometryAccess(){public Geometry get(){return new Geometry(m.titleX,m.titleY,m.titleWidth,m.titleHeight);}public void set(int x,int y,int w,int h){m.titleX=x;m.titleY=y;m.titleWidth=w;m.titleHeight=h;}public int canvasWidth(){return m.canvasWidth;}public int canvasHeight(){return m.canvasHeight;}};}
    private GeometryAccess buttonGeometry(MenuScreen m,MenuButton b){return new GeometryAccess(){public Geometry get(){return new Geometry(b.x,b.y,b.width,b.height);}public void set(int x,int y,int w,int h){b.x=x;b.y=y;b.width=w;b.height=h;}public int canvasWidth(){return m.canvasWidth;}public int canvasHeight(){return m.canvasHeight;}};}

    private void installTitleInteractions(VisualFrame frame,MenuScreen m){
        frame.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{if(isTextInputTarget(e.getTarget())||frame.isHandleTarget(e.getTarget()))return;selection=Selection.TITLE;selectedButton=null;selectedButtons.clear();syncFrameSelection();rebuildInspector();frame.beginMove(e);e.consume();});
        frame.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(frame.moving){frame.moveTo(e);e.consume();}});
        frame.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(frame.moving){boolean changed=frame.finishGesture();if(changed)commitVisualGesture();e.consume();}});
        frame.addEventHandler(MouseEvent.MOUSE_CLICKED,e->{if(e.getClickCount()==2&&!frame.isHandleTarget(e.getTarget())&&!isTextInputTarget(e.getTarget())){frame.beginInlineEdit(m.title,value->{m.title=value;app.changed();render();rebuildInspector();});e.consume();}});
    }

    private void installButtonInteractions(VisualFrame frame,MenuButton def,MenuScreen m){
        frame.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{if(isTextInputTarget(e.getTarget())||frame.isHandleTarget(e.getTarget()))return;
            boolean additive=e.isControlDown()||e.isShiftDown();selection=Selection.BUTTON;
            if(additive){if(selectedButtons.contains(def)&&selectedButtons.size()>1)selectedButtons.remove(def);else selectedButtons.add(def);}else{selectedButtons.clear();selectedButtons.add(def);}selectedButton=selectedButtons.contains(def)?def:selectedButtons.stream().findFirst().orElse(null);
            syncFrameSelection();rebuildInspector();frame.beginMove(e);e.consume();});
        frame.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(frame.moving){frame.moveTo(e);e.consume();}});
        frame.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(frame.moving){boolean changed=frame.finishGesture();if(changed)commitVisualGesture();e.consume();}});
        frame.addEventHandler(MouseEvent.MOUSE_CLICKED,e->{if(e.getClickCount()==2&&!frame.isHandleTarget(e.getTarget())&&!isTextInputTarget(e.getTarget())){frame.beginInlineEdit(def.text,value->{def.text=value;app.changed();render();rebuildInspector();});e.consume();}});
    }

    private void commitVisualGesture(){app.changed();render();rebuildInspector();}

    private void syncFrameSelection(){
        for(Node n:canvas.getChildren())if(n instanceof VisualFrame f){boolean selected=f.key.equals("title")?selection==Selection.TITLE:selection==Selection.BUTTON&&f.key instanceof MenuButton b&&selectedButtons.contains(b);f.setSelected(selected);}
    }

    private void canvasKeyPressed(KeyEvent e){
        if(isTextInputTarget(e.getTarget()))return;MenuScreen m=picker.getValue();if(m==null)return;
        if(e.isShortcutDown()&&e.getCode()==KeyCode.A){selectedButtons.clear();selectedButtons.addAll(m.buttons);selection=Selection.BUTTON;selectedButton=selectedButtons.stream().findFirst().orElse(null);syncFrameSelection();rebuildInspector();e.consume();return;}
        if(e.getCode()==KeyCode.DELETE){deleteSelection();e.consume();return;}
        int dx=0,dy=0;if(e.getCode()==KeyCode.LEFT)dx=-1;else if(e.getCode()==KeyCode.RIGHT)dx=1;else if(e.getCode()==KeyCode.UP)dy=-1;else if(e.getCode()==KeyCode.DOWN)dy=1;else return;
        if(e.isShiftDown())resizeSelection(dx,dy);else moveSelection(dx,dy);e.consume();
    }

    private void moveSelection(int dx,int dy){MenuScreen m=picker.getValue();if(m==null)return;boolean changed=false;
        if(selection==Selection.TITLE){m.titleX=clamp(m.titleX+dx,0,m.canvasWidth-m.titleWidth);m.titleY=clamp(m.titleY+dy,0,m.canvasHeight-m.titleHeight);changed=true;}
        else if(selection==Selection.BUTTON&&!selectedButtons.isEmpty()){for(MenuButton b:selectedButtons){b.x=clamp(b.x+dx,0,m.canvasWidth-b.width);b.y=clamp(b.y+dy,0,m.canvasHeight-b.height);}changed=true;}
        if(changed){app.changed();render();rebuildInspector();}
    }

    private void resizeSelection(int dx,int dy){MenuScreen m=picker.getValue();if(m==null)return;boolean changed=false;
        if(selection==Selection.TITLE){m.titleWidth=clamp(m.titleWidth+dx,20,m.canvasWidth-m.titleX);m.titleHeight=clamp(m.titleHeight+dy,18,m.canvasHeight-m.titleY);changed=true;}
        else if(selection==Selection.BUTTON&&!selectedButtons.isEmpty()){for(MenuButton b:selectedButtons){b.width=clamp(b.width+dx,20,m.canvasWidth-b.x);b.height=clamp(b.height+dy,18,m.canvasHeight-b.y);}changed=true;}
        if(changed){app.changed();render();rebuildInspector();}
    }

    private void rebuildInspector(){inspector.getChildren().clear();MenuScreen m=picker.getValue();if(m==null){inspector.getChildren().add(new Label("No hay menú."));return;}switch(selection){case SCREEN->screenInspector(m);case TITLE->titleInspector(m);case BUTTON->buttonInspector(m,selectedButton);}}

    private void screenInspector(MenuScreen m){
        TextField screenName=new TextField(m.title),width=new TextField(Integer.toString(m.canvasWidth)),height=new TextField(Integer.toString(m.canvasHeight));ColorPicker background=pickerColor(m.background);ComboBox<String>backgroundSprite=assetPicker(m.backgroundAssetKey);CheckBox initial=new CheckBox("Pantalla inicial");initial.setSelected(m.id.equals(app.project().getStartMenu()));
        Runnable apply=()->{m.title=screenName.getText();m.background=awt(background.getValue());m.backgroundAssetKey=assetValue(backgroundSprite);m.canvasWidth=clamp(parseInt(width,m.canvasWidth),320,1920);m.canvasHeight=clamp(parseInt(height,m.canvasHeight),240,1080);if(initial.isSelected())app.project().setStartMenu(m.id);app.changed();render();};
        bind(screenName,apply);bind(width,apply);bind(height,apply);background.setOnAction(e->apply.run());backgroundSprite.setOnAction(e->apply.run());initial.setOnAction(e->apply.run());
        GridPane form=form();row(form,0,"Nombre/título",screenName);row(form,1,"Fondo",background);row(form,2,"Sprite fondo",backgroundSprite);row(form,3,"Ancho",width);row(form,4,"Alto",height);inspector.getChildren().addAll(title("PANTALLA · "+m.id),form,initial,hint("El lienzo es WYSIWYG: los elementos se mueven y redimensionan directamente."));
    }

    private void titleInspector(MenuScreen m){
        TextField text=new TextField(m.title),x=num(m.titleX),y=num(m.titleY),w=num(m.titleWidth),h=num(m.titleHeight),fontSize=num(m.titleFontSize),speed=new TextField(format(m.titleAnimationSpeed));
        ComboBox<String>font=fontPicker(m.titleFontKey),textStyle=textStylePicker(m.titleTextStyleKey),sprite=assetPicker(m.titleAssetKey);ColorPicker color=pickerColor(m.titleColor);ComboBox<MenuAnimation>animation=new ComboBox<>(FXCollections.observableArrayList(MenuAnimation.values()));animation.setValue(m.titleAnimation);
        Runnable apply=()->{m.title=text.getText();m.titleX=clamp(parseInt(x,m.titleX),0,m.canvasWidth-1);m.titleY=clamp(parseInt(y,m.titleY),0,m.canvasHeight-1);m.titleWidth=clamp(parseInt(w,m.titleWidth),20,m.canvasWidth-m.titleX);m.titleHeight=clamp(parseInt(h,m.titleHeight),18,m.canvasHeight-m.titleY);m.titleFontSize=Math.max(8,parseInt(fontSize,m.titleFontSize));m.titleFontKey=fontValue(font);m.titleTextStyleKey=textStyleValue(textStyle);m.titleColor=awt(color.getValue());m.titleAssetKey=assetValue(sprite);m.titleAnimation=animation.getValue();m.titleAnimationSpeed=Math.max(.2,parseDouble(speed,m.titleAnimationSpeed));app.changed();render();};
        for(TextField f:new TextField[]{text,x,y,w,h,fontSize,speed})bind(f,apply);font.setOnAction(e->apply.run());textStyle.setOnAction(e->apply.run());color.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());animation.setOnAction(e->apply.run());
        GridPane form=form();row(form,0,"Texto",text);row(form,1,"X",x);row(form,2,"Y",y);row(form,3,"Ancho",w);row(form,4,"Alto",h);row(form,5,"Fuente",font);row(form,6,"Text Style",textStyle);row(form,7,"Tamaño",fontSize);row(form,8,"Color",color);row(form,9,"Sprite/logo",sprite);row(form,10,"Animación",animation);row(form,11,"Velocidad",speed);
        inspector.getChildren().addAll(title("TÍTULO"),form,hint("Doble clic sobre el título para editar el texto ahí mismo. Arrastra cualquier tirador de la selección para cambiar ancho y alto."));
    }

    private void buttonInspector(MenuScreen m,MenuButton b){
        if(b==null){selection=Selection.SCREEN;screenInspector(m);return;}
        TextField text=new TextField(b.text),x=num(b.x),y=num(b.y),w=num(b.width),h=num(b.height),fontSize=num(b.fontSize),speed=new TextField(format(b.animationSpeed));
        ComboBox<String>font=fontPicker(b.fontKey),textStyle=textStylePicker(b.textStyleKey);ComboBox<MenuAction>action=actionPicker(b.action);ComboBox<TargetOption>target=new ComboBox<>();target.setMaxWidth(Double.MAX_VALUE);ComboBox<String>sprite=assetPicker(b.assetKey),hoverSprite=assetPicker(b.hoverAssetKey),uiSkin=new ComboBox<>();uiSkin.getItems().add("(ninguno)");uiSkin.getItems().addAll(app.project().getUiSkins().keySet());uiSkin.setValue(b.uiSkinKey.isBlank()?"(ninguno)":b.uiSkinKey);
        CheckBox enabled=new CheckBox("Habilitado");enabled.setSelected(b.enabled);ColorPicker textColor=pickerColor(b.textColor),bgColor=pickerColor(b.backgroundColor);ComboBox<MenuAnimation>animation=new ComboBox<>(FXCollections.observableArrayList(MenuAnimation.values()));animation.setValue(b.animation);ComboBox<MenuHoverEffect>hover=new ComboBox<>(FXCollections.observableArrayList(MenuHoverEffect.values()));hover.setValue(b.hoverEffect);
        Runnable apply=()->{b.text=text.getText();b.x=clamp(parseInt(x,b.x),0,m.canvasWidth-1);b.y=clamp(parseInt(y,b.y),0,m.canvasHeight-1);b.width=clamp(parseInt(w,b.width),20,m.canvasWidth-b.x);b.height=clamp(parseInt(h,b.height),18,m.canvasHeight-b.y);b.fontSize=Math.max(8,parseInt(fontSize,b.fontSize));b.fontKey=fontValue(font);b.textStyleKey=textStyleValue(textStyle);b.action=action.getValue();TargetOption selected=target.getValue();b.target=selected==null?"":selected.id();b.assetKey=assetValue(sprite);b.hoverAssetKey=assetValue(hoverSprite);b.uiSkinKey="(ninguno)".equals(uiSkin.getValue())?"":Objects.toString(uiSkin.getValue(),"");b.enabled=enabled.isSelected();b.textColor=awt(textColor.getValue());b.backgroundColor=awt(bgColor.getValue());b.animation=animation.getValue();b.hoverEffect=hover.getValue();b.animationSpeed=Math.max(.2,parseDouble(speed,b.animationSpeed));app.changed();render();};
        for(TextField f:new TextField[]{text,x,y,w,h,fontSize,speed})bind(f,apply);font.setOnAction(e->apply.run());textStyle.setOnAction(e->apply.run());action.setOnAction(e->{String old=b.target;b.action=action.getValue();populateTargets(target,b.action,old);apply.run();});target.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());hoverSprite.setOnAction(e->apply.run());uiSkin.setOnAction(e->apply.run());enabled.setOnAction(e->apply.run());textColor.setOnAction(e->apply.run());bgColor.setOnAction(e->apply.run());animation.setOnAction(e->apply.run());hover.setOnAction(e->apply.run());populateTargets(target,b.action,b.target);
        Button delete=new Button(selectedButtons.size()>1?"Eliminar "+selectedButtons.size()+" botones":"Eliminar botón");delete.getStyleClass().add("danger-button");delete.setOnAction(e->deleteSelection());
        GridPane form=form();row(form,0,"Texto",text);row(form,1,"X",x);row(form,2,"Y",y);row(form,3,"Ancho",w);row(form,4,"Alto",h);row(form,5,"Fuente",font);row(form,6,"Text Style",textStyle);row(form,7,"Tamaño",fontSize);row(form,8,"Color texto",textColor);row(form,9,"Color base",bgColor);row(form,10,"Sprite",sprite);row(form,11,"Sprite hover",hoverSprite);row(form,12,"UI Skin",uiSkin);row(form,13,"Estado",enabled);row(form,14,"Acción",action);row(form,15,"Destino",target);row(form,16,"Animación",animation);row(form,17,"Hover",hover);row(form,18,"Velocidad",speed);
        inspector.getChildren().addAll(title(selectedButtons.size()>1?"BOTONES · "+selectedButtons.size()+" seleccionados":"BOTÓN"),form,delete,hint("Doble clic edita el texto. Arrastra para mover. Shift+flechas redimensiona; flechas mueve 1 px."));
    }

    private ComboBox<MenuAction>actionPicker(MenuAction value){ComboBox<MenuAction>box=new ComboBox<>(FXCollections.observableArrayList(MenuAction.values()));box.setValue(value);box.setMaxWidth(Double.MAX_VALUE);box.setConverter(new StringConverter<>(){@Override public String toString(MenuAction a){if(a==null)return"";return switch(a){case START_GAME->"INICIAR JUEGO / ESCENA";case OPEN_MENU->"ABRIR MENÚ";case EXIT->"SALIR";};}@Override public MenuAction fromString(String s){return value;}});return box;}
    private void populateTargets(ComboBox<TargetOption>box,MenuAction action,String current){box.getItems().clear();if(action==MenuAction.EXIT){box.setDisable(true);box.setValue(null);return;}box.setDisable(false);if(action==MenuAction.START_GAME){for(Level level:app.project().getLevels().values())box.getItems().add(new TargetOption(level.id,level.name));}else{for(MenuScreen menu:app.project().getMenus().values())box.getItems().add(new TargetOption(menu.id,menu.title));}TargetOption selected=box.getItems().stream().filter(o->o.id().equals(current)).findFirst().orElse(null);if(selected==null&&!box.getItems().isEmpty())selected=action==MenuAction.START_GAME?box.getItems().stream().filter(o->o.id().equals(app.project().getStartLevel())).findFirst().orElse(box.getItems().getFirst()):box.getItems().getFirst();box.setValue(selected);}

    private void addMenu(){TextInputDialog d=new TextInputDialog("Nueva pantalla");d.setHeaderText("Nombre de la pantalla");d.showAndWait().ifPresent(name->{if(name.isBlank())return;String id=slug(name,app.project().getMenus().keySet());MenuScreen m=new MenuScreen(id,name.trim());app.project().getMenus().put(id,m);app.changed();rendering=true;try{picker.getItems().add(m);picker.setValue(m);}finally{rendering=false;}selection=Selection.SCREEN;selectedButtons.clear();rebuildInspector();render();});}
    private void removeMenu(){MenuScreen m=picker.getValue();if(m==null||app.project().getMenus().size()<=1)return;app.project().getMenus().remove(m.id);if(m.id.equals(app.project().getStartMenu()))app.project().setStartMenu(app.project().getMenus().keySet().iterator().next());for(MenuScreen screen:app.project().getMenus().values())for(MenuButton b:screen.buttons)if(b.action==MenuAction.OPEN_MENU&&b.target.equals(m.id))b.target="";app.changed();refresh();}
    private void addButton(){MenuScreen m=picker.getValue();if(m==null)return;MenuButton b=new MenuButton("Botón",Math.max(0,m.canvasWidth/2-100),Math.max(0,m.canvasHeight/2-24),200,48,MenuAction.START_GAME,app.project().getStartLevel());m.buttons.add(b);selectedButtons.clear();selectedButtons.add(b);selectedButton=b;selection=Selection.BUTTON;app.changed();render();rebuildInspector();}
    private void importFont(){FileChooser fc=new FileChooser();fc.setTitle("Importar fuente");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fuentes TrueType/OpenType","*.ttf","*.otf"));java.io.File f=fc.showOpenDialog(app.owner());if(f==null)return;try{byte[]data=Files.readAllBytes(f.toPath());Font check=Font.loadFont(new ByteArrayInputStream(data),16);if(check==null){app.error("Fuente inválida","JavaFX no pudo cargar la fuente seleccionada.");return;}String key=uniqueFontKey(f.getName());app.project().getFonts().put(key,new FontAsset(key,f.getName(),data));app.changed();app.status("Fuente importada: "+f.getName());rebuildInspector();render();}catch(Exception ex){app.error("No se pudo importar la fuente",ex.getMessage());}}

    private void copySelection(){MenuScreen m=picker.getValue();if(m==null)return;switch(selection){case SCREEN->{screenClipboard=m.copy();app.status("Pantalla copiada: "+m.title);}case BUTTON->{if(!selectedButtons.isEmpty()){buttonClipboard=selectedButtons.stream().map(MenuButton::copy).toList();app.status(buttonClipboard.size()+" botón(es) copiado(s).");}}case TITLE->{titleClipboard=new TitleStyle(m.titleX,m.titleY,m.titleWidth,m.titleHeight,m.titleFontSize,m.titleFontKey,m.titleTextStyleKey,m.titleColor,m.titleAssetKey,m.titleAnimation,m.titleAnimationSpeed);app.status("Estilo del título copiado.");}}}
    private void pasteSelection(){MenuScreen m=picker.getValue();if(m==null)return;switch(selection){case SCREEN->{if(screenClipboard==null)return;String id=slug(screenClipboard.title+" copia",app.project().getMenus().keySet());MenuScreen c=cloneMenu(screenClipboard,id,screenClipboard.title+" copia");app.project().getMenus().put(id,c);app.changed();refresh();rendering=true;try{picker.setValue(c);}finally{rendering=false;}render();}case BUTTON->{if(buttonClipboard.isEmpty())return;selectedButtons.clear();for(MenuButton source:buttonClipboard){MenuButton c=source.copy();c.x=Math.max(0,Math.min(m.canvasWidth-c.width,c.x+16));c.y=Math.max(0,Math.min(m.canvasHeight-c.height,c.y+16));m.buttons.add(c);selectedButtons.add(c);}selectedButton=selectedButtons.iterator().next();selection=Selection.BUTTON;app.changed();render();rebuildInspector();}case TITLE->{if(titleClipboard==null)return;m.titleX=titleClipboard.x;m.titleY=titleClipboard.y;m.titleWidth=titleClipboard.width;m.titleHeight=titleClipboard.height;m.titleFontSize=titleClipboard.fontSize;m.titleFontKey=titleClipboard.fontKey;m.titleTextStyleKey=titleClipboard.textStyleKey;m.titleColor=titleClipboard.color;m.titleAssetKey=titleClipboard.asset;m.titleAnimation=titleClipboard.animation;m.titleAnimationSpeed=titleClipboard.speed;app.changed();render();rebuildInspector();}}}
    private void deleteSelection(){MenuScreen m=picker.getValue();if(m==null)return;switch(selection){case SCREEN->removeMenu();case BUTTON->{if(selectedButtons.isEmpty()&&selectedButton!=null)selectedButtons.add(selectedButton);if(selectedButtons.isEmpty())return;m.buttons.removeAll(selectedButtons);selectedButtons.clear();selectedButton=null;selection=Selection.SCREEN;app.changed();render();rebuildInspector();}case TITLE->{m.title="";m.titleAssetKey="";app.changed();render();rebuildInspector();}}}
    private static MenuScreen cloneMenu(MenuScreen s,String id,String title){MenuScreen c=new MenuScreen(id,title);c.background=s.background;c.titleColor=s.titleColor;c.backgroundAssetKey=s.backgroundAssetKey;c.titleAssetKey=s.titleAssetKey;c.titleFontKey=s.titleFontKey;c.titleTextStyleKey=s.titleTextStyleKey;c.canvasWidth=s.canvasWidth;c.canvasHeight=s.canvasHeight;c.titleX=s.titleX;c.titleY=s.titleY;c.titleWidth=s.titleWidth;c.titleHeight=s.titleHeight;c.titleFontSize=s.titleFontSize;c.titleAnimation=s.titleAnimation;c.titleAnimationSpeed=s.titleAnimationSpeed;for(MenuButton b:s.buttons)c.buttons.add(b.copy());return c;}

    private ComboBox<String>assetPicker(String value){ComboBox<String>c=new ComboBox<>();c.getItems().add("(ninguno)");c.getItems().addAll(app.project().getDrawableAssetKeys());c.setValue(value==null||value.isBlank()?"(ninguno)":value);c.setMaxWidth(Double.MAX_VALUE);return c;}private static String assetValue(ComboBox<String>c){return c.getValue()==null||"(ninguno)".equals(c.getValue())?"":c.getValue();}
    private ComboBox<String>fontPicker(String value){ComboBox<String>c=new ComboBox<>();c.getItems().add("(Sistema)");c.getItems().addAll(app.project().getFonts().keySet());c.setValue(value==null||value.isBlank()?"(Sistema)":value);c.setMaxWidth(Double.MAX_VALUE);return c;}private static String fontValue(ComboBox<String>c){return c.getValue()==null||"(Sistema)".equals(c.getValue())?"":c.getValue();}
    private ComboBox<String>textStylePicker(String value){ComboBox<String>c=new ComboBox<>();c.getItems().add("(ninguno)");c.getItems().addAll(app.project().getTextStyles().keySet());c.setValue(value==null||value.isBlank()?"(ninguno)":value);c.setMaxWidth(Double.MAX_VALUE);return c;}private static String textStyleValue(ComboBox<String>c){return c.getValue()==null||"(ninguno)".equals(c.getValue())?"":c.getValue();}
    private String uniqueFontKey(String base){String clean=base.replaceAll("[^A-Za-z0-9._-]","_");String key=clean;int n=2;while(app.project().getFonts().containsKey(key))key=n+++"_"+clean;return key;}
    private static ColorPicker pickerColor(java.awt.Color c){return new ColorPicker(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0));}private static java.awt.Color awt(Color c){return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}private static Color fx(java.awt.Color c){return Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0);}
    private static void bind(TextField field,Runnable apply){field.setOnAction(e->apply.run());field.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});}
    private static TextField num(int n){return new TextField(Integer.toString(n));}private static int parseInt(TextField f,int def){try{return Integer.parseInt(f.getText().trim());}catch(Exception e){return def;}}private static double parseDouble(TextField f,double def){try{return Double.parseDouble(f.getText().trim());}catch(Exception e){return def;}}private static String format(double n){return String.format(Locale.ROOT,"%.2f",n);}private static int clamp(int n,int min,int max){return Math.max(min,Math.min(Math.max(min,max),n));}
    private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(100);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}private static String slug(String name,Collection<String>used){String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="menu";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}
    private static boolean isTextInputTarget(Object target){if(!(target instanceof Node node))return false;for(Node current=node;current!=null;current=current.getParent())if(current instanceof TextInputControl)return true;return node.getScene()!=null&&node.getScene().getFocusOwner() instanceof TextInputControl;}

    private final class VisualFrame extends StackPane {
        private final Object key;private final Node content;private final GeometryAccess geometry;private final Pane selectionBorder=new Pane();private final EnumMap<Edge,Region>handles=new EnumMap<>(Edge.class);private Geometry gestureStart;private double pressSceneX,pressSceneY;private boolean moving,resizing,gestureChanged;private Edge resizeEdge;
        VisualFrame(Object key,Node content,GeometryAccess geometry,boolean selected){this.key=key;this.content=content;this.geometry=geometry;getChildren().add(content);selectionBorder.setMouseTransparent(true);selectionBorder.setStyle("-fx-border-color:#52d3ff;-fx-border-width:1.5;-fx-background-color:transparent;");getChildren().add(selectionBorder);for(Edge edge:Edge.values()){Region h=new Region();h.setMinSize(9,9);h.setPrefSize(9,9);h.setMaxSize(9,9);h.setStyle("-fx-background-color:#f7fbff;-fx-border-color:#29bfff;-fx-border-width:1;");h.setCursor(cursor(edge));h.setUserData(edge);h.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{beginResize(edge,e);e.consume();});h.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(resizing){resizeTo(e);e.consume();}});h.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(resizing){boolean changed=finishGesture();if(changed)commitVisualGesture();e.consume();}});handles.put(edge,h);getChildren().add(h);}applyGeometry();setSelected(selected);}
        private Cursor cursor(Edge e){return switch(e){case N,S->Cursor.V_RESIZE;case E,W->Cursor.H_RESIZE;case NE,SW->Cursor.NE_RESIZE;case NW,SE->Cursor.NW_RESIZE;};}
        boolean isHandleTarget(Object target){if(!(target instanceof Node n))return false;for(Node p=n;p!=null&&p!=this;p=p.getParent())if(p.getUserData() instanceof Edge)return true;return n.getUserData() instanceof Edge;}
        void setSelected(boolean selected){selectionBorder.setVisible(selected);handles.values().forEach(h->h.setVisible(selected));}
        void applyGeometry(){Geometry g=geometry.get();setLayoutX(g.x);setLayoutY(g.y);setPrefSize(g.w,g.h);setMinSize(g.w,g.h);setMaxSize(g.w,g.h);if(content instanceof Region r){r.setPrefSize(g.w,g.h);r.setMinSize(g.w,g.h);r.setMaxSize(g.w,g.h);}selectionBorder.setPrefSize(g.w,g.h);layoutHandles(g.w,g.h);}
        private void layoutHandles(double w,double h){place(handles.get(Edge.NW),-4,-4);place(handles.get(Edge.N),w/2-4,-4);place(handles.get(Edge.NE),w-5,-4);place(handles.get(Edge.E),w-5,h/2-4);place(handles.get(Edge.SE),w-5,h-5);place(handles.get(Edge.S),w/2-4,h-5);place(handles.get(Edge.SW),-4,h-5);place(handles.get(Edge.W),-4,h/2-4);}
        private void place(Node n,double x,double y){n.setTranslateX(x-getWidth()/2+4.5);n.setTranslateY(y-getHeight()/2+4.5);}
        @Override protected void layoutChildren(){super.layoutChildren();Geometry g=geometry.get();selectionBorder.resizeRelocate(0,0,g.w,g.h);layoutHandles(g.w,g.h);}
        void beginMove(MouseEvent e){moving=true;resizing=false;gestureChanged=false;gestureStart=geometry.get();pressSceneX=e.getSceneX();pressSceneY=e.getSceneY();canvas.requestFocus();}
        void moveTo(MouseEvent e){if(!moving||gestureStart==null)return;int dx=(int)Math.round(e.getSceneX()-pressSceneX),dy=(int)Math.round(e.getSceneY()-pressSceneY);int nx=clamp(gestureStart.x+dx,0,geometry.canvasWidth()-gestureStart.w),ny=clamp(gestureStart.y+dy,0,geometry.canvasHeight()-gestureStart.h);geometry.set(nx,ny,gestureStart.w,gestureStart.h);gestureChanged|=nx!=gestureStart.x||ny!=gestureStart.y;applyGeometry();}
        void beginResize(Edge edge,MouseEvent e){resizing=true;moving=false;gestureChanged=false;resizeEdge=edge;gestureStart=geometry.get();pressSceneX=e.getSceneX();pressSceneY=e.getSceneY();canvas.requestFocus();}
        void resizeTo(MouseEvent e){if(!resizing||gestureStart==null)return;int dx=(int)Math.round(e.getSceneX()-pressSceneX),dy=(int)Math.round(e.getSceneY()-pressSceneY),x=gestureStart.x,y=gestureStart.y,w=gestureStart.w,h=gestureStart.h;boolean west=resizeEdge==Edge.W||resizeEdge==Edge.NW||resizeEdge==Edge.SW,east=resizeEdge==Edge.E||resizeEdge==Edge.NE||resizeEdge==Edge.SE,north=resizeEdge==Edge.N||resizeEdge==Edge.NE||resizeEdge==Edge.NW,south=resizeEdge==Edge.S||resizeEdge==Edge.SE||resizeEdge==Edge.SW;if(west){x+=dx;w-=dx;}if(east)w+=dx;if(north){y+=dy;h-=dy;}if(south)h+=dy;int minW=20,minH=18;if(w<minW){if(west)x-=minW-w;w=minW;}if(h<minH){if(north)y-=minH-h;h=minH;}if(x<0){w+=x;x=0;}if(y<0){h+=y;y=0;}if(x+w>geometry.canvasWidth())w=geometry.canvasWidth()-x;if(y+h>geometry.canvasHeight())h=geometry.canvasHeight()-y;w=Math.max(minW,w);h=Math.max(minH,h);geometry.set(x,y,w,h);gestureChanged|=x!=gestureStart.x||y!=gestureStart.y||w!=gestureStart.w||h!=gestureStart.h;applyGeometry();}
        boolean finishGesture(){boolean changed=gestureChanged;moving=false;resizing=false;gestureStart=null;gestureChanged=false;return changed;}
        void beginInlineEdit(String value,java.util.function.Consumer<String>commit){TextField editor=new TextField(value==null?"":value);editor.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);editor.setStyle("-fx-font-size:16px;-fx-background-color:rgba(12,22,34,.96);-fx-text-fill:white;-fx-border-color:#52d3ff;-fx-border-width:2;");getChildren().add(editor);editor.requestFocus();editor.selectAll();final boolean[]done={false};Runnable save=()->{if(done[0])return;done[0]=true;String next=editor.getText();getChildren().remove(editor);commit.accept(next);};editor.setOnAction(e->save.run());editor.focusedProperty().addListener((o,a,b)->{if(!b)save.run();});editor.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.ESCAPE){done[0]=true;getChildren().remove(editor);canvas.requestFocus();e.consume();}});}
    }
}
