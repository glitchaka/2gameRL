package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.editor.EditorFrame;
import com.buttclapdev.twogamerl.runtime.GameLauncher;

import javax.swing.*;
import java.util.Arrays;

public final class App {
    private App() {}
    public static void main(String[] args) {
        if (args.length > 0 && "--game".equals(args[0])) {
            GameLauncher.main(Arrays.copyOfRange(args,1,args.length));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new EditorFrame().setVisible(true);
        });
    }
}
