package com.helium.util.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import java.util.List;

public class ImpactFrame {
    private final long startTime;
    private final int totalDurationMs;
    private final float startScale, endScale;
    private final List<Identifier> textureFrames;
    private final int frameCount;
    private final int color;
    private final boolean isKillEffect;

    public ImpactFrame(int totalDurationMs, float startScale, float endScale,
                       List<Identifier> textureFrames, int color, boolean isKillEffect) {
        this.startTime = System.currentTimeMillis();
        this.totalDurationMs = totalDurationMs;
        this.startScale = startScale;
        this.endScale = endScale;
        this.textureFrames = textureFrames;
        this.frameCount = textureFrames.size();
        this.color = color;
        this.isKillEffect = isKillEffect;
    }

    public float getProgress() {
        float progress = (float)(System.currentTimeMillis() - startTime) / totalDurationMs;
        return Math.min(1.0f, Math.max(0.0f, progress));
    }

    public int getCurrentFrameIndex() {
        int index = (int)(getProgress() * frameCount);
        return Math.min(index, frameCount - 1);
    }

    public Identifier getCurrentTexture() {
        if (frameCount == 0) return null;
        return textureFrames.get(getCurrentFrameIndex());
    }

    public float getCurrentScale() {
        float progress = getProgress();
        return startScale + (endScale - startScale) * progress;
    }

    public int getCurrentAlpha() {
        float progress = getProgress();
        float fadeProgress = Math.min(1.0f, progress / 0.8f);
        int baseAlpha = (color >> 24) & 0xFF;
        return (int)(baseAlpha * (1.0f - fadeProgress));
    }

    public int getCurrentColor() {
        return (getCurrentAlpha() << 24) | (color & 0x00FFFFFF);
    }

    public boolean isExpired() {
        return getProgress() >= 1.0f;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (isKillEffect) {
            Identifier texture = getCurrentTexture();
            if (texture != null) {
                int color = getCurrentColor();
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        0, 0, 0, 0,
                        screenWidth, screenHeight,
                        screenWidth, screenHeight
                );
                int alpha = (color >> 24) & 0xFF;
                if (alpha > 0) {
                    int rgb = color & 0x00FFFFFF;
                    int tintColor = (alpha << 24) | rgb;
                    context.fill(0, 0, screenWidth, screenHeight, tintColor);
                }
            } else {
                context.fill(0, 0, screenWidth, screenHeight, getCurrentColor());
            }
            return;
        }

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int frameSize = (int)(screenWidth * getCurrentScale());
        int x = centerX - frameSize / 2;
        int y = centerY - frameSize / 2;
        Identifier texture = getCurrentTexture();
        if (texture == null) return;
        int currentColor = getCurrentColor();

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x, y, 0, 0,
                frameSize, frameSize,
                frameSize, frameSize
        );

        int alpha = (currentColor >> 24) & 0xFF;
        if (alpha > 0) {
            int rgb = currentColor & 0x00FFFFFF;
            int tintColor = (alpha << 24) | rgb;
            context.fill(x, y, x + frameSize, y + frameSize, tintColor);
        }
    }
}