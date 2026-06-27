package com.helium.gui.themes;

import com.helium.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class NamiTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final NamiTheme INSTANCE = new NamiTheme();

    @Override
    public String getName() {
        return "Nami Style";
    }

    @Override
    public boolean getCategoryIcons() {
        return false;
    }

    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(60, 100, 255, 255);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(60, 100, 255, 255);
    }

    @Override
    public SettingColor getTextColor() {
        return new SettingColor(255, 255, 255, 255);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(200, 200, 200, 255);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(60, 100, 255, 100);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(45, 225, 45);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(255, 255, 0);
    }

    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
                new SettingColor(10, 15, 30, 160),
                new SettingColor(15, 25, 50, 160),
                new SettingColor(20, 35, 70, 180)
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(20, 20, 40, 100);
    }

    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(60, 100, 255, 200);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(0, 0, 0, 0);
    }

    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
                new SettingColor(60, 100, 255),
                new SettingColor(80, 120, 255),
                new SettingColor(100, 150, 255)
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(60, 100, 255, 200);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(10, 10, 20, 200);
    }

    @Override
    public SettingColor getStarscriptText() { return new SettingColor(255, 255, 255); }
    @Override
    public SettingColor getStarscriptBraces() { return new SettingColor(60, 100, 255); }
    @Override
    public SettingColor getStarscriptParenthesis() { return new SettingColor(200, 200, 200); }
    @Override
    public SettingColor getStarscriptDots() { return new SettingColor(60, 100, 255); }
    @Override
    public SettingColor getStarscriptStrings() { return new SettingColor(0, 255, 150); }
    @Override
    public SettingColor getStarscriptNumbers() { return new SettingColor(180, 150, 255); }
    @Override
    public SettingColor getStarscriptKeywords() { return new SettingColor(60, 100, 255); }
}