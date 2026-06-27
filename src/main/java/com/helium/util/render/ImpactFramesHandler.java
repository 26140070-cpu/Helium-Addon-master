package com.helium.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImpactFramesHandler {
    private static final CopyOnWriteArrayList<ImpactFrame> activeFrames = new CopyOnWriteArrayList<>();
    private static final java.util.Map<String, List<Identifier>> textureCache = new java.util.HashMap<>();
    private static boolean texturesLoaded = false;

    public static void preloadTextures() {
        if (texturesLoaded) return;
        texturesLoaded = true;

        loadAndRegister("hit");
        loadAndRegister("kill");
        loadAndRegister("totem_pop");
    }

    private static void loadAndRegister(String folder) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Identifier> ids = new ArrayList<>();
        int index = 1;
        while (true) {
            final int currentIndex = index;
            Identifier id = Identifier.of("helium", "textures/impactframe/" + folder + "/frame_" + currentIndex + ".png");
            InputStream stream = ImpactFramesHandler.class.getResourceAsStream(
                    "/assets/helium/textures/impactframe/" + folder + "/frame_" + currentIndex + ".png"
            );
            if (stream == null) break;

            try {
                NativeImage image = NativeImage.read(stream);
                final NativeImage finalImage = image;
                NativeImageBackedTexture texture = new NativeImageBackedTexture(
                        () -> "impact_" + folder + "_" + currentIndex, finalImage
                );
                client.getTextureManager().registerTexture(id, texture);
                ids.add(id);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { stream.close(); } catch (Exception ignored) {}
            }
            index++;
        }
        textureCache.put(folder, ids);
    }

    public static List<Identifier> getTextures(String folder) {
        return textureCache.getOrDefault(folder, List.of());
    }

    public static void addImpactEffect(String folder, int color, float startScale, float endScale,
                                       int durationMs, boolean isKill) {
        List<Identifier> frames = getTextures(folder);
        if (frames.isEmpty()) return;
        activeFrames.add(new ImpactFrame(durationMs, startScale, endScale, frames, color, isKill));
    }

    public static void update() {
        activeFrames.removeIf(ImpactFrame::isExpired);
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        for (ImpactFrame frame : activeFrames) {
            frame.render(context, screenWidth, screenHeight);
        }
    }
}