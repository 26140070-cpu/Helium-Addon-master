package com.helium.modules.render;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class CustomLoadingScreen extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .defaultValue(new SettingColor(20, 20, 20, 255))
            .build()
    );

    public static CustomLoadingScreen INSTANCE;

    public CustomLoadingScreen() {
        super(HeliumAddon.CATEGORY, "HeliumCLS", "Modifies the background color of loading screens.");
        INSTANCE = this;
    }

    public int getBackgroundColor() {
        return color.get().getPacked();
    }
}