package com.helium.modules.legit.UHC;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.MinecraftClientAccessor; // Asegúrate de que esta ruta coincida con tu Mixin
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TriggerBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgMovement = settings.createGroup("Movement & Combos");

    public enum TargetMode { Crosshair, Nearest }
    public enum TargetEntities { Players, Mobs, Both }
    public enum CritMode { Strict, None }

    private final Setting<TargetMode> targetMode = sgGeneral.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("How to find the target.").defaultValue(TargetMode.Crosshair).build());
    private final Setting<TargetEntities> targetEntities = sgGeneral.add(new EnumSetting.Builder<TargetEntities>().name("target-entities").description("What type of entities to attack.").defaultValue(TargetEntities.Both).build());
    private final Setting<CritMode> critMode = sgGeneral.add(new EnumSetting.Builder<CritMode>().name("crit-mode").description("Only attack when you can deal a critical hit.").defaultValue(CritMode.None).build());
    private final Setting<Boolean> autoStomp = sgGeneral.add(new BoolSetting.Builder().name("auto-stomp").description("Automatically switch to sword after landing two axe hits.").defaultValue(true).build());
    private final Setting<Boolean> onlyMouseHold = sgGeneral.add(new BoolSetting.Builder().name("only-mouse-hold").description("Only attack while left click is held.").defaultValue(false).build());

    private final Setting<Double> weaponThreshold = sgGeneral.add(new DoubleSetting.Builder().name("weapon-threshold").description("Weapon cooldown progress needed to hit (1.0 = fully charged).").defaultValue(0.92).min(0.1).max(1.0).build());
    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder().name("range").description("Max attack range.").defaultValue(3.5).min(2.0).sliderMax(6.0).build());
    private final Setting<Boolean> ignoreFriends = sgTargeting.add(new BoolSetting.Builder().name("ignore-friends").description("Don't attack Meteor friends.").defaultValue(true).build());

    private final Setting<Boolean> fallbackWTap = sgMovement.add(new BoolSetting.Builder().name("fallback-w-tap").description("Use basic W-Tap only if ComboTap is disabled.").defaultValue(true).build());
    private final Setting<Boolean> fallbackJump = sgMovement.add(new BoolSetting.Builder().name("fallback-jump").description("Use basic Jump only if ComboTap is disabled.").defaultValue(true).build());

    private int axeHits = 0;
    private Entity target;

    public TriggerBot() {
        super(HeliumAddon.LEGIT_UHC, "trigger-bot", "Triggerbot with Mixin Invoker, Auto-Stomp, and ComboTap integration.");
    }

    @Override
    public void onDeactivate() {
        axeHits = 0;
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;

        if (onlyMouseHold.get() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            return;
        }

        target = (targetMode.get() == TargetMode.Crosshair) ? getExtendedCrosshairTarget() : findNearestTarget();

        if (target == null || !isValidTarget(target)) return;

        if (mc.player.getAttackCooldownProgress(0.5f) < weaponThreshold.get()) return;
        if (!isCritConditionMet()) return;

        executeAttack(target);
    }

    private void executeAttack(Entity targetEntity) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.crosshairTarget == null || (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() != targetEntity)) {
            mc.crosshairTarget = new EntityHitResult(targetEntity);
        }

        ((MinecraftClientAccessor) mc).invokeDoAttack();

        handleStompMechanic();
        handleComboMechanics();
    }

    private void handleComboMechanics() {
        if (Modules.get().isActive(ComboTap.class)) return;

        if (fallbackWTap.get() && mc.player.isSprinting()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }

        if (fallbackJump.get() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    private void handleStompMechanic() {
        if (!autoStomp.get()) return;

        Item mainHand = mc.player.getMainHandStack().getItem();

        if (mainHand instanceof AxeItem) {
            axeHits++;
            if (axeHits >= 2) {
                FindItemResult sword = InvUtils.findInHotbar(itemStack -> itemStack.getItem().getTranslationKey().contains("sword"));
                if (sword.found()) {
                    InvUtils.swap(sword.slot(), false);
                }
                axeHits = 0;
            }
        } else if (mainHand.getTranslationKey().contains("sword")) {
            axeHits = 0;
        }
    }

    private boolean isCritConditionMet() {
        if (critMode.get() == CritMode.None) return true;

        return !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && mc.player.fallDistance > 0.0f
                && mc.player.getVehicle() == null;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity)) return false;

        if (targetEntities.get() == TargetEntities.Players && !(entity instanceof PlayerEntity)) return false;
        if (targetEntities.get() == TargetEntities.Mobs && entity instanceof PlayerEntity) return false;

        if (mc.player.distanceTo(entity) > range.get()) return false;

        if (entity instanceof PlayerEntity playerTarget) {
            if (ignoreFriends.get() && Friends.get().isFriend(playerTarget)) return false;
        }
        return true;
    }

    private Entity getExtendedCrosshairTarget() {
        double customRange = range.get();
        Vec3d cameraPos = mc.player.getCameraPosVec(1.0F);
        Vec3d rotationVec = mc.player.getRotationVec(1.0F);
        Vec3d endPos = cameraPos.add(rotationVec.x * customRange, rotationVec.y * customRange, rotationVec.z * customRange);

        Box box = mc.player.getBoundingBox().stretch(rotationVec.multiply(customRange)).expand(1.0D, 1.0D, 1.0D);

        EntityHitResult hitResult = ProjectileUtil.raycast(
                mc.player,
                cameraPos,
                endPos,
                box,
                entity -> !entity.isSpectator() && entity.canHit(),
                customRange * customRange
        );

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            return hitResult.getEntity();
        }
        return null;
    }

    private LivingEntity findNearestTarget() {
        LivingEntity best = null;
        double bestDist = range.get() * range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;

            double dist = mc.player.squaredDistanceTo(living);
            if (dist > bestDist) continue;

            bestDist = dist;
            best = living;
        }
        return best;
    }
}