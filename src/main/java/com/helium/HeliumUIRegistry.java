package com.helium;

import com.helium.gui.HeliumTab;
import com.helium.gui.WelcomeWindow;
import com.helium.gui.themes.*;
import com.helium.util.NotificationManager;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class HeliumUIRegistry {
    private static final Logger LOG = LogUtils.getLogger();

    public static void register() {
        LOG.info("[Helium] Registering UI elements and Themes...");

        Tabs.get().add(new HeliumTab());

        GuiThemes.add(NamiTheme.INSTANCE);
        GuiThemes.add(JJSTheme.INSTANCE);
        GuiThemes.add(LambdaTheme.INSTANCE);
        GuiThemes.add(NetherTheme.INSTANCE);
        GuiThemes.add(HeliumTheme.INSTANCE);
        GuiThemes.add(DarkPurpleTheme.INSTANCE);

        try {
            java.lang.reflect.Field nameField = HeliumTheme.class.getSuperclass().getSuperclass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(HeliumTheme.INSTANCE, "Helium");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.error("Could not rename theme Helium", e);
        }

        try {
            WelcomeWindow.showWindow();
        } catch (Exception e) {
            LOG.error("Failed to display Welcome Window on early init", e);
        }

        NotificationManager.INSTANCE.add("Helium Addon", "UI Elements & Premium Themes Loaded!", 4000);
    }
}