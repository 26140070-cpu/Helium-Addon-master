package com.helium.util.render;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class HeliumRenderUtils {

    public static void drawRoundRect(HudRenderer renderer, double x, double y, double w, double h, double rad, Color color) {
        if (rad <= 0) {
            renderer.quad(x, y, w, h, color);
            return;
        }

        renderer.quad(x, y + rad, w, h - rad * 2, color);

        int segments = 8;
        for (int i = 0; i < segments; i++) {
            double pct = (double) i / segments;
            double angle = Math.asin(1.0 - pct);
            double offset = rad * (1.0 - Math.cos(angle));
            double rowHeight = rad / segments;

            renderer.quad(x + offset, y + (i * rowHeight), w - offset * 2, rowHeight, color);
            renderer.quad(x + offset, y + h - ((i + 1) * rowHeight), w - offset * 2, rowHeight, color);
        }
    }

    public static void drawScaledText(HudRenderer renderer, String text, double x, double y, Color color, boolean shadow, double scale) {
        renderer.text(text, x, y, color, shadow, scale);
    }
}