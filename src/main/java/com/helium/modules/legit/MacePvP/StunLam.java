package com.helium.modules.legit.MacePvP;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class StunLam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Max distance to detect a blocking player below you.")
            .defaultValue(6.0)
            .min(1.0)
            .max(10.0)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switch to axe and then to mace.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> switchBackDelay = sgGeneral.add(new IntSetting.Builder()
            .name("switch-back-delay")
            .description("Ticks before switching back to the original weapon after the combo.")
            .defaultValue(4)
            .min(0)
            .max(20)
            .sliderMax(20)
            .visible(autoSwitch::get)
            .build()
    );

    private enum State { IDLE, AXE_ATTACK, MACE_ATTACK, RESET }
    private State state = State.IDLE;
    private PlayerEntity currentTarget = null;
    private int originalSlot = -1;
    private int switchBackTicks = 0;

    public StunLam() {
        super(HeliumAddon.LEGIT_MACE, "AutoStunlam", "Automatically breaks shield with an axe and finishes with a mace slam using real fall distance.");
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        state = State.IDLE;
        currentTarget = null;
        switchBackTicks = 0;
        if (originalSlot != -1 && mc.player != null) {
            InvUtils.swapBack();
            originalSlot = -1;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE -> {
                currentTarget = findBlockingTargetBelow();
                if (currentTarget != null) {
                    if (autoSwitch.get()) {
                        FindItemResult axe = findAxe();
                        if (axe.found()) {
                            originalSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
                            InvUtils.swap(axe.slot(), false);
                        }
                    }
                    attackWithAxe(currentTarget);
                    state = State.AXE_ATTACK;
                }
            }
            case AXE_ATTACK -> {
                if (autoSwitch.get()) {
                    FindItemResult mace = findMace();
                    if (mace.found()) {
                        InvUtils.swap(mace.slot(), false);
                    }
                }
                attackWithMace(currentTarget);
                switchBackTicks = switchBackDelay.get();
                state = State.RESET;
            }
            case RESET -> {
                if (switchBackTicks > 0) {
                    switchBackTicks--;
                } else {
                    if (autoSwitch.get() && originalSlot != -1) {
                        InvUtils.swapBack();
                        originalSlot = -1;
                    }
                    state = State.IDLE;
                }
            }
        }
    }

    private PlayerEntity findBlockingTargetBelow() {
        if (!isAirborne()) return null;

        Vec3d myPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;
            if (!player.isBlocking()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > range.get()) continue;

            Vec3d theirPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            if (theirPos.y >= myPos.y) continue;           
            double horizontalDist = Math.hypot(myPos.x - theirPos.x, myPos.z - theirPos.z);
            if (horizontalDist > 2.0) continue;            

            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private boolean isAirborne() {
        return !mc.player.isOnGround();   
    }

    private void attackWithAxe(PlayerEntity target) {
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void attackWithMace(PlayerEntity target) {
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private FindItemResult findAxe() {
        return InvUtils.findInHotbar(stack -> stack.getItem() instanceof AxeItem);
    }

    private FindItemResult findMace() {
        return InvUtils.findInHotbar(stack -> stack.getItem() instanceof MaceItem);
    }
}