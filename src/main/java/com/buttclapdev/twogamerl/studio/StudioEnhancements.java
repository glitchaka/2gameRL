package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.ResourceRef;
import com.buttclapdev.twogamerl.runtime.GameView;
import com.buttclapdev.twogamerl.runtime.ParticleRuntime222;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class StudioEnhancements {
    private static final String AUTOCOMPLETE="2rl.script.autocomplete";
    private static final Set<Scene> PREVIEW_HOOKS=Collections.newSetFromMap(new WeakHashMap<>());
    private StudioEnhancements(){}

    static void install(Stage stage,StudioApp app){ThemeManager.install();ThemeManager.apply(stage.getScene());installMenus(stage,app);installPlayOverride(stage,app);installWorkspaceHooks(stage,app);installScriptEditors(stage.getScene().getRoot(),app);installPreviewCloseHook();}

    private static void installMenus(Stage stage,StudioApp app){
        MenuBar bar=findFirst(stage.getScene().getRoot(),MenuBar.class);if(bar==null)return;
        if(bar.getMenus().stream().noneMatch(m->"Apariencia".equals(m.getText()))){Menu appearance=new Menu("Apariencia");ToggleGroup group=new ToggleGroup();for(ThemeManager.Theme theme:ThemeManager.Theme.values()){RadioMenuItem item=new RadioMenuItem(theme.toString());item.setToggleGroup(group);item.setSelected(theme==ThemeManager.current());item.setOnAction(e->{ThemeManager.set(theme);app.status("Tema del Studio: "+theme+".");});appearance.getItems().add(item);}int helpIndex=-1;for(int i=0;i<bar.getMenus().size();i++)if("Ayuda".equals(bar.getMenus().get(i).getText())){helpIndex=i;break;}if(helpIndex<0)bar.getMenus().add(appearance);else bar.getMenus().add(helpIndex,appearance);}
        Menu help=bar.getMenus().stream().filter(m->"Ayuda".equals(m.getText())).findFirst().orElse(null);if(help!=null&&help.getItems().stream().noneMatch(i->"Biblia completa de 2GameScript".equals(i.getText()))){for(MenuItem item:help.getItems())if(item.getText()!=null&&item.getText().contains("Tutorial y referencia")){item.setText("Tutorial interactivo y referencia rápida");item.setAccelerator(KeyCombination.keyCombination("F2"));}MenuItem bible=new MenuItem("Biblia completa de 2GameScript");bible.setAccelerator(KeyCombination.keyCombination("F1"));bible.setOnAction(e->BibleDialog.show(stage));help.getItems().add(new SeparatorMenuItem());help.getItems().add(bible);}
    }

    private static void installPlayOverride(Stage stage,StudioApp app){for(Button b:findAll(stage.getScene().getRoot(),Button.class))if("▶ Probar".equals(b.getText())){b.setOnAction(e->playPreview(app));b.setTooltip(new Tooltip("Probar en pantalla completa. F11 alterna pantalla completa; F10 cierra solo la prueba."));}}
    private static void playPreview(StudioApp app){
        GameProject runtime=app.project().deepCopy();ResourceRef.installRuntimeAliases(runtime);Stage preview=new Stage();preview.getIcons().setAll(app.owner().getIcons());preview.setTitle("Probar · "+runtime.getTitle());GameView view=new GameView(runtime);ParticleRuntime222.attach(view);Scene scene=new Scene(view,Math.max(640,runtime.getLogicalWidth()*3),Math.max(360,runtime.getLogicalHeight()*3));var css=StudioEnhancements.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());scene.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.F10){preview.close();e.consume();}else if(e.getCode()==KeyCode.F11){preview.setFullScreen(!preview.isFullScreen());e.consume();}});preview.setScene(scene);preview.setOnShown(e->{preview.setFullScreen(true);view.requestFocus();});preview.setOnHidden(e->view.stop());preview.show();app.status("Prueba en ejecución · F10 cerrar prueba · F11 pantalla completa.");
    }

    private static void installWorkspaceHooks(Stage stage,StudioApp app){if(!(stage.getScene().getRoot() instanceof BorderPane root))return;if(!(root.getCenter() instanceof StackPane workspace))return;Runnable installCurrent=()->{for(Node child:workspace.getChildren()){if(child instanceof GraphicsEditorPane pane){GraphicsBrowserEnhancements.install(pane,app);ParticleStudio222.install(pane,app);}if(child instanceof SceneEditorPane pane){SceneInspector222.install(pane,app);CameraViewport222.install(pane,app);}installScriptEditors(child,app);}};installCurrent.run();workspace.getChildren().addListener((ListChangeListener<Node>)c->installCurrent.run());}

    private static void installScriptEditors(Node node,StudioApp app){if(node instanceof TextArea area&&isScriptEditor(area)&&!Boolean.TRUE.equals(area.getProperties().get(AUTOCOMPLETE))){area.getProperties().put(AUTOCOMPLETE,true);ScriptAutocomplete.install(area,app,List::of);ScriptSemantic222.install(area,app);Tooltip.install(area,new Tooltip("2GameScript: autocompletado contextual automático. Ctrl+Espacio fuerza las sugerencias."));}if(node instanceof Parent parent&&!Boolean.TRUE.equals(parent.getProperties().get(AUTOCOMPLETE+".watch"))){parent.getProperties().put(AUTOCOMPLETE+".watch",true);parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>)change->{while(change.next())for(Node added:change.getAddedSubList())installScriptEditors(added,app);});for(Node child:parent.getChildrenUnmodifiable())installScriptEditors(child,app);}}
    private static boolean isScriptEditor(TextArea area){String style=area.getStyle()==null?"":area.getStyle().toLowerCase();return style.contains("monospace")||style.contains("consolas")||style.contains("jetbrains mono");}

    private static void installPreviewCloseHook(){Window.getWindows().addListener((ListChangeListener<Window>)change->{while(change.next())for(Window window:change.getAddedSubList())hookPreview(window);});for(Window window:Window.getWindows())hookPreview(window);}
    private static void hookPreview(Window window){if(!(window instanceof Stage stage))return;Runnable install=()->{Scene scene=stage.getScene();if(scene==null||!stage.getTitle().startsWith("Probar ·")||!PREVIEW_HOOKS.add(scene))return;if(scene.getRoot() instanceof GameView view)ParticleRuntime222.attach(view);scene.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.F10){stage.close();e.consume();}});};install.run();stage.sceneProperty().addListener((o,a,b)->install.run());stage.titleProperty().addListener((o,a,b)->install.run());}

    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}return null;}
    private static <T extends Node>List<T>findAll(Node root,Class<T>type){ArrayList<T>out=new ArrayList<>();findAll(root,type,out);return out;}private static <T extends Node>void findAll(Node root,Class<T>type,List<T>out){if(type.isInstance(root))out.add(type.cast(root));if(root instanceof Parent p)for(Node child:p.getChildrenUnmodifiable())findAll(child,type,out);}
}
