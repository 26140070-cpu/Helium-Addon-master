package com.helium.util;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    public static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> activeNotifications = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void add(String title, String message, long durationMs) {
        activeNotifications.add(new Notification(title, message, durationMs));
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.getWindow() == null || activeNotifications.isEmpty()) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int yOffset = screenHeight - 50;

        long now = System.currentTimeMillis();

        for (int i = 0; i < activeNotifications.size(); i++) {
            Notification notif = activeNotifications.get(i);

            int titleWidth = mc.textRenderer.getWidth(notif.title);
            int messageWidth = mc.textRenderer.getWidth(notif.message);
            int width = Math.max(titleWidth, messageWidth) + 25;
            int height = 30;

            double animProgress = 1.0;

            if (now - notif.startTime < 250) {
                animProgress = (double) (now - notif.startTime) / 250.0;
            } else if (notif.endTime - now < 250) {
                animProgress = (double) (notif.endTime - now) / 250.0;
            }

            animProgress = Math.max(0.0, Math.min(1.0, animProgress));
            double ease = 1.0 - Math.pow(1.0 - animProgress, 3);

            int xStart = (int) (screenWidth - (width + 10) * ease);
            int xEnd = xStart + width;

            event.drawContext.fill(xStart, yOffset, xEnd, yOffset + height, new Color(15, 15, 20, 185).getPacked());
            event.drawContext.fill(xStart, yOffset, xStart + 2, yOffset + height, new Color(255, 170, 0, 255).getPacked());

            event.drawContext.drawText(mc.textRenderer, notif.title, xStart + 8, yOffset + 4, new Color(255, 255, 255, 255).getPacked(), false);
            event.drawContext.drawText(mc.textRenderer, notif.message, xStart + 8, yOffset + 17, new Color(180, 180, 185, 255).getPacked(), false);

            double lifeProgress = (double) (notif.endTime - now) / (notif.endTime - notif.startTime);
            lifeProgress = Math.max(0.0, Math.min(1.0, lifeProgress));
            int progressBarWidth = (int) (width * lifeProgress);

            event.drawContext.fill(xStart, yOffset + height - 2, xStart + progressBarWidth, yOffset + height, new Color(255, 170, 0, 120).getPacked());

            yOffset -= (height + 6);
        }

        activeNotifications.removeIf(Notification::isExpired);
    }

    private static class Notification {
        String title;
        String message;
        long startTime;
        long endTime;

        public Notification(String title, String message, long durationMs) {
            this.title = title;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + durationMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > endTime;
        }
    }
}