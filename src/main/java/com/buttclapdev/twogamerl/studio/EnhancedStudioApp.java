package com.buttclapdev.twogamerl.studio;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 2.2.1 bootstrap layer.  StudioApp remains the core editor while this class
 * installs patch-level desktop features that do not belong to the game project
 * format: editor themes, the technical Bible reader and Asset Browser views.
 */
public final class EnhancedStudioApp extends Application {
    private StudioApp delegate;

    @Override public void start(Stage stage) throws Exception {
        delegate = new StudioApp();
        delegate.start(stage);
        StudioEnhancements.install(stage, delegate);
    }

    @Override public void stop() throws Exception {
        if (delegate != null) delegate.stop();
    }
}
