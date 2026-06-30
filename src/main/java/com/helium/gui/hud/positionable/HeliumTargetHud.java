package com.helium.gui.hud.positionable;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import com.helium.util.render.HeliumRenderUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.helium.HeliumAddon.Helium_Hud;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HeliumTargetHud extends HudElement {
    public static final HudElementInfo<HeliumTargetHud> INFO = new HudElementInfo<>(
            Helium_Hud,
            "helium-target-hud",
            "A ClickCrystals-style Target HUD with advanced stats.",
            HeliumTargetHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> customScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the Target HUD.")
            .defaultValue(1.0)
            .min(0.5)
            .max(3.0)
            .sliderMin(0.5)
            .sliderMax(2.0)
            .build()
    );

    private final Setting<Double> roundness = sgGeneral.add(new DoubleSetting.Builder()
            .name("roundness")
            .description("The roundness of the corners.")
            .defaultValue(8.0)
            .min(0.0)
            .max(20.0)
            .sliderMin(0.0)
            .sliderMax(15.0)
            .build()
    );

    private final Setting<SettingColor> damageParticlesColor = sgGeneral.add(new ColorSetting.Builder()
            .name("damage-particles-color")
            .description("Color of the damage particles.")
            .defaultValue(new SettingColor(255, 50, 50, 255))
            .build()
    );

    private static final Map<String, Identifier> HEAD_CACHE = new HashMap<>();
    private static final Map<UUID, Integer> POP_COUNTERS = new HashMap<>();
    private final Random random = new Random();

    private LivingEntity target;
    private long timer = 0;
    private final long STAY_TIME = 4000;
    private float lastHp = -1;

    private final Color cBg = new Color(15, 15, 22, 210);
    private final Color cText = new Color(255, 255, 255, 255);
    private final Color cSubText = new Color(180, 185, 195, 255);
    private final Color cHealth = new Color(45, 240, 45, 255);
    private final Color cAbsorb = new Color(255, 215, 0, 255);
    private final Color cGray = new Color(70, 70, 75, 255);
    private final Color cBlack = new Color(0, 0, 0, 255);
    private final Color cPopCount = new Color(255, 60, 60, 255);

    public HeliumTargetHud() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event.entity instanceof LivingEntity entity) {
            target = entity;
            timer = System.currentTimeMillis() + STAY_TIME;
            lastHp = target.getHealth();

            if (entity instanceof PlayerEntity player) {
                fetchMinotarHead(player.getName().getString());
                POP_COUNTERS.putIfAbsent(player.getUuid(), 0);
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet && mc.world != null) {
            if (packet.getStatus() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity player) {
                    UUID uuid = player.getUuid();
                    POP_COUNTERS.put(uuid, POP_COUNTERS.getOrDefault(uuid, 0) + 1);

                    if (target != null && target.getUuid().equals(uuid)) {
                        timer = System.currentTimeMillis() + STAY_TIME;
                    }
                }
            }
        }
    }

    private void fetchMinotarHead(String playerName) {
        if (HEAD_CACHE.containsKey(playerName)) return;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create("https://minotar.net/helm/" + playerName + "/64.png").toURL();
                InputStream stream = url.openStream();
                NativeImage image = NativeImage.read(stream);

                mc.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "minotar", image);
                    Identifier id = Identifier.of("helium", "minotar_" + playerName.toLowerCase());
                    mc.getTextureManager().registerTexture(id, texture);
                    HEAD_CACHE.put(playerName, id);
                });
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void tick(HudRenderer renderer) {
        super.tick(renderer);
        if (target != null && (target.isDead() || target.getHealth() <= 0 || target.isRemoved() || System.currentTimeMillis() > timer)) {
            target = null;
            lastHp = -1;
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        DrawContext context = renderer.drawContext;
        if (context == null) return;

        double s = customScale.get();
        double rad = roundness.get() * s;

        if (target != null) {
            setSize(220 * s, 125 * s);
        } else {
            setSize(130 * s, 26 * s);
        }

        double renderX = this.x;
        double renderY = this.y;

        if (target != null) {
            double w = 220 * s;
            double h = 125 * s;

            HeliumRenderUtils.drawRoundRect(renderer, renderX, renderY, w, h, rad, cBg);

            double marginX = renderX + (8 * s);
            double caretY = renderY + (8 * s);
            double headSize = 34 * s;

            String entityName = target.getName().getString();
            Identifier headId = target instanceof PlayerEntity ? HEAD_CACHE.get(entityName) : null;

            if (headId != null) {
                renderer.texture(headId, marginX, caretY, headSize, headSize, Color.WHITE);
            } else {
                HeliumRenderUtils.drawRoundRect(renderer, marginX, caretY, headSize, headSize, 4 * s, cGray);
            }

            renderer.text(entityName, marginX + (40 * s), caretY, cText, true);

            int ping = 0;
            String brand = "Vanilla";
            ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
            if (target instanceof PlayerEntity player && networkHandler != null) {
                PlayerListEntry entry = networkHandler.getPlayerListEntry(player.getUuid());
                if (entry != null) ping = entry.getLatency();
                if (player.equals(mc.player)) {
                    brand = mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand() : "Vanilla";
                } else if (networkHandler.getBrand() != null) {
                    brand = networkHandler.getBrand();
                }
            }

            if (brand.contains(":")) brand = brand.split(":")[1];
            brand = StringUtils.capitalize(brand);

            double dist = mc.player != null ? mc.player.distanceTo(target) : 0;
            double bps = Math.hypot(target.getVelocity().x, target.getVelocity().z) * 20.0;

            String info = ping + " ms | " + (Math.round(dist * 10.0) / 10.0) + " m | " + (Math.round(bps * 10.0) / 10.0) + " BPS";
            renderer.text(info, marginX + (40 * s), caretY + (13 * s), cSubText, true);

            String state = "Standing";
            if (target.isSwimming()) state = "Swimming";
            else if (target.isSprinting()) state = "Sprinting";
            else if (target.isSneaking()) state = "Sneaking";

            renderer.text(state + " | " + brand, marginX + (40 * s), caretY + (23 * s), cSubText, true);

            final double fTotemX = renderX + w - (52 * s);
            final double fTotemY = renderY + (6 * s);
            final float totemScaleFactor = (float) (2.3f * s);

            renderer.post(() -> RenderUtils.drawItem(context, Items.TOTEM_OF_UNDYING.getDefaultStack(), (int) fTotemX, (int) fTotemY, totemScaleFactor, true));

            int pops = POP_COUNTERS.getOrDefault(target.getUuid(), 0);
            String popText = "Pops: " + pops;
            double popTextWidth = renderer.textWidth(popText);
            renderer.text(popText, fTotemX - (popTextWidth / 2) + (32 * s), fTotemY + (44 * s), cPopCount, true);

            double itemY = renderY + (58 * s);
            List<ItemStack> items = new ArrayList<>();
            items.add(target.getMainHandStack());
            items.add(target.getEquippedStack(EquipmentSlot.HEAD));
            items.add(target.getEquippedStack(EquipmentSlot.CHEST));
            items.add(target.getEquippedStack(EquipmentSlot.LEGS));
            items.add(target.getEquippedStack(EquipmentSlot.FEET));
            items.add(target.getOffHandStack());

            double itemX = marginX;
            for (ItemStack item : items) {
                if (item.isEmpty()) continue;

                final double fItemX = itemX;
                final double fItemY = itemY;
                renderer.post(() -> RenderUtils.drawItem(context, item, (int) fItemX, (int) fItemY, (float) s, true));

                if (item.isDamageable()) {
                    int durability = 100 - (int) ((item.getDamage() * 100.0f) / item.getMaxDamage());
                    Color durColor = durability > 50 ? new Color(85, 255, 85) : (durability > 20 ? new Color(255, 255, 85) : new Color(255, 85, 85));
                    renderer.text(durability + "%", itemX, itemY + (16 * s), durColor, true);
                }
                itemX += (24 * s);
            }

            double healthBarY = renderY + (104 * s);
            double barWidth = w - (16 * s);
            double barHeight = 8 * s;
            double rBar = Math.max(0, rad - (4 * s));

            float maxHp = target.getMaxHealth();
            float hp = target.getHealth();
            float absorb = target.getAbsorptionAmount();

            double hpRatio = MathHelper.clamp(hp / maxHp, 0, 1);
            double absorbRatio = MathHelper.clamp(absorb / maxHp, 0, 1);

            HeliumRenderUtils.drawRoundRect(renderer, marginX, healthBarY, barWidth, barHeight, rBar, cBlack);
            HeliumRenderUtils.drawRoundRect(renderer, marginX, healthBarY, barWidth * hpRatio, barHeight, rBar, cHealth);

            if (lastHp != -1 && hp < lastHp) {
                double damageRatio = MathHelper.clamp((lastHp - hp) / maxHp, 0, 1);
                double pX = marginX + (barWidth * hpRatio);
                double pWidth = Math.min(barWidth * damageRatio, barWidth - (barWidth * hpRatio));

                Color pColor = new Color(damageParticlesColor.get());
                pColor.a = 180;
                HeliumRenderUtils.drawRoundRect(renderer, pX, healthBarY, pWidth, barHeight, 0, pColor);

                double particleOriginX = marginX + (barWidth * hpRatio);
                for (int i = 0; i < 15; i++) {
                    double pSize = (1.5 + random.nextDouble() * 2.0) * s;
                    double offsetX = (random.nextDouble() - 0.5) * (10.0 * s);
                    double offsetY = (random.nextDouble() - 0.5) * (12.0 * s);

                    Color singleParticleColor = new Color(damageParticlesColor.get());
                    singleParticleColor.a = 150 + random.nextInt(105);

                    HeliumRenderUtils.drawRoundRect(
                            renderer,
                            particleOriginX + offsetX,
                            healthBarY + (barHeight / 2) + offsetY,
                            pSize,
                            pSize,
                            0,
                            singleParticleColor
                    );
                }
            }

            if (absorb > 0) {
                HeliumRenderUtils.drawRoundRect(renderer, marginX + (barWidth * hpRatio), healthBarY, Math.min(barWidth * absorbRatio, barWidth - (barWidth * hpRatio)), barHeight, rBar, cAbsorb);
            }

            lastHp = hp;

        } else {
            double w = 130 * s;
            double h = 26 * s;

            HeliumRenderUtils.drawRoundRect(renderer, renderX, renderY, w, h, rad, cBg);

            String text = "Not in combat";
            double textW = renderer.textWidth(text);
            renderer.text(text, renderX + (w - textW) / 2, renderY + (h - 9 * s) / 2, cSubText, true);
        }
    }
}