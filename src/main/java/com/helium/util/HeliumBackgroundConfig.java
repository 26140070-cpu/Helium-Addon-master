package com.helium.util;

import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HeliumBackgroundConfig {
    public static final File BG_DIR = new File(MinecraftClient.getInstance().runDirectory, "meteor-client/helium-background");
    public static final File CONFIG_FILE = new File(BG_DIR, "config.txt");

    public static boolean enabled = false;
    public static String imageFileName = "";
    public static String audioFileName = "";
    public static boolean cleanerEnabled = true;  

    public static void init() {
        if (!BG_DIR.exists()) BG_DIR.mkdirs();
        load();
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
                for (String line : lines) {
                    if (line.startsWith("enabled=")) enabled = Boolean.parseBoolean(line.substring(8));
                    else if (line.startsWith("image=")) imageFileName = line.substring(6);
                    else if (line.startsWith("audio=")) audioFileName = line.substring(6);
                    else if (line.startsWith("cleaner=")) cleanerEnabled = Boolean.parseBoolean(line.substring(8));
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("enabled=" + enabled);
            lines.add("image=" + imageFileName);
            lines.add("audio=" + audioFileName);
            lines.add("cleaner=" + cleanerEnabled);
            Files.write(CONFIG_FILE.toPath(), lines);
        } catch (Exception ignored) {}
    }

    public static List<String> getImageFiles() {
        return getFiles("png", "jpg", "jpeg", "gif");
    }

    public static List<String> getAudioFiles() {
        return getFiles("mp3", "wav");
    }

    private static List<String> getFiles(String... extensions) {
        List<String> result = new ArrayList<>();
        File[] files = BG_DIR.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                for (String ext : extensions) {
                    if (name.endsWith("." + ext)) {
                        result.add(f.getName());
                        break;
                    }
                }
            }
        }
        return result;
    }
}