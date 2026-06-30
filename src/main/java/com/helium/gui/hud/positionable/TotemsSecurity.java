package com.helium.gui.hud.positionable;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Items;
import com.helium.HeliumAddon;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TotemsSecurity extends HudElement {
    public static final HudElementInfo<TotemsSecurity> INFO = new HudElementInfo<>(HeliumAddon.Helium_Hud, "totem-security", "Displays totem health and remaining totems.", TotemsSecurity::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color of the text.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );

    public TotemsSecurity() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        int totems = 0;

        if (mc.player != null) {
            totems = InvUtils.find(Items.TOTEM_OF_UNDYING).count();
        }

        double healthPercent = Math.min(100.0, (totems / 10.0) * 100.0);
        String text = "Totem Health: " + (int)healthPercent + "% | Remaining: " + totems;

        setSize(renderer.textWidth(text, true), renderer.textHeight(true));
        renderer.text(text, x, y, textColor.get(), true);
    }
}