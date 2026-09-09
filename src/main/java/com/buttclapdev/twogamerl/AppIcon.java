package com.buttclapdev.twogamerl;

import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Generates the 2RL mark in code so every packaged build always has an application icon. */
public final class AppIcon {
    private AppIcon() {}

    public static Image create(int requestedSize) {
        int size = Math.max(32, requestedSize);
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        double s = size / 256.0;

        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#071827")),
                new Stop(.55, Color.web("#0A1223")),
                new Stop(1, Color.web("#160B2B"))));
        g.fillRoundRect(3*s, 3*s, 250*s, 250*s, 46*s, 46*s);

        g.setGlobalAlpha(.18);
        g.setStroke(Color.web("#4CDFFF"));
        g.setLineWidth(Math.max(1, .7*s));
        for (int i = 28; i < 240; i += 22) {
            g.strokeLine(i*s, 18*s, i*s, 238*s);
            g.strokeLine(18*s, i*s, 238*s, i*s);
        }
        g.setGlobalAlpha(1);

        g.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#43F4FF")),
                new Stop(.55, Color.web("#3194FF")),
                new Stop(1, Color.web("#C03BFF"))));
        g.setLineWidth(7*s);
        g.strokeRoundRect(8*s, 8*s, 240*s, 240*s, 42*s, 42*s);

        // Small pixel fragments make the mark recognizable as a 2D/pixel-oriented engine.
        g.setFill(Color.web("#38EAF7"));
        g.fillRect(25*s, 75*s, 10*s, 10*s);
        g.fillRect(38*s, 62*s, 7*s, 7*s);
        g.setFill(Color.web("#338DFF"));
        g.fillRect(219*s, 179*s, 9*s, 9*s);
        g.fillRect(229*s, 190*s, 6*s, 6*s);
        g.setFill(Color.web("#D337FF"));
        g.fillRect(216*s, 62*s, 7*s, 7*s);

        // Subtle isometric diamond behind the lettermark.
        g.setGlobalAlpha(.38);
        g.setStroke(Color.web("#28CBFF"));
        g.setLineWidth(2*s);
        g.strokePolygon(new double[]{128*s, 202*s, 128*s, 54*s},
                        new double[]{41*s, 90*s, 218*s, 90*s}, 4);
        g.setGlobalAlpha(1);

        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 72*s));
        g.setFill(Color.rgb(0, 0, 0, .62));
        g.fillText("2RL", 131*s, 137*s);
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#35EEF5")),
                new Stop(.38, Color.web("#EBF7FF")),
                new Stop(1, Color.web("#B7D8FF"))));
        g.fillText("2RL", 128*s, 133*s);

        WritableImage result = new WritableImage(size, size);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, result);
    }
}
