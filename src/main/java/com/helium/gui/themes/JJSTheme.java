package com.helium.gui.themes;

import com.helium.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class JJSTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final JJSTheme INSTANCE = new JJSTheme();

    @Override
    public String getName() {
        return "JJS Style";
    }

    @Override
    public boolean getCategoryIcons() {
        return false;
    }

    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(0, 162, 255, 255);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(0, 162, 255, 255);
    }

    @Override
    public SettingColor getTextColor() {
        return new SettingColor(30, 30, 30, 255);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(100, 100, 100, 255);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(0, 162, 255, 60);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(0, 200, 0);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(255, 180, 0);
    }

    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
                new SettingColor(240, 240, 242, 150),
                new SettingColor(225, 225, 228, 150),
                new SettingColor(210, 210, 215, 150)
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(255, 255, 255, 100);
    }

    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(50, 50, 50);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(200, 200, 200, 255);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(240, 240, 242, 0);
    }

    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
                new SettingColor(0, 162, 255),
                new SettingColor(0, 140, 220),
                new SettingColor(0, 120, 200)
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(0, 162, 255, 255);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(220, 220, 220, 255);
    }

    @Override
    public SettingColor getStarscriptText() { return new SettingColor(30, 30, 30); }
    @Override
    public SettingColor getStarscriptBraces() { return new SettingColor(0, 162, 255); }
    @Override
    public SettingColor getStarscriptParenthesis() { return new SettingColor(100, 100, 100); }
    @Override
    public SettingColor getStarscriptDots() { return new SettingColor(0, 162, 255); }
    @Override
    public SettingColor getStarscriptStrings() { return new SettingColor(0, 150, 0); }
    @Override
    public SettingColor getStarscriptNumbers() { return new SettingColor(150, 0, 200); }
    @Override
    public SettingColor getStarscriptKeywords() { return new SettingColor(0, 162, 255); }
}