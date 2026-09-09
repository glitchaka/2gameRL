package com.buttclapdev.twogamerl.studio;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

/** Removes the obsolete quick-reference example set from the 2.2.2 script workspace in favor of the canonical technical Bible. */
final class TutorialPatch222 {
    private TutorialPatch222(){}
    static void install(Node root,Stage owner){if(root instanceof Button b&&"Tutorial + API".equals(b.getText())&&!Boolean.TRUE.equals(b.getProperties().get("2rl.bible.222"))){b.getProperties().put("2rl.bible.222",true);b.setText("Biblia técnica");b.setTooltip(new Tooltip("Especificación técnica canónica de 2GameScript 2.2.2"));b.setOnAction(e->BibleDialog.show(owner));}if(root instanceof Parent p)for(Node c:p.getChildrenUnmodifiable())install(c,owner);}
}
