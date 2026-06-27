package com.helium.util;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HeliumMenuFogConfig {
    public static final File CONFIG_FILE = new File(HeliumBackgroundConfig.BG_DIR, "menu-fog.txt");
    public static boolean enabled = true; 

    public static void init() {
        HeliumBackgroundConfig.init();
        load();
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
                for (String line : lines) {
                    if (line.startsWith("enabled=")) {
                        enabled = Boolean.parseBoolean(line.substring(8));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("enabled=" + enabled);
            Files.write(CONFIG_FILE.toPath(), lines);
        } catch (Exception ignored) {}
    }
}