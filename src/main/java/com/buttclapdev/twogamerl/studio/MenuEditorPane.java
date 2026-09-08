package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.MenuAction;
import com.buttclapdev.twogamerl.model.GameProject.MenuButton;
import com.buttclapdev.twogamerl.model.GameProject.MenuScreen;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Collection;
import java.util.Locale;

final class MenuEditorPane extends SplitPane {
    private final StudioApp app;
    private final ComboBox<MenuScreen> picker = new ComboBox<>();
    private final Pane canvas = new Pane();
    private final VBox inspector = new VBox(10);
    private MenuButton selected;

    MenuEditorPane(StudioApp app) {
        this.app=app;getItems().addAll(left(),center(),right());setDividerPositions(.16,.78);refresh();
    }
    private Node left(){VBox box=new VBox(8,title("PANTALLAS"),picker);box.getStyleClass().add("side-panel");box.setPadding(new Insets(12));box.setPrefWidth(230);picker.setMaxWidth(Double.MAX_VALUE);picker.setOnAction(e->render());Button add=new Button("＋ Pantalla"),addButton=new Button("＋ Botón");add.setOnAction(e->addMenu());addButton.setOnAction(e->addButton());box.getChildren().addAll(add,addButton);return box;}
    private Node center(){canvas.setPrefSize(640,480);canvas.setMinSize(640,480);canvas.setMaxSize(640,480);canvas.getStyleClass().add("menu-canvas");StackPane shell=new StackPane(canvas);shell.setPadding(new Insets(35));shell.getStyleClass().add("canvas-shell");ScrollPane scroll=new ScrollPane(shell);scroll.setFitToWidth(true);scroll.setFitToHeight(true);return scroll;}
    private Node right(){inspector.getStyleClass().add("side-panel");inspector.setPadding(new Insets(12));inspector.setPrefWidth(310);return inspector;}
    private void refresh(){picker.setItems(FXCollections.observableArrayList(app.project().getMenus().values()));if(!picker.getItems().isEmpty()){MenuScreen start=app.project().getMenus().get(app.project().getStartMenu());picker.setValue(start==null?picker.getItems().getFirst():start);}render();}
    private void render(){MenuScreen m=picker.getValue();canvas.getChildren().clear();selected=null;inspector.getChildren().setAll(title("BOTÓN"),new Label("Selecciona un botón."));if(m==null)return;java.awt.Color c=m.background;canvas.setStyle(String.format(Locale.ROOT,"-fx-background-color: rgb(%d,%d,%d);",c.getRed(),c.getGreen(),c.getBlue()));Label heading=new Label(m.title);heading.getStyleClass().add("menu-preview-title");heading.setLayoutX(30);heading.setLayoutY(30);canvas.getChildren().add(heading);for(MenuButton def:m.buttons){Button b=new Button(def.text);b.setLayoutX(def.x);b.setLayoutY(def.y);b.setPrefSize(def.width,def.height);b.getStyleClass().add("primary-button");double[]drag=new double[2];b.setOnMousePressed(e->{selected=def;drag[0]=e.getX();drag[1]=e.getY();rebuildInspector();e.consume();});b.setOnMouseDragged(e->{def.x=(int)Math.max(0,Math.min(640-def.width,e.getSceneX()-canvas.localToScene(0,0).getX()-drag[0]));def.y=(int)Math.max(0,Math.min(480-def.height,e.getSceneY()-canvas.localToScene(0,0).getY()-drag[1]));b.setLayoutX(def.x);b.setLayoutY(def.y);app.changed();e.consume();});canvas.getChildren().add(b);}}
    private void rebuildInspector(){inspector.getChildren().clear();if(selected==null){inspector.getChildren().add(new Label("Selecciona un botón"));return;}TextField text=new TextField(selected.text),target=new TextField(selected.target);ComboBox<MenuAction> action=new ComboBox<>(FXCollections.observableArrayList(MenuAction.values()));action.setValue(selected.action);Button delete=new Button("Eliminar botón");delete.getStyleClass().add("danger-button");Runnable apply=()->{selected.text=text.getText();selected.target=target.getText();selected.action=action.getValue();app.changed();render();};text.setOnAction(e->apply.run());target.setOnAction(e->apply.run());action.setOnAction(e->apply.run());delete.setOnAction(e->{picker.getValue().buttons.remove(selected);selected=null;app.changed();render();});GridPane form=form();row(form,0,"Texto",text);row(form,1,"Acción",action);row(form,2,"Destino",target);inspector.getChildren().addAll(title("BOTÓN"),form,delete);}
    private void addMenu(){TextInputDialog d=new TextInputDialog("Nueva pantalla");d.setHeaderText("Nombre de la pantalla");d.showAndWait().ifPresent(name->{if(name.isBlank())return;String id=slug(name,app.project().getMenus().keySet());MenuScreen m=new MenuScreen(id,name.trim());app.project().getMenus().put(id,m);app.changed();picker.getItems().add(m);picker.setValue(m);});}
    private void addButton(){MenuScreen m=picker.getValue();if(m==null)return;MenuButton b=new MenuButton("Botón",220,210,200,48,MenuAction.START_GAME,"");m.buttons.add(b);app.changed();render();selected=b;rebuildInspector();}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(75);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
    private static String slug(String name, Collection<String> used){String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="menu";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}
}
