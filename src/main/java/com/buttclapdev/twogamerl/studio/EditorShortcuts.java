package com.buttclapdev.twogamerl.studio;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class EditorShortcuts {
    private EditorShortcuts() {}

    static void install(Node node, Runnable copy, Runnable paste, Runnable delete) {
        node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isEditingText(event)) return;
            if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
                if (copy != null) copy.run();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.V) {
                if (paste != null) paste.run();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                if (delete != null) delete.run();
                event.consume();
            }
        });
    }

    private static boolean isEditingText(KeyEvent event) {
        if (event.getTarget() instanceof Node target) {
            for (Node current=target;current!=null;current=current.getParent()) if (current instanceof TextInputControl) return true;
            if (target.getScene()!=null && target.getScene().getFocusOwner() instanceof TextInputControl) return true;
        }
        return false;
    }
}
