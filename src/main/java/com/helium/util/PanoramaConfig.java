package com.helium.util;

import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PanoramaConfig {
    private static final File CONFIG_FILE = new File(
            MinecraftClient.getInstance().runDirectory, "meteor-client/helium-panorama.txt"
    );
    public static boolean enabled = false;

    public static void init() {
        load();
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
                if (!lines.isEmpty()) {
                    enabled = Boolean.parseBoolean(lines.get(0));
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add(String.valueOf(enabled));
            Files.write(CONFIG_FILE.toPath(), lines);
        } catch (Exception ignored) {}
    }
}