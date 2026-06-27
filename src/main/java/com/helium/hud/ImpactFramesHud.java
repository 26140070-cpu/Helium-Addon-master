package com.helium.hud;

import com.helium.util.render.ImpactFramesHandler;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import static com.helium.HeliumAddon.Helium_Hud;

public class ImpactFramesHud extends HudElement {
    public static final HudElementInfo<ImpactFramesHud> INFO = new HudElementInfo<>(
            Helium_Hud, "impact-frames", "Renders Impact Frames on screen", ImpactFramesHud::new
    );

    public ImpactFramesHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        DrawContext context = renderer.drawContext;
        if (context != null) {
            ImpactFramesHandler.render(context);
        }
    }
}