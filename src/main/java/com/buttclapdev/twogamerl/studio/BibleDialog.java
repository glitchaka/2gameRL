package com.buttclapdev.twogamerl.studio;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BibleDialog {
    private record Chapter(String title,int offset,int level){@Override public String toString(){return title;}}
    private BibleDialog(){}

    static void show(Stage owner){
        String book=load();
        Stage stage=new Stage();stage.initOwner(owner);stage.setTitle("Biblia de 2GameScript 2.2.1");
        BorderPane root=new BorderPane();root.getStyleClass().addAll("bible-reader","tutorial-window");
        TextArea text=new TextArea(book);text.setEditable(false);text.setWrapText(true);text.getStyleClass().add("bible-text");
        TreeView<Chapter>index=buildIndex(book);index.getStyleClass().add("bible-index");index.setShowRoot(false);index.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null&&b.getValue()!=null){int p=Math.max(0,Math.min(book.length(),b.getValue().offset()));text.positionCaret(p);text.requestFocus();}});
        TextField search=new TextField();search.setPromptText("Buscar en toda la Biblia…");Button next=new Button("Siguiente");Label stats=new Label(stats(book,index));stats.getStyleClass().add("muted");
        final int[]cursor={0};Runnable find=()->{String q=search.getText()==null?"":search.getText().trim();if(q.isBlank())return;String haystack=book.toLowerCase(Locale.ROOT),needle=q.toLowerCase(Locale.ROOT);int at=haystack.indexOf(needle,Math.max(0,cursor[0]));if(at<0)at=haystack.indexOf(needle);if(at>=0){text.selectRange(at,at+q.length());text.requestFocus();cursor[0]=at+Math.max(1,q.length());}else{cursor[0]=0;new Alert(Alert.AlertType.INFORMATION,"No se encontró «"+q+"».",ButtonType.OK).showAndWait();}};next.setOnAction(e->find.run());search.setOnAction(e->find.run());search.textProperty().addListener((o,a,b)->cursor[0]=0);
        Button top=new Button("Inicio");top.setOnAction(e->{text.positionCaret(0);text.requestFocus();});Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox toolbar=new HBox(8,new Label("Biblia de 2GameScript"),search,next,top,spacer,stats);toolbar.setAlignment(Pos.CENTER_LEFT);toolbar.setPadding(new Insets(10,12,10,12));toolbar.getStyleClass().add("context-toolbar");
        SplitPane split=new SplitPane(index,text);split.setDividerPositions(.25);root.setTop(toolbar);root.setCenter(split);
        Scene scene=new Scene(root,1380,860);var css=BibleDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());ThemeManager.apply(scene);scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F,KeyCombination.CONTROL_DOWN),search::requestFocus);stage.setScene(scene);stage.setMinWidth(980);stage.setMinHeight(640);stage.show();
    }

    private static TreeView<Chapter>buildIndex(String book){TreeItem<Chapter>root=new TreeItem<>(new Chapter("2GameScript",0,0));root.setExpanded(true);List<TreeItem<Chapter>>stack=new ArrayList<>();stack.add(root);int offset=0;for(String line:book.split("\n",-1)){if(line.startsWith("#")){int level=0;while(level<line.length()&&line.charAt(level)=='#')level++;if(level<=4&&level<line.length()&&Character.isWhitespace(line.charAt(level))){String title=line.substring(level).trim();TreeItem<Chapter>item=new TreeItem<>(new Chapter(title,offset,level));while(stack.size()>level)stack.remove(stack.size()-1);TreeItem<Chapter>parent=stack.get(Math.max(0,stack.size()-1));parent.getChildren().add(item);item.setExpanded(level<=2);while(stack.size()<level)stack.add(parent);if(stack.size()==level)stack.add(item);else stack.set(level,item);}}offset+=line.length()+1;}return new TreeView<>(root);}
    private static String stats(String book,TreeView<Chapter>index){long lines=book.lines().count();int chapters=count(index.getRoot());return chapters+" secciones · "+lines+" líneas";}
    private static int count(TreeItem<Chapter>item){if(item==null)return 0;int n=item.getParent()==null?0:1;for(TreeItem<Chapter>c:item.getChildren())n+=count(c);return n;}
    private static String load(){try(InputStream in=BibleDialog.class.getResourceAsStream("/manuals/2GAMESCRIPT_BIBLE.md")){if(in==null)return"No se encontró el manual interno de 2GameScript.";return new String(in.readAllBytes(),StandardCharsets.UTF_8);}catch(Exception e){return"No se pudo abrir la Biblia de 2GameScript:\n"+e.getMessage();}}
}
