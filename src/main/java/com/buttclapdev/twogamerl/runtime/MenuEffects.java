package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject.MenuAnimation;
import com.buttclapdev.twogamerl.model.GameProject.MenuHoverEffect;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class MenuEffects {
    private MenuEffects() {}

    public static StackPane decorate(Node content, MenuAnimation animation, double speed, MenuHoverEffect hover) {
        StackPane wrapper = new StackPane(content);
        wrapper.setPickOnBounds(true);
        applyIdle(wrapper, animation, speed);
        installHover(content, hover);
        return wrapper;
    }

    public static void applyIdle(Node node, MenuAnimation animation, double speed) {
        double factor = Math.max(.2, speed);
        switch (animation == null ? MenuAnimation.NONE : animation) {
            case NONE -> { }
            case PULSE -> {
                ScaleTransition t = new ScaleTransition(Duration.seconds(1.15 / factor), node);
                t.setFromX(1); t.setFromY(1); t.setToX(1.055); t.setToY(1.055);
                t.setAutoReverse(true); t.setCycleCount(Animation.INDEFINITE); t.play();
            }
            case FLOAT -> {
                TranslateTransition t = new TranslateTransition(Duration.seconds(1.35 / factor), node);
                t.setFromY(0); t.setToY(-7); t.setAutoReverse(true); t.setCycleCount(Animation.INDEFINITE); t.play();
            }
            case FADE -> {
                FadeTransition t = new FadeTransition(Duration.seconds(1.25 / factor), node);
                t.setFromValue(1); t.setToValue(.55); t.setAutoReverse(true); t.setCycleCount(Animation.INDEFINITE); t.play();
            }
        }
    }

    public static void installHover(Node node, MenuHoverEffect effect) {
        MenuHoverEffect actual = effect == null ? MenuHoverEffect.NONE : effect;
        node.setOnMouseEntered(e -> {
            switch (actual) {
                case NONE -> { }
                case SCALE -> { node.setScaleX(1.06); node.setScaleY(1.06); }
                case GLOW -> node.setEffect(new DropShadow(18, Color.rgb(72, 184, 255, .85)));
                case LIFT -> node.setTranslateY(-4);
            }
        });
        node.setOnMouseExited(e -> {
            node.setScaleX(1); node.setScaleY(1); node.setTranslateY(0); node.setEffect(null);
        });
    }
}
