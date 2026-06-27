package com.helium.util;

import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HeliumConfig {
    public static final File CONFIG_FILE = new File(MinecraftClient.getInstance().runDirectory, "meteor-client/helium-splash.txt");
    public static final List<String> splashTexts = new ArrayList<>();
    public static int currentIndex = 0;
    public static int splashColor = 0xFFFF00;
    public static boolean splashRainbow = false;

    public static void init() {
        load();
    }

    public static void updateIndex() {
        if (splashTexts.size() <= 1) return;
        currentIndex++;
        if (currentIndex >= splashTexts.size()) {
            currentIndex = 0;
        }
    }

    public static String getCurrentSplash() {
        if (splashTexts.isEmpty()) return "Helium Addon";
        if (currentIndex >= splashTexts.size()) currentIndex = 0;
        return splashTexts.get(currentIndex);
    }

    public static int getSplashColor() {
        return splashColor;
    }

    public static void setSplashColor(int color) {
        splashColor = color;
        save();
    }

    public static boolean getSplashRainbow() {         
        return splashRainbow;
    }

    public static void setSplashRainbow(boolean rainbow) {   
        splashRainbow = rainbow;
        save();
    }

    public static void load() {
        splashTexts.clear();
        try {
            if (CONFIG_FILE.exists()) {
                List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
                if (!lines.isEmpty()) {
                    if (lines.get(0).startsWith("color=")) {
                        try {
                            splashColor = Integer.parseInt(lines.get(0).substring(6), 16);
                        } catch (NumberFormatException ignored) {}
                        lines.remove(0);
                    }
                    if (!lines.isEmpty() && lines.get(0).startsWith("rainbow=")) {
                        splashRainbow = Boolean.parseBoolean(lines.get(0).substring(8));
                        lines.remove(0);
                    }
                }
                splashTexts.addAll(lines);
            } else {
                splashTexts.add("Helium on Top!");
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("color=" + Integer.toHexString(splashColor).toUpperCase());
            lines.add("rainbow=" + splashRainbow);
            lines.addAll(splashTexts);
            Files.write(CONFIG_FILE.toPath(), lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addText(String text) {
        splashTexts.add(text);
        save();
    }

    public static void removeText(int index) {
        if (index >= 0 && index < splashTexts.size()) {
            splashTexts.remove(index);
            save();
        }
    }
}