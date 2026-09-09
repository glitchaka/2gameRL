package com.buttclapdev.twogamerl.studio;

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

    static void install(Stage stage,StudioApp app){ThemeManager.install();ThemeManager.apply(stage.getScene());installMenus(stage,app);replacePreviewButton(stage.getScene().getRoot(),app);RcFeaturePack.install(stage,app);installWorkspaceHooks(stage,app);StudioControlPolish.install(stage.getScene().getRoot());installScriptEditors(stage.getScene().getRoot(),app);TutorialPatch222.install(stage.getScene().getRoot(),stage);installPreviewCloseHook();}

    private static void installMenus(Stage stage,StudioApp app){
        MenuBar bar=findFirst(stage.getScene().getRoot(),MenuBar.class);if(bar==null)return;
        /* The old read-only "Navegador global de recursos" duplicated the real Graphics browser and exposed no useful editing workflow. Remove it entirely. */
        bar.getMenus().removeIf(m->"Recursos".equals(m.getText())&&m.getItems().stream().anyMatch(i->i.getText()!=null&&i.getText().contains("Navegador global")));
        if(bar.getMenus().stream().noneMatch(m->"Apariencia".equals(m.getText()))){Menu appearance=new Menu("Apariencia");ToggleGroup group=new ToggleGroup();for(ThemeManager.Theme theme:ThemeManager.Theme.values()){RadioMenuItem item=new RadioMenuItem(theme.toString());item.setToggleGroup(group);item.setSelected(theme==ThemeManager.current());item.setOnAction(e->{ThemeManager.set(theme);app.status("Tema del Studio: "+theme+".");});appearance.getItems().add(item);}int helpIndex=-1;for(int i=0;i<bar.getMenus().size();i++)if("Ayuda".equals(bar.getMenus().get(i).getText())){helpIndex=i;break;}if(helpIndex<0)bar.getMenus().add(appearance);else bar.getMenus().add(helpIndex,appearance);}
        Menu help=bar.getMenus().stream().filter(m->"Ayuda".equals(m.getText())).findFirst().orElse(null);if(help!=null&&help.getItems().stream().noneMatch(i->"Biblia completa de 2GameScript".equals(i.getText()))){for(MenuItem item:help.getItems())if(item.getText()!=null&&item.getText().contains("Tutorial y referencia")){item.setText("Biblia técnica de 2GameScript");item.setAccelerator(KeyCombination.keyCombination("F2"));item.setOnAction(e->BibleDialog.show(stage));}MenuItem bible=new MenuItem("Biblia completa de 2GameScript");bible.setAccelerator(KeyCombination.keyCombination("F1"));bible.setOnAction(e->BibleDialog.show(stage));help.getItems().add(new SeparatorMenuItem());help.getItems().add(bible);}
    }

    private static void replacePreviewButton(Node root,StudioApp app){
        if(root instanceof Button b&&b.getText()!=null&&b.getText().contains("Probar")){b.setOnAction(e->StudioPreviewWindow.show(app));return;}
        if(root instanceof Parent p)for(Node child:p.getChildrenUnmodifiable())replacePreviewButton(child,app);
    }

    private static void installWorkspaceHooks(Stage stage,StudioApp app){if(!(stage.getScene().getRoot() instanceof BorderPane root))return;if(!(root.getCenter() instanceof StackPane workspace))return;Runnable installCurrent=()->{for(Node child:workspace.getChildren()){if(child instanceof GraphicsEditorPane pane)GraphicsBrowserEnhancements.install(pane,app);if(child instanceof SceneEditorPane pane)SceneTools222.install(pane,app);StudioControlPolish.install(child);installScriptEditors(child,app);TutorialPatch222.install(child,stage);}};installCurrent.run();workspace.getChildren().addListener((ListChangeListener<Node>)c->installCurrent.run());}

    private static void installScriptEditors(Node node,StudioApp app){if(node instanceof TextArea area&&isScriptEditor(area)&&!Boolean.TRUE.equals(area.getProperties().get(AUTOCOMPLETE))){area.getProperties().put(AUTOCOMPLETE,true);ScriptAutocomplete.install(area,app,List::of);Tooltip.install(area,new Tooltip("2GameScript: autocompletado contextual automático. Ctrl+Espacio fuerza las sugerencias."));}if(node instanceof Parent parent&&!Boolean.TRUE.equals(parent.getProperties().get(AUTOCOMPLETE+".watch"))){parent.getProperties().put(AUTOCOMPLETE+".watch",true);parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>)change->{while(change.next())for(Node added:change.getAddedSubList())installScriptEditors(added,app);});for(Node child:parent.getChildrenUnmodifiable())installScriptEditors(child,app);}}
    private static boolean isScriptEditor(TextArea area){String style=area.getStyle()==null?"":area.getStyle().toLowerCase();return style.contains("monospace")||style.contains("consolas")||style.contains("jetbrains mono");}

    private static void installPreviewCloseHook(){Window.getWindows().addListener((ListChangeListener<Window>)change->{while(change.next())for(Window window:change.getAddedSubList())hookPreview(window);});for(Window window:Window.getWindows())hookPreview(window);}
    private static void hookPreview(Window window){if(!(window instanceof Stage stage))return;Runnable install=()->{Scene scene=stage.getScene();if(scene==null||!stage.getTitle().startsWith("Probar ·")||!PREVIEW_HOOKS.add(scene))return;scene.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.F10){stage.close();e.consume();}});};install.run();stage.sceneProperty().addListener((o,a,b)->install.run());stage.titleProperty().addListener((o,a,b)->install.run());}

    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}return null;}
}
