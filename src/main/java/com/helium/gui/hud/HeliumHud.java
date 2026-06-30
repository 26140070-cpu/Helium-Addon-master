package com.helium.gui.hud;

import com.helium.util.render.HeliumRenderUtils;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public abstract class HeliumHud extends HudElement {

    public HeliumHud(HudElementInfo<?> info) {
        super(info);
    }

    public void renderBackdrop(HudRenderer renderer, double rad) {
        int argb = this.getArgb();
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        Color color = new Color(r, g, b, a);

        HeliumRenderUtils.drawRoundRect(renderer, this.x, this.y, this.getWidth(), this.getHeight(), rad, color);
    }

    public void renderBackdrop(HudRenderer renderer) {
        renderBackdrop(renderer, 4.0);
    }

    public abstract int getArgb();
}