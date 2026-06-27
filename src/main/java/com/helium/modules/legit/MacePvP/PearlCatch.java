package com.helium.modules.legit.MacePvP;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class PearlCatch extends Module {
    private enum Stage { IDLE, THROW_PEARL, THROW_WIND, WAIT_COOLDOWN }
    private Stage stage = Stage.IDLE;
    private int timer = 0;
    private FindItemResult pearlSlot, windSlot;
    private Vec3d prevPos = null;

    private final SettingGroup sgTiming = settings.createGroup("Timing");

    private final Setting<Integer> windDelayTicks = sgTiming.add(new IntSetting.Builder()
            .name("wind-delay-ticks")
            .description("Ticks after throwing the pearl before throwing the wind charge (1 tick = 50 ms).")
            .defaultValue(1)
            .min(1).max(5).sliderMax(5)
            .build()
    );

    private final Setting<Integer> cooldownTicks = sgTiming.add(new IntSetting.Builder()
            .name("cooldown-ticks")
            .description("Ticks to wait after the wind charge before another catch can be attempted.")
            .defaultValue(10)
            .min(5).max(40).sliderMax(40)
            .build()
    );

    private final Setting<Boolean> disableOnCatch = sgTiming.add(new BoolSetting.Builder()
            .name("disable-on-catch")
            .description("Desactiva el módulo automáticamente cuando la perla aterriza (teletransporte exitoso).")
            .defaultValue(true)
            .build()
    );

    public PearlCatch() {
        super(HeliumAddon.LEGIT_MACE, "auto-pearl-catch", "Lanza una Ender Pearl y una Wind Charge para forzar un teletransporte inmediato.");
    }

    @Override
    public void onActivate() {
        stage = Stage.IDLE;
        timer = 0;
        prevPos = (mc.player != null) ? new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()) : null;
    }

    @Override
    public void onDeactivate() {
        stage = Stage.IDLE;
        timer = 0;
        prevPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        switch (stage) {
            case IDLE -> {
                pearlSlot = InvUtils.findInHotbar(Items.ENDER_PEARL);
                windSlot = InvUtils.findInHotbar(Items.WIND_CHARGE);
                if (pearlSlot.found() && windSlot.found()) {
                    InvUtils.swap(pearlSlot.slot(), false);
                    stage = Stage.THROW_PEARL;
                }
            }
            case THROW_PEARL -> {
                if (!InvUtils.findInHotbar(Items.ENDER_PEARL).found()) {
                    stage = Stage.IDLE;
                    return;
                }
                Rotations.rotate(mc.player.getYaw(), -90);
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                timer = windDelayTicks.get();
                stage = Stage.THROW_WIND;
            }
            case THROW_WIND -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                InvUtils.swap(windSlot.slot(), false);
                Rotations.rotate(mc.player.getYaw(), -90);
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                timer = cooldownTicks.get();
                stage = Stage.WAIT_COOLDOWN;
            }
            case WAIT_COOLDOWN -> {
                if (timer > 0) {
                    timer--;
                }

                Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                if (prevPos != null && currentPos.squaredDistanceTo(prevPos) > 0.25) {
                    if (disableOnCatch.get()) {
                        toggle();
                        return;
                    }
                }
                prevPos = currentPos;

                if (timer <= 0) {
                    stage = Stage.IDLE;
                }
            }
        }

        if (stage != Stage.WAIT_COOLDOWN && mc.player != null) {
            prevPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
    }
}