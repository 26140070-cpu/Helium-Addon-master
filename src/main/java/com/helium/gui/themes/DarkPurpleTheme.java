package com.helium.gui.themes;

import com.helium.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DarkPurpleTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final DarkPurpleTheme INSTANCE = new DarkPurpleTheme();

    @Override
    public String getName() {
        return "DarkPurple";
    }


    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(84, 48, 115);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(84, 48, 115);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(37, 173, 107);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(198, 58, 93);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(84, 48, 115);
    }


    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(164, 161, 179);
    }


    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(new SettingColor(44, 44, 59, 200), new SettingColor(84, 48, 115, 200), new SettingColor(84, 48, 115, 200));
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(84, 48, 115);
    }


    @Override
    public TriColorSetting getOutlineColor() {
        return new TriColorSetting(new SettingColor(102, 99, 128), new SettingColor(102, 99, 128), new SettingColor(102, 99, 128));
    }


    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(102, 99, 128);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(102, 99, 128, 150);
    }


    @Override
    public RecolorGuiTheme.TriColorSetting getScrollbarColor() {
        return new TriColorSetting(new SettingColor(69, 69, 75, 200), new SettingColor(74, 74, 80, 200), new SettingColor(79, 79, 85, 200));
    }


    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(new SettingColor(168, 94, 214), new SettingColor(168, 94, 214), new SettingColor(168, 94, 214));
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(35, 158, 98);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(43, 66, 66);
    }
}