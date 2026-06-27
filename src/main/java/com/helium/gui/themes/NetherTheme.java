package com.helium.gui.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import com.helium.gui.RecolorGuiTheme;

public class NetherTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final NetherTheme INSTANCE = new NetherTheme();

    private static final int CRIMSON_BRIGHT = 255;
    private static final int CRIMSON_MID = 200;
    private static final int CRIMSON_DARK = 139;

    private static final int SOUL_FIRE = 100;
    private static final int SOUL_FIRE_BRIGHT = 150;

    private static final int OBSIDIAN_DARK = 30;
    private static final int OBSIDIAN_MID = 60;
    private static final int OBSIDIAN_LIGHT = 90;

    private static final int LAVA_ORANGE = 255;
    private static final int LAVA_RED = 150;

    @Override
    public String getName() {
        return "Nether";
    }

    @Override
    public boolean getCategoryIcons() {
        return true;
    }


    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(220, 20, 60);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(200, 30, 70);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(255, 140, 0);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(30, 20, 20);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(100, 150, 255);
    }


    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(200, 150, 150);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(255, 80, 80, 100);
    }

    @Override
    public SettingColor getTitleTextColor() {
        return new SettingColor(255, 120, 0);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(100, 180, 255);
    }


    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
                new SettingColor(20, 15, 15, 230),      
                new SettingColor(60, 40, 40, 230),      
                new SettingColor(90, 50, 50, 230)       
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(50, 35, 35, 200);
    }


    @Override
    public TriColorSetting getOutlineColor() {
        return new TriColorSetting(
                new SettingColor(60, 30, 30),           
                new SettingColor(100, 50, 50),          
                new SettingColor(200, 100, 100)         
        );
    }


    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(220, 80, 80);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(150, 60, 60, 180);
    }


    @Override
    public TriColorSetting getScrollbarColor() {
        return new TriColorSetting(
                new SettingColor(80, 40, 40, 220),      
                new SettingColor(150, 60, 60, 220),     
                new SettingColor(220, 100, 100, 220)    
        );
    }


    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
                new SettingColor(150, 60, 60),          
                new SettingColor(200, 100, 100),        
                new SettingColor(255, 140, 0)           
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(255, 120, 0);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(40, 30, 30);
    }


    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(220, 220, 220);
    }

    @Override
    public SettingColor getStarscriptBraces() {
        return new SettingColor(255, 140, 0);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(200, 80, 80);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(120, 160, 255);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(255, 160, 80);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(255, 80, 80);
    }

    @Override
    public SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(200, 120, 120);
    }
}