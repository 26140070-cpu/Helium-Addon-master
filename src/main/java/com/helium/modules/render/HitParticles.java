package com.helium.modules.render;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import com.helium.HeliumAddon;
import java.util.ArrayList;
import java.util.List;

public class HitParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> particleCount = sgGeneral.add(new IntSetting.Builder().name("particle-count").description("Amount of particles to spawn per hit.").defaultValue(12).min(1).max(50).build());
    private final Setting<Double> particleSize = sgGeneral.add(new DoubleSetting.Builder().name("particle-size").description("Size of the custom particles.").defaultValue(0.4).min(0.1).max(2.0).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("color").description("Color of the hit particles.").defaultValue(new SettingColor(255, 30, 50, 255)).build());
    private final Setting<Boolean> cancelDefault = sgGeneral.add(new BoolSetting.Builder().name("cancel-default").description("Cancels default Minecraft critical hit particles.").defaultValue(true).build());

    private final List<CustomParticle> particles = new ArrayList<>();

    private static final float[][] STAR_V = {{0, 0.6f, 0}, {0.15f, 0, 0}, {0, -0.6f, 0}, {-0.15f, 0, 0}};
    private static final float[][] STAR_H = {{0.6f, 0, 0}, {0, 0.15f, 0}, {-0.6f, 0, 0}, {0, -0.15f, 0}};
    private static final float[][] HEART_L = {{0, -0.5f, 0}, {-0.5f, 0.2f, 0}, {-0.25f, 0.5f, 0}, {0, 0.2f, 0}};
    private static final float[][] HEART_R = {{0, -0.5f, 0}, {0, 0.2f, 0}, {0.25f, 0.5f, 0}, {0.5f, 0.2f, 0}};

    public HitParticles() {
        super(HeliumAddon.CATEGORY, "hit-particles", "Spawns premium 3D stars and hearts when attacking entities.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        Entity target = event.entity;
        if (target == null) return;

        Vec3d spawnPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ());

        for (int i = 0; i < particleCount.get(); i++) {
            particles.add(new CustomParticle(spawnPos));
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (cancelDefault.get() && event.packet instanceof EntityAnimationS2CPacket packet) {
            if (packet.getAnimationId() == 4 || packet.getAnimationId() == 5) {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onParticle(ParticleEvent event) {
        if (cancelDefault.get()) {
            String name = event.particle.getClass().getSimpleName();
            if (name.contains("Damage") || name.contains("Crit") || name.contains("Enchanted")) {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        particles.forEach(CustomParticle::tick);
        particles.removeIf(CustomParticle::isExpired);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (CustomParticle p : particles) {
            float progress = (float) (System.currentTimeMillis() - p.startTime) / p.maxLife;
            int alpha = (int) (color.get().a * (1.0f - progress));

            if (alpha <= 0) continue;

            Color c = new Color(color.get().r, color.get().g, color.get().b, alpha);

            if (p.isStar) {
                drawQuad(event, p.pos, p.rot, particleSize.get(), STAR_V, c);
                drawQuad(event, p.pos, p.rot, particleSize.get(), STAR_H, c);
            } else {
                drawQuad(event, p.pos, p.rot, particleSize.get(), HEART_L, c);
                drawQuad(event, p.pos, p.rot, particleSize.get(), HEART_R, c);
            }
        }
    }

    private void drawQuad(Render3DEvent event, Vec3d pos, Vector3f rot, double scale, float[][] vertices, Color color) {
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            p[i] = new Vector3f(vertices[i][0] * (float) scale, vertices[i][1] * (float) scale, vertices[i][2] * (float) scale);
            p[i].rotateX(rot.x).rotateY(rot.y).rotateZ(rot.z);
        }

        event.renderer.quad(
                pos.getX() + p[0].x, pos.getY() + p[0].y, pos.getZ() + p[0].z,
                pos.getX() + p[1].x, pos.getY() + p[1].y, pos.getZ() + p[1].z,
                pos.getX() + p[2].x, pos.getY() + p[2].y, pos.getZ() + p[2].z,
                pos.getX() + p[3].x, pos.getY() + p[3].y, pos.getZ() + p[3].z,
                color
        );

        event.renderer.quad(
                pos.getX() + p[3].x, pos.getY() + p[3].y, pos.getZ() + p[3].z,
                pos.getX() + p[2].x, pos.getY() + p[2].y, pos.getZ() + p[2].z,
                pos.getX() + p[1].x, pos.getY() + p[1].y, pos.getZ() + p[1].z,
                pos.getX() + p[0].x, pos.getY() + p[0].y, pos.getZ() + p[0].z,
                color
        );
    }

    private static class CustomParticle {
        public Vec3d pos;
        public Vec3d vel;
        public Vector3f rot;
        public Vector3f rotSpd;
        public final long startTime;
        public final int maxLife;
        public final boolean isStar;

        public CustomParticle(Vec3d startPos) {
            this.pos = startPos;
            this.startTime = System.currentTimeMillis();

            this.vel = new Vec3d(
                    (Math.random() - 0.5) * 0.6,
                    Math.random() * 0.4 + 0.1,
                    (Math.random() - 0.5) * 0.6
            );

            this.rot = new Vector3f(
                    (float) (Math.random() * Math.PI * 2),
                    (float) (Math.random() * Math.PI * 2),
                    (float) (Math.random() * Math.PI * 2)
            );

            this.rotSpd = new Vector3f(
                    (float) ((Math.random() - 0.5) * 0.3),
                    (float) ((Math.random() - 0.5) * 0.3),
                    (float) ((Math.random() - 0.5) * 0.3)
            );

            this.maxLife = 600 + (int) (Math.random() * 400);
            this.isStar = Math.random() > 0.5;
        }

        public void tick() {
            pos = pos.add(vel);
            vel = new Vec3d(vel.getX() * 0.85, vel.getY() * 0.85 - 0.03, vel.getZ() * 0.85);
            rot.add(rotSpd);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > maxLife;
        }
    }
}