package com.helium.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class HeliumScreenBackground {
    private static Clip backgroundClip;
    private static Identifier backgroundTextureId = null;

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!HeliumBackgroundConfig.enabled || HeliumBackgroundConfig.imageFileName.isEmpty()) {
            stopAudio();
            return;
        }

        if (renderImage(context, screenWidth, screenHeight)) {
            if (!HeliumBackgroundConfig.audioFileName.isEmpty() && (backgroundClip == null || !backgroundClip.isActive())) {
                playAudio();
            }
        } else {
            context.fill(0, 0, screenWidth, screenHeight, 0xAAFF00FF);
        }
    }

    private static boolean renderImage(DrawContext context, int screenWidth, int screenHeight) {
        try {
            File imageFile = new File(HeliumBackgroundConfig.BG_DIR, HeliumBackgroundConfig.imageFileName);
            if (!imageFile.exists()) return false;

            NativeImage nativeImage;

            try (FileInputStream fis = new FileInputStream(imageFile)) {
                nativeImage = NativeImage.read(fis);
            } catch (IOException e) {
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                if (bufferedImage == null) return false;
                nativeImage = bufferedImageToNative(bufferedImage);
            }

            if (nativeImage == null) return false;

            backgroundTextureId = Identifier.of("helium", "background");
            TextureManager tm = MinecraftClient.getInstance().getTextureManager();

            if (tm.getTexture(backgroundTextureId) != null) {
                tm.destroyTexture(backgroundTextureId);
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "helium_background", nativeImage);
            tm.registerTexture(backgroundTextureId, texture);

            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    backgroundTextureId,
                    0, 0, 0, 0,
                    screenWidth, screenHeight,
                    screenWidth, screenHeight
            );
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static NativeImage bufferedImageToNative(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int argb = bufferedImage.getRGB(x, y);
                nativeImage.setColor(x, y, argb);
            }
        }
        return nativeImage;
    }

    private static void playAudio() {
        stopAudio();
        File audioFile = new File(HeliumBackgroundConfig.BG_DIR, HeliumBackgroundConfig.audioFileName);
        if (!audioFile.exists()) return;

        new Thread(() -> {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat baseFormat = stream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, stream);
                Clip clip = AudioSystem.getClip();
                clip.open(din);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log10(0.8) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }

                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
                backgroundClip = clip;
            } catch (Exception ignored) {}
        }, "Helium-BG-Audio").start();
    }

    public static void ensureAudio() {
        if (!HeliumBackgroundConfig.audioFileName.isEmpty() && (backgroundClip == null || !backgroundClip.isActive())) {
            playAudio();
        }
    }

    public static void stopAudio() {
        if (backgroundClip != null) {
            try {
                backgroundClip.stop();
                backgroundClip.close();
            } catch (Exception ignored) {}
            backgroundClip = null;
        }
    }
}