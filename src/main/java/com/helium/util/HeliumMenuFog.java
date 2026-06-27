package com.helium.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import java.io.InputStream;

public class HeliumMenuFog {
    private static final Identifier FOG_TEXTURE_ID = Identifier.of("helium", "menu_fog");
    private static boolean textureLoaded = false;

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!HeliumMenuFogConfig.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager tm = client.getTextureManager();

        if (!textureLoaded) {
            try (InputStream stream = HeliumMenuFog.class.getResourceAsStream("/assets/helium/textures/fog.png")) {
                if (stream != null) {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(
                            () -> "menu_fog",
                            net.minecraft.client.texture.NativeImage.read(stream)
                    );
                    tm.registerTexture(FOG_TEXTURE_ID, texture);
                    textureLoaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (textureLoaded) {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    FOG_TEXTURE_ID,
                    0, 0, 0, 0,
                    screenWidth, screenHeight,
                    screenWidth, screenHeight
            );
        }
    }
}