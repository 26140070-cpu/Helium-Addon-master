package com.helium.util;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FontAutoInstaller {

    private static final String[] FONTS = {
            "fingerpaint.ttf",
            "rubikstorm.ttf"
    };

    public static void init() {
        try {
            File fontsFolder = new File(MeteorClient.FOLDER, "fonts");

            if (!fontsFolder.exists()) {
                fontsFolder.mkdirs();
            }

            for (String fontName : FONTS) {
                File fontFile = new File(fontsFolder, fontName);

                if (!fontFile.exists()) {
                    InputStream in = FontAutoInstaller.class.getResourceAsStream("/assets/helium/fonts/" + fontName);

                    if (in != null) {
                        Files.copy(in, fontFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        in.close();
                        MeteorClient.LOG.info("[Helium] Font " + fontName + " successfully auto-installed.");
                    } else {
                        MeteorClient.LOG.error("[Helium] Failed to find " + fontName + " inside the addon.");
                    }
                }
            }

            if (Config.get() != null && !Config.get().customFont.get()) {
                Config.get().customFont.set(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}