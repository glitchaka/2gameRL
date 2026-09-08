package com.buttclapdev.twogamerl.studio;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class EditorShortcuts {
    private EditorShortcuts() {}

    static void install(Node node, Runnable copy, Runnable paste, Runnable delete) {
        node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isEditingText(event.getTarget())) return;
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

    private static boolean isEditingText(Object target) {
        if (!(target instanceof Node node)) return false;
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof TextInputControl) return true;
        }
        return false;
    }
}
