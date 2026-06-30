package com.helium.gui.hud.positionable;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.systems.hud.HudGroup;

public class Watermark extends HudElement {
    public static final HudElementInfo<Watermark> MARKTEXT = new HudElementInfo<>(
            new HudGroup("Helium"),
            "helium-watermark",
            "Shows the Helium Addon Watermark.",
            Watermark::new
    );

    public Watermark() {
        super(MARKTEXT);
    }

    @Override
    public void render(HudRenderer renderer) {
        double width = renderer.textWidth("Helium Addon V0.0.1", true);
        double height = renderer.textHeight(true);

        this.box.setSize(width, height);

        renderer.text("Helium Addon V0.0.1", this.x, this.y, Color.RED, true);
    }
}