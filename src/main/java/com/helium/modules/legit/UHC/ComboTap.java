package com.helium.modules.legit.UHC;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;

public class ComboTap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> chance = sgGeneral.add(new IntSetting.Builder()
            .name("chance")
            .description("Chance (%) to trigger the W+S Tap combo.")
            .defaultValue(100)
            .min(1)
            .max(100)
            .sliderMax(100)
            .build()
    );

    private final Setting<Integer> msDelay = sgGeneral.add(new IntSetting.Builder()
            .name("delay-ms")
            .description("Milliseconds to hold 'S' and release 'W' after a hit.")
            .defaultValue(60)
            .min(1)
            .max(500)
            .sliderMax(500)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only execute the combo when you are on the ground.")
            .defaultValue(true)
            .build()
    );

    private boolean isComboActive = false;
    private long timer = 0L;

    public ComboTap() {
        super(HeliumAddon.LEGIT_UHC, "combo-tap", "Performs WTAP and STAP simultaneously for max knockback.");
    }

    @Override
    public void onDeactivate() {
        if (isComboActive && mc.player != null) {
            mc.options.backKey.setPressed(false);
            mc.options.forwardKey.setPressed(Input.isKeyPressed(GLFW.GLFW_KEY_W));
            isComboActive = false;
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null) return;

        if (Math.random() * 100 > chance.get()) return;
        if (!mc.player.isOnGround() && onlyOnGround.get()) return;

        Entity target = event.entity;
        if (target == null || !target.isAlive()) return;

        if (!Input.isKeyPressed(GLFW.GLFW_KEY_W)) return;

        if (mc.player.isSprinting() && !isComboActive) {

            isComboActive = true;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(true);
            timer = System.currentTimeMillis();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !isComboActive) return;

        // si el jugador deja de presionar W finaliza el combito
        if (!Input.isKeyPressed(GLFW.GLFW_KEY_W)) {
            mc.options.backKey.setPressed(false);
            isComboActive = false;
            return;
        }

        if (System.currentTimeMillis() - timer >= msDelay.get()) {
            mc.options.backKey.setPressed(false);
            mc.options.forwardKey.setPressed(true);
            isComboActive = false; // finaliza el combito
        }
    }
}