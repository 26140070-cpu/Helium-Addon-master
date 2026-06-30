package com.helium.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import com.helium.HeliumAddon;
import java.util.ArrayList;
import java.util.List;

public class JumpCircles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> ringColor = sgGeneral.add(new ColorSetting.Builder().name("color").description("Color of the glowing expansion rings.").defaultValue(new SettingColor(255, 125, 0, 255)).build());

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = false;

    public JumpCircles() {
        super(HeliumAddon.CATEGORY, "jump-circles", "Renders premium, high-fidelity glowing expansion rings.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (wasOnGround && !mc.player.isOnGround() && mc.player.getVelocity().y > 0) {
            circles.add(new Circle(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        }
        wasOnGround = mc.player.isOnGround();

        circles.removeIf(Circle::isExpired);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Circle circle : circles) {
            circle.render(event, ringColor.get());
        }
    }

    private static class Circle {
        private final Vec3d pos;
        private final long startTime;
        private static final long MAX_TIME = 600;

        public Circle(Vec3d pos) {
            this.pos = pos;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > MAX_TIME;
        }

        public void render(Render3DEvent event, SettingColor colorSetting) {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = (double) elapsed / MAX_TIME;

            double easeOutCubic = 1.0 - Math.pow(1.0 - progress, 3);

            double baseRadius = easeOutCubic * 2.2;
            double ringThickness = 0.3 * (1.0 - progress);

            int mainAlpha = (int) (130 * (1.0 - Math.pow(progress, 2)));
            int glowAlpha = (int) (60 * (1.0 - progress));

            if (mainAlpha <= 0) return;

            int r = colorSetting.r;
            int g = colorSetting.g;
            int b = colorSetting.b;

            Color mainColor = new Color(r, g, b, mainAlpha);
            Color glowColor = new Color(r, g, b, glowAlpha);

            drawFilledRing(event, baseRadius, ringThickness, mainColor);
            drawFilledRing(event, baseRadius + 0.15, ringThickness + 0.2, glowColor);
        }

        private void drawFilledRing(Render3DEvent event, double radius, double thickness, Color color) {
            double innerR = radius - (thickness / 2.0);
            double outerR = radius + (thickness / 2.0);

            if (innerR < 0) innerR = 0;

            int quality = 40;
            double renderY = pos.y + 0.015;

            for (int i = 0; i < quality; i++) {
                double angle1 = Math.toRadians((i * 360.0) / quality);
                double angle2 = Math.toRadians(((i + 1) * 360.0) / quality);

                double x1Inner = pos.x + Math.sin(angle1) * innerR;
                double z1Inner = pos.z + Math.cos(angle1) * innerR;

                double x1Outer = pos.x + Math.sin(angle1) * outerR;
                double z1Outer = pos.z + Math.cos(angle1) * outerR;

                double x2Inner = pos.x + Math.sin(angle2) * innerR;
                double z2Inner = pos.z + Math.cos(angle2) * innerR;

                double x2Outer = pos.x + Math.sin(angle2) * outerR;
                double z2Outer = pos.z + Math.cos(angle2) * outerR;

                event.renderer.quad(
                        x1Inner, renderY, z1Inner,
                        x1Outer, renderY, z1Outer,
                        x2Outer, renderY, z2Outer,
                        x2Inner, renderY, z2Inner,
                        color
                );
            }
        }
    }
}