package com.helium.gui.themes;

import me.pindour.catppuccin.gui.screens.CatppuccinModuleScreen;
import me.pindour.catppuccin.gui.screens.CatppuccinModulesScreen;
import me.pindour.catppuccin.gui.themes.catppuccin.CatppuccinGuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;

public class HeliumTheme extends CatppuccinGuiTheme {
    public static final HeliumTheme INSTANCE = new HeliumTheme();

    public HeliumTheme() {
    }

    @Override public Color accentColor()       { return new Color(245, 160, 70); }
    @Override public Color baseColor()         { return new Color(35, 25, 20); }
    @Override public Color mantleColor()       { return new Color(40, 30, 25); }
    @Override public Color crustColor()        { return new Color(25, 18, 15); }
    @Override public Color surface0Color()     { return new Color(60, 40, 25); }
    @Override public Color surface1Color()     { return new Color(80, 55, 35); }
    @Override public Color surface2Color()     { return new Color(100, 70, 45); }
    @Override public Color overlay0Color()     { return new Color(120, 85, 55); }
    @Override public Color overlay1Color()     { return new Color(140, 100, 65); }
    @Override public Color overlay2Color()     { return new Color(160, 115, 75); }
    @Override public Color textColor()         { return new Color(255, 248, 240); }
    @Override public Color textSecondaryColor(){ return new Color(210, 180, 140); }
    @Override public Color greenColor()        { return new Color(100, 200, 100); }
    @Override public Color yellowColor()       { return new Color(255, 215, 0); }
    @Override public Color redColor()          { return new Color(220, 100, 100); }
    @Override public Color blueColor()         { return new Color(137, 180, 250); }

    @Override
    public TabScreen modulesScreen() {
        return new CatppuccinModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof CatppuccinModulesScreen;
    }

    @Override
    public WidgetScreen moduleScreen(Module module) {
        return new CatppuccinModuleScreen(this, module);
    }
}