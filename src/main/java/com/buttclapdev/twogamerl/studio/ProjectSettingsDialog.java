package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;

final class ProjectSettingsDialog {
    private ProjectSettingsDialog(){}

    static void show(StudioApp app){
        GameProject p=app.project();Stage stage=new Stage(StageStyle.UNDECORATED);stage.initOwner(app.owner());stage.initModality(Modality.APPLICATION_MODAL);stage.setTitle("Configuración del proyecto · 2gameRL");
        BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame");root.setTop(titleBar(stage));
        TabPane tabs=new TabPane();tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);tabs.getTabs().addAll(new Tab("Pantalla y render",displayTab(app,p)),new Tab("Input Map",inputTab(app,p)));root.setCenter(tabs);
        Button close=new Button("Cerrar");close.getStyleClass().add("primary-button");close.setOnAction(e->stage.close());HBox foot=new HBox(close);foot.setAlignment(Pos.CENTER_RIGHT);foot.setPadding(new Insets(10,14,12,14));root.setBottom(foot);
        Scene scene=new Scene(root,860,650);var css=ProjectSettingsDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);stage.showAndWait();
    }

    private static Pane displayTab(StudioApp app,GameProject p){
        VBox box=new VBox(12);box.setPadding(new Insets(18));
        ComboBox<String>preset=new ComboBox<>(FXCollections.observableArrayList("256 × 144","320 × 180","384 × 216","426 × 240","640 × 360","Personalizada"));preset.setValue("Personalizada");
        Spinner<Integer>w=new Spinner<>(160,7680,p.getLogicalWidth()),h=new Spinner<>(90,4320,p.getLogicalHeight());w.setEditable(true);h.setEditable(true);
        ComboBox<ScreenMode>screen=new ComboBox<>(FXCollections.observableArrayList(ScreenMode.values()));screen.setValue(p.getScreenMode());
        ComboBox<ScaleMode>scale=new ComboBox<>(FXCollections.observableArrayList(ScaleMode.values()));scale.setValue(p.getScaleMode());
        ComboBox<FilterMode>filter=new ComboBox<>(FXCollections.observableArrayList(FilterMode.values()));filter.setValue(p.getDefaultFilter());
        CheckBox aspect=new CheckBox("Mantener proporción"),integer=new CheckBox("Escalado entero cuando sea posible"),uiSmooth=new CheckBox("UI y tipografía pueden usar suavizado independiente");aspect.setSelected(p.isKeepAspect());integer.setSelected(p.isIntegerScale());uiSmooth.setSelected(p.isUiSmooth());
        java.awt.Color lc=p.getLetterboxColor();ColorPicker letterbox=new ColorPicker(Color.rgb(lc.getRed(),lc.getGreen(),lc.getBlue(),lc.getAlpha()/255.0));
        Runnable apply=()->{p.setLogicalWidth(w.getValue());p.setLogicalHeight(h.getValue());p.setScreenMode(screen.getValue());p.setScaleMode(scale.getValue());p.setDefaultFilter(filter.getValue());p.setKeepAspect(aspect.isSelected());p.setIntegerScale(integer.isSelected());p.setUiSmooth(uiSmooth.isSelected());Color c=letterbox.getValue();p.setLetterboxColor(new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity()));app.changed();};
        preset.setOnAction(e->{String v=preset.getValue();if(v==null||v.startsWith("Personalizada"))return;String[]parts=v.replace(" ","").split("×");w.getValueFactory().setValue(Integer.parseInt(parts[0]));h.getValueFactory().setValue(Integer.parseInt(parts[1]));apply.run();});
        w.valueProperty().addListener((o,a,b)->apply.run());h.valueProperty().addListener((o,a,b)->apply.run());screen.setOnAction(e->apply.run());scale.setOnAction(e->apply.run());filter.setOnAction(e->apply.run());aspect.setOnAction(e->apply.run());integer.setOnAction(e->apply.run());uiSmooth.setOnAction(e->apply.run());letterbox.setOnAction(e->apply.run());
        GridPane form=form();row(form,0,"Preset",preset);HBox resolution=new HBox(8,w,new Label("×"),h);resolution.setAlignment(Pos.CENTER_LEFT);row(form,1,"Resolución lógica",resolution);row(form,2,"Modo inicial",screen);row(form,3,"Escalado",scale);row(form,4,"Filtro por defecto",filter);row(form,5,"Color de bandas",letterbox);
        Label explain=hint("PIXEL_PERFECT usa la resolución lógica y amplía por factores enteros con nearest-neighbor. En una pantalla 1920×1080, un juego 320×180 se muestra a 6×. Si no cabe exacto, queda centrado con letterbox en vez de deformarse.");
        box.getChildren().addAll(title("PANTALLA Y RENDER"),form,aspect,integer,uiSmooth,new Separator(),title("COMPORTAMIENTO DE PRUEBA / EXPORTADO"),hint("F11 alterna pantalla completa y ventana. Probar usa estas mismas reglas de resolución y escalado; el juego exportado conserva la configuración."),explain);return new ScrollPaneFit(box);
    }

    private static Pane inputTab(StudioApp app,GameProject p){
        BorderPane pane=new BorderPane();pane.setPadding(new Insets(14));ListView<InputAction>list=new ListView<>();list.setItems(FXCollections.observableArrayList(p.getInputActions().values()));list.setPrefWidth(260);VBox editor=new VBox(10);editor.setPadding(new Insets(0,0,0,14));
        Runnable rebuild=()->{editor.getChildren().clear();InputAction a=list.getSelectionModel().getSelectedItem();if(a==null){editor.getChildren().add(hint("Selecciona una acción."));return;}TextField name=new TextField(a.name),bindings=new TextField(String.join(", ",a.bindings));Spinner<Double>dead=new Spinner<>(0.0,1.0,a.deadZone,.05);dead.setEditable(true);Runnable save=()->{a.name=name.getText().isBlank()?a.key:name.getText().trim();a.bindings.clear();for(String s:bindings.getText().split("[,;]")){String b=s.trim().toUpperCase(Locale.ROOT);if(!b.isBlank()&&!a.bindings.contains(b))a.bindings.add(b);}a.deadZone=dead.getValue();app.changed();list.refresh();};name.setOnAction(e->save.run());bindings.setOnAction(e->save.run());name.focusedProperty().addListener((o,x,y)->{if(!y)save.run();});bindings.focusedProperty().addListener((o,x,y)->{if(!y)save.run();});dead.valueProperty().addListener((o,x,y)->save.run());GridPane f=form();row(f,0,"ID estable",new Label(a.key));row(f,1,"Nombre",name);row(f,2,"Bindings",bindings);row(f,3,"Dead zone",dead);editor.getChildren().addAll(title("ACCIÓN · "+a.key),f,hint("Bindings de teclado separados por coma. 2GameScript usa ifAction / ifActionPressed, por lo que el gameplay no depende de una tecla concreta."));};
        list.getSelectionModel().selectedItemProperty().addListener((o,a,b)->rebuild.run());
        Button add=new Button("＋ Acción"),remove=new Button("Eliminar");add.setOnAction(e->{TextInputDialog d=new TextInputDialog("Action");d.setHeaderText("Nueva acción de input");d.showAndWait().ifPresent(raw->{String key=unique(raw,p.getInputActions().keySet());InputAction a=new InputAction(key,List.of());p.getInputActions().put(key,a);app.changed();list.getItems().setAll(p.getInputActions().values());list.getSelectionModel().select(a);});});remove.setOnAction(e->{InputAction a=list.getSelectionModel().getSelectedItem();if(a==null)return;p.getInputActions().remove(a.key);app.changed();list.getItems().setAll(p.getInputActions().values());if(!list.getItems().isEmpty())list.getSelectionModel().selectFirst();});VBox left=new VBox(8,title("ACCIONES"),list,new HBox(7,add,remove),hint("Defaults: MoveLeft/Right/Up/Down, Jump, Accept y Cancel."));VBox.setVgrow(list,Priority.ALWAYS);pane.setLeft(left);pane.setCenter(editor);if(!list.getItems().isEmpty())list.getSelectionModel().selectFirst();return pane;
    }

    private static String unique(String raw,Collection<String>used){String base=raw==null?"Action":raw.trim().replaceAll("[^A-Za-z0-9_-]","");if(base.isBlank())base="Action";String key=base;int n=2;while(used.contains(key))key=base+n++;return key;}
    private static HBox titleBar(Stage stage){Label mark=new Label("2RL"),title=new Label("Configuración del proyecto");mark.getStyleClass().add("window-app-mark");title.getStyleClass().add("window-title");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button close=new Button("×");close.getStyleClass().addAll("window-control","window-close");close.setOnAction(e->stage.close());HBox bar=new HBox(9,mark,title,spacer,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");return bar;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(10);g.setVgap(9);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(160);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}private static void row(GridPane g,int r,String label,javafx.scene.Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}private static Label title(String t){Label l=new Label(t);l.getStyleClass().add("panel-title");return l;}private static Label hint(String t){Label l=new Label(t);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static final class ScrollPaneFit extends Pane{private final ScrollPane scroll;ScrollPaneFit(Region content){scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.getStyleClass().add("editor-scroll");getChildren().add(scroll);} @Override protected void layoutChildren(){scroll.resizeRelocate(0,0,getWidth(),getHeight());}}
}
