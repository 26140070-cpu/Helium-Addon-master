package com.helium.modules.combat;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import com.helium.util.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class HeliumCrystalAura extends Module {
    public static volatile int lastEntityId = -1;

    private final SettingGroup sgGeneral    = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced   = settings.createGroup("Advanced");
    private final SettingGroup sgRotations  = settings.createGroup("Rotations");
    private final SettingGroup sgPlace      = settings.createGroup("Place");
    private final SettingGroup sgBreak      = settings.createGroup("Break");
    private final SettingGroup sgDamage     = settings.createGroup("Damage");
    private final SettingGroup sgTargets    = settings.createGroup("Targets");
    private final SettingGroup sgPause      = settings.createGroup("Pause");
    private final SettingGroup sgRender     = settings.createGroup("Render");

    private final Setting<Double> targetRange        = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(10.0).min(1.0).sliderMax(15.0).build());
    private final Setting<Boolean> autoRefill        = sgGeneral.add(new BoolSetting.Builder().name("auto-refill").defaultValue(true).build());
    private final Setting<Boolean> debugMode         = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());

    private final Setting<Boolean> multithreading    = sgAdvanced.add(new BoolSetting.Builder().name("multithreading").defaultValue(true).build());
    private final Setting<Boolean> fastCrystal       = sgAdvanced.add(new BoolSetting.Builder().name("fast-crystal").defaultValue(true).build());
    private final Setting<Boolean> predictId         = sgAdvanced.add(new BoolSetting.Builder().name("predict-id").defaultValue(true).visible(fastCrystal::get).build());
    private final Setting<Integer> maxPackets        = sgAdvanced.add(new IntSetting.Builder().name("max-packets").defaultValue(6).min(1).max(20).build());
    private final Setting<Boolean> allowMultiPop     = sgAdvanced.add(new BoolSetting.Builder().name("multi-pop").defaultValue(true).build());
    private final Setting<Boolean> inhibit           = sgAdvanced.add(new BoolSetting.Builder().name("inhibit").defaultValue(true).build());

    private final Setting<Boolean> rotate            = sgRotations.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<YawStepMode> yawStepMode   = sgRotations.add(new EnumSetting.Builder<YawStepMode>().name("yaw-step").defaultValue(YawStepMode.SEMI).visible(rotate::get).build());
    private final Setting<Integer> yawStepLimit      = sgRotations.add(new IntSetting.Builder().name("yaw-step-limit").defaultValue(180).min(1).sliderMax(180).visible(() -> rotate.get() && yawStepMode.get() != YawStepMode.OFF).build());
    private final Setting<Boolean> rotationMovementFix = sgRotations.add(new BoolSetting.Builder().name("movement-fix").defaultValue(true).visible(rotate::get).build());

    private final Setting<Boolean> doPlace           = sgPlace.add(new BoolSetting.Builder().name("place").defaultValue(true).build());
    private final Setting<Double> placeRange         = sgPlace.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.5).min(0.1).max(6.0).visible(doPlace::get).build());
    private final Setting<Double> placeWallRange     = sgPlace.add(new DoubleSetting.Builder().name("place-wall-range").defaultValue(4.5).min(0.1).max(6.0).visible(doPlace::get).build());
    private final Setting<Boolean> placeRangeEye     = sgPlace.add(new BoolSetting.Builder().name("place-range-eye").defaultValue(false).visible(doPlace::get).build());
    private final Setting<Boolean> placeRangeCenter  = sgPlace.add(new BoolSetting.Builder().name("place-range-center").defaultValue(true).visible(doPlace::get).build());
    private final Setting<Integer> multiPlace        = sgPlace.add(new IntSetting.Builder().name("multi-place").defaultValue(1).min(1).max(3).visible(doPlace::get).build());
    private final Setting<Integer> placeDelay        = sgPlace.add(new IntSetting.Builder().name("place-delay").defaultValue(0).min(0).visible(doPlace::get).build());
    private final Setting<Boolean> strictDirection   = sgPlace.add(new BoolSetting.Builder().name("strict-direction").defaultValue(false).visible(doPlace::get).build());
    private final Setting<ForcePlace> forcePlace     = sgPlace.add(new EnumSetting.Builder<ForcePlace>().name("force-place").defaultValue(ForcePlace.PRE).visible(doPlace::get).build());
    private final Setting<Boolean> sequential        = sgPlace.add(new BoolSetting.Builder().name("sequential").defaultValue(true).visible(doPlace::get).build());

    private final Setting<Boolean> doBreak           = sgBreak.add(new BoolSetting.Builder().name("break").defaultValue(true).build());
    private final Setting<Double> breakSpeed         = sgBreak.add(new DoubleSetting.Builder().name("break-speed").defaultValue(18.0).min(0.1).sliderMax(20.0).visible(doBreak::get).build());
    private final Setting<Double> attackDelay        = sgBreak.add(new DoubleSetting.Builder().name("attack-delay").defaultValue(0.0).min(0.0).sliderMax(5.0).visible(doBreak::get).build());
    private final Setting<Integer> attackFactor      = sgBreak.add(new IntSetting.Builder().name("attack-factor").defaultValue(0).min(0).sliderMax(3).visible(() -> doBreak.get() && attackDelay.get() > 0.0).build());
    private final Setting<Double> attackLimit        = sgBreak.add(new DoubleSetting.Builder().name("attack-limit").defaultValue(1.5).min(0.5).sliderMax(20.0).visible(doBreak::get).build());
    private final Setting<Boolean> breakDelayAdapt   = sgBreak.add(new BoolSetting.Builder().name("adaptive-break-delay").defaultValue(true).visible(doBreak::get).build());
    private final Setting<Double> breakTimeout       = sgBreak.add(new DoubleSetting.Builder().name("break-timeout").defaultValue(3.0).min(0.0).sliderMax(10.0).visible(() -> doBreak.get() && breakDelayAdapt.get()).build());
    private final Setting<Double> minBreakTimeout    = sgBreak.add(new DoubleSetting.Builder().name("min-break-timeout").defaultValue(5.0).min(0.0).sliderMax(20.0).visible(() -> doBreak.get() && breakDelayAdapt.get()).build());
    private final Setting<Integer> ticksExisted      = sgBreak.add(new IntSetting.Builder().name("ticks-existed").defaultValue(0).min(0).sliderMax(10).visible(doBreak::get).build());
    private final Setting<Double> breakRange         = sgBreak.add(new DoubleSetting.Builder().name("break-range").defaultValue(4.5).min(0.1).max(6.0).visible(doBreak::get).build());
    private final Setting<Double> breakWallRange     = sgBreak.add(new DoubleSetting.Builder().name("break-wall-range").defaultValue(4.5).min(0.1).max(6.0).visible(doBreak::get).build());
    private final Setting<Double> maxYOffset         = sgBreak.add(new DoubleSetting.Builder().name("max-y-offset").defaultValue(5.0).min(1.0).sliderMax(10.0).visible(doBreak::get).build());
    private final Setting<Boolean> antiWeakness      = sgBreak.add(new BoolSetting.Builder().name("anti-weakness").defaultValue(true).visible(doBreak::get).build());
    private final Setting<SwapMode> antiWeaknessSwap = sgBreak.add(new EnumSetting.Builder<SwapMode>().name("anti-weakness-swap").defaultValue(SwapMode.SILENT).visible(() -> doBreak.get() && antiWeakness.get()).build());

    private final Setting<Double> minDamage          = sgDamage.add(new DoubleSetting.Builder().name("min-damage").defaultValue(4.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Double> maxLocalDamage     = sgDamage.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(12.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Boolean> safetyOverride    = sgDamage.add(new BoolSetting.Builder().name("safety-override").defaultValue(false).build());
    private final Setting<Boolean> blockDestruction  = sgDamage.add(new BoolSetting.Builder().name("block-destruction").defaultValue(true).build());
    private final Setting<Boolean> selfExtrapolate   = sgDamage.add(new BoolSetting.Builder().name("self-extrapolate").defaultValue(false).build());
    private final Setting<Integer> extrapolateTicks  = sgDamage.add(new IntSetting.Builder().name("extrapolate-ticks").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Boolean> assumeBestArmor   = sgDamage.add(new BoolSetting.Builder().name("assume-best-armor").defaultValue(false).build());
    private final Setting<Boolean> armorBreaker      = sgDamage.add(new BoolSetting.Builder().name("armor-breaker").defaultValue(true).build());
    private final Setting<Double> armorScale         = sgDamage.add(new DoubleSetting.Builder().name("armor-scale").defaultValue(5.0).min(1.0).sliderMax(20.0).visible(armorBreaker::get).build());
    private final Setting<Double> lethalMultiplier   = sgDamage.add(new DoubleSetting.Builder().name("lethal-multiplier").defaultValue(1.5).min(0.0).sliderMax(4.0).build());
    private final Setting<Boolean> antiTotem         = sgDamage.add(new BoolSetting.Builder().name("lethal-totem").defaultValue(false).build());

    private final Setting<Boolean> players           = sgTargets.add(new BoolSetting.Builder().name("players").defaultValue(true).build());
    private final Setting<Boolean> monsters          = sgTargets.add(new BoolSetting.Builder().name("monsters").defaultValue(false).build());
    private final Setting<Boolean> neutrals          = sgTargets.add(new BoolSetting.Builder().name("neutrals").defaultValue(false).build());
    private final Setting<Boolean> animals           = sgTargets.add(new BoolSetting.Builder().name("animals").defaultValue(false).build());
    private final Setting<Boolean> shulkers          = sgTargets.add(new BoolSetting.Builder().name("shulkers").description("Target shulker boxes").defaultValue(false).build());

    private final Setting<Boolean> pauseEat          = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> pauseMine         = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").defaultValue(true).build());

    private final Setting<Boolean> renderPlace       = sgRender.add(new BoolSetting.Builder().name("render-place").defaultValue(true).build());
    private final Setting<SettingColor> placeSideColor = sgRender.add(new ColorSetting.Builder().name("place-side-color").defaultValue(new SettingColor(255, 0, 0, 25)).visible(renderPlace::get).build());
    private final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder().name("place-line-color").defaultValue(new SettingColor(255, 0, 0)).visible(renderPlace::get).build());
    private final Setting<Boolean> damageESP         = sgRender.add(new BoolSetting.Builder().name("damage-esp").defaultValue(true).build());
    private final Setting<SettingColor> damageColor  = sgRender.add(new ColorSetting.Builder().name("damage-color").defaultValue(new SettingColor(255, 255, 255)).visible(damageESP::get).build());
    private final Setting<Integer> fadeTime          = sgRender.add(new IntSetting.Builder().name("fade-time").defaultValue(250).min(0).sliderMax(1000).build());

    private final CacheTimer lastBreakTimer      = new CacheTimer();
    private final CacheTimer lastPlaceTimer      = new CacheTimer();
    private final CacheTimer autoSwapTimer       = new CacheTimer();
    private final Deque<Long> breakLatencies     = new EvictingQueue<>(20);
    private final Map<Integer, Long> attackPackets = Collections.synchronizedMap(new HashMap<>());
    private final Map<BlockPos, Long> placePackets = Collections.synchronizedMap(new HashMap<>());
    private final PerSecondCounter crystalCounter = new PerSecondCounter();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private final Map<Integer, Integer> antiStuckCount = new HashMap<>();
    private final List<AntiStuckData> stuckCrystals = new CopyOnWriteArrayList<>();

    private DamageData<EndCrystalEntity> bestAttackCrystal;
    private DamageData<BlockPos> bestPlaceCrystal;
    private BlockPos renderPos;
    private double renderDamage;
    private Vec3d targetRotationVec;
    private boolean rotatedThisTick;

    public HeliumCrystalAura() {
        super(HeliumAddon.CATEGORY, "helium-crystal-aura", "Advanced crystal aura with all bypasses.");
    }

    @Override
    public String getInfoString() {
        return debugMode.get() ? String.format("%dms %d", getBreakMs(), crystalCounter.getPerSecond())
                : String.format("%d %d", getBreakMs(), crystalCounter.getPerSecond());
    }

    @Override
    public void onActivate() {
        clearState();
    }

    @Override
    public void onDeactivate() {
        clearState();
    }

    private void clearState() {
        bestAttackCrystal = null;
        bestPlaceCrystal = null;
        renderPos = null;
        targetRotationVec = null;
        rotatedThisTick = false;
        stuckCrystals.clear();
        attackPackets.clear();
        placePackets.clear();
        breakLatencies.clear();
        fadeList.clear();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (pauseEat.get() && mc.player.isUsingItem()) return;
        if (pauseMine.get() && mc.interactionManager.isBreakingBlock()) return;

        stuckCrystals.removeIf(data -> mc.player.squaredDistanceTo(data.pos()) < data.stuckDist() - 0.5);

        List<Entity> entities = java.util.stream.StreamSupport
                .stream(mc.world.getEntities().spliterator(), false)
                .toList();

        bestAttackCrystal = calculateAttackCrystal(entities);
        bestPlaceCrystal = doPlace.get() ? calculatePlaceCrystal(entities) : null;

        if (bestAttackCrystal == null && bestPlaceCrystal != null) {
            EndCrystalEntity intersecting = intersectingCrystalCheck(bestPlaceCrystal.getDamageData());
            if (intersecting != null) {
                Vec3d crystalPos = new Vec3d(intersecting.getX(), intersecting.getY(), intersecting.getZ());
                double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystalPos,
                        blockDestruction.get(),
                        selfExtrapolate.get() ? extrapolateTicks.get() : 0,
                        assumeBestArmor.get());
                if (!playerDamageCheck(selfDamage)) {
                    bestAttackCrystal = new DamageData<>(intersecting, bestPlaceCrystal.getAttackTarget(),
                            bestPlaceCrystal.getDamage(), selfDamage, bestPlaceCrystal.getBlockPos(), false);
                }
            }
        }

        rotatedThisTick = false;
        targetRotationVec = null;
        if (rotate.get()) {
            if (bestAttackCrystal != null) {
                EndCrystalEntity crystal = bestAttackCrystal.getDamageData();
                targetRotationVec = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
            } else if (bestPlaceCrystal != null && canHoldCrystal()) {
                targetRotationVec = bestPlaceCrystal.getDamageData().toCenterPos().add(0, 0.5, 0);
            }
            if (targetRotationVec != null) {
                float[] desired = RotationUtils.getRotationsTo(mc.player.getEyePos(), targetRotationVec);
                if (yawStepMode.get() == YawStepMode.FULL || (yawStepMode.get() == YawStepMode.SEMI && bestAttackCrystal != null)) {
                    float serverYaw = RotationUtils.getInstance().getWrappedYaw();
                    float diff = MathHelper.wrapDegrees(desired[0] - serverYaw);
                    int dir = diff > 0 ? 1 : -1;
                    if (Math.abs(diff) > yawStepLimit.get()) {
                        desired[0] = serverYaw + dir * yawStepLimit.get();
                    } else {
                        rotatedThisTick = true;
                    }
                } else {
                    rotatedThisTick = true;
                }
                RotationUtils.getInstance().setRotationSilent(desired[0], desired[1], Integer.MAX_VALUE);
            }
        }

        int actions = 0;
        if (bestAttackCrystal != null && (!rotate.get() || rotatedThisTick)) {
            float breakDelay = getBreakDelay();
            if (attackDelay.get() > 0.0) {
                float factor = 50.0f / Math.max(1, attackFactor.get());
                breakDelay = (float) (attackDelay.get() * factor);
            }
            if (breakDelayAdapt.get()) {
                breakDelay = (float) Math.max(minBreakTimeout.get() * 50.0, getBreakMs() + breakTimeout.get() * 50.0);
            }
            if (lastBreakTimer.passed((long) breakDelay) && actions < maxPackets.get()) {
                if (!inhibit.get() || !attackPackets.containsKey(bestAttackCrystal.getDamageData().getId())) {
                    Hand hand = getCrystalHand();
                    attackCrystal(bestAttackCrystal.getDamageData(), hand);
                    lastBreakTimer.reset();
                    actions++;
                    crystalCounter.updateCounter();
                    if (sequential.get() && bestPlaceCrystal != null && actions < maxPackets.get()) {
                        placeCrystal(bestPlaceCrystal.getDamageData(), hand);
                        lastPlaceTimer.reset();
                        actions++;
                    }
                }
            }
        }

        if (bestPlaceCrystal != null && doPlace.get() && (!rotate.get() || rotatedThisTick)
                && lastPlaceTimer.passed((long) (1000.0 - placeDelay.get() * 50.0)) && actions < maxPackets.get()) {
            Hand hand = getCrystalHand();
            for (int i = 0; i < Math.min(multiPlace.get(), getCrystalCount()); i++) {
                if (actions >= maxPackets.get()) break;
                placeCrystal(bestPlaceCrystal.getDamageData(), hand);
                lastPlaceTimer.reset();
                actions++;
            }
        }

        if (bestPlaceCrystal != null && isHoldingCrystal()) {
            renderPos = bestPlaceCrystal.getDamageData();
            renderDamage = bestPlaceCrystal.getDamage();
        } else {
            renderPos = null;
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (renderPlace.get() && renderPos != null) {
            event.renderer.box(renderPos, toMeteorColor(placeSideColor.get()), toMeteorColor(placeLineColor.get()), ShapeMode.Both, 0);
        }
        for (Map.Entry<BlockPos, Animation> entry : fadeList.entrySet()) {
            if (entry.getKey().equals(renderPos)) continue;
            float factor = entry.getValue().getFactor();
            if (factor <= 0) continue;
            int alpha = (int) (100 * factor);
            SettingColor lineColor = placeLineColor.get();
            SettingColor sideColor = placeSideColor.get();
            event.renderer.box(entry.getKey(),
                    new Color(sideColor.r, sideColor.g, sideColor.b, (int)(40 * factor)),
                    new Color(lineColor.r, lineColor.g, lineColor.b, alpha),
                    ShapeMode.Both, 0);
        }
        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() <= 0);
        if (renderPos != null && isHoldingCrystal() && !fadeList.containsKey(renderPos)) {
            fadeList.put(renderPos, new Animation(true, fadeTime.get()));
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (damageESP.get() && renderPos != null) {
            Vec3d vec = renderPos.toCenterPos().add(0, 0.8, 0);
            if (NametagUtils.to2D(new Vector3d(vec.x, vec.y, vec.z), 1.0)) {
                NametagUtils.begin(new Vector3d(vec.x, vec.y, vec.z));
                String text = String.format("%.1f", renderDamage);
                TextRenderer.get().render(text, -TextRenderer.get().getWidth(text) / 2.0, 0.0, damageColor.get(), true);
                NametagUtils.end();
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (event.packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> p : bundle.getPackets()) handleServerPacket(p);
        } else {
            handleServerPacket(event.packet);
        }
    }

    private void handleServerPacket(Packet<?> packet) {
        if (packet instanceof ExplosionS2CPacket) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof EndCrystalEntity) recordBreak(e.getId());
            }
        } else if (packet instanceof PlaySoundS2CPacket sound &&
                sound.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value() &&
                sound.getCategory() == SoundCategory.BLOCKS) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof EndCrystalEntity) recordBreak(e.getId());
            }
        } else if (packet instanceof EntitiesDestroyS2CPacket destroy) {
            for (int id : destroy.getEntityIds()) {
                recordBreak(id);
            }
        }
    }

    private void recordBreak(int entityId) {
        Long time = attackPackets.remove(entityId);
        if (time != null) {
            breakLatencies.add(System.currentTimeMillis() - time);
        }
        antiStuckCount.remove(entityId);
    }

    private void attackCrystal(EndCrystalEntity crystal, Hand hand) {
        if (hand == null) return;
        if (antiWeakness.get() && mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            if (strength == null || strength.getAmplifier() <= mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier()) {
                int toolSlot = getBestWeaponSlot();
                if (toolSlot != -1) {
                    if (antiWeaknessSwap.get() == SwapMode.SILENT) {
                        InventoryManager.getInstance().setSlot(toolSlot, InventoryManager.Priority.NORMAL);
                    } else {
                        InventoryManager.getInstance().setClientSlot(toolSlot, InventoryManager.Priority.NORMAL);
                    }
                    attackInternal(crystal, Hand.MAIN_HAND);
                    if (antiWeaknessSwap.get() == SwapMode.SILENT) {
                        InventoryManager.getInstance().syncToClient();
                    }
                    return;
                }
            }
        }
        attackInternal(crystal, hand);
    }

    private void attackInternal(EndCrystalEntity crystal, Hand hand) {
        attackPackets.put(crystal.getId(), System.currentTimeMillis());
        Integer count = antiStuckCount.get(crystal.getId());
        if (count != null) {
            antiStuckCount.put(crystal.getId(), count + 1);
            if (count + 1 > attackLimit.get() * 10) {
                stuckCrystals.add(new AntiStuckData(crystal.getId(), crystal.getBlockPos(),
                        new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()), mc.player.squaredDistanceTo(crystal)));
                return;
            }
        } else {
            antiStuckCount.put(crystal.getId(), 1);
        }
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    private void placeCrystal(BlockPos pos, Hand hand) {
        if (pos == null || hand == null) return;
        if (!canUseCrystalOnBlock(pos) || placeRangeCheck(pos)) return;

        Direction side = getPlaceDirection(pos);
        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), side, pos, false);

        if (autoSwapNeeded() && getCrystalHand() == null) {
            int crystalSlot = findCrystalSlot();
            if (crystalSlot != -1 && autoSwapTimer.passed(500)) {
                InventoryManager.getInstance().setSlot(crystalSlot);
                autoSwapTimer.reset();
            }
        }
        if (!isHoldingCrystal()) return;

        placePackets.put(pos, System.currentTimeMillis());
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        if (predictId.get() && fastCrystal.get() && lastEntityId != -1) {
            int predictedId = lastEntityId + 1;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(mc.world.getEntityById(predictedId) != null ? mc.world.getEntityById(predictedId) : new EndCrystalEntity(mc.world, 0, 0, 0), false));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            attackPackets.put(predictedId, System.currentTimeMillis());
        }
    }

    private DamageData<EndCrystalEntity> calculateAttackCrystal(List<Entity> entities) {
        DamageData<EndCrystalEntity> best = null;
        for (Entity e : entities) {
            if (!(e instanceof EndCrystalEntity crystal) || !e.isAlive()) continue;
            if (stuckCrystals.stream().anyMatch(d -> d.id() == e.getId())) continue;
            if (crystal.age < ticksExisted.get() || (inhibit.get() && attackPackets.containsKey(e.getId()))) continue;
            if (attackRangeCheck(crystal)) continue;

            Vec3d crystalPos = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
            double self = ExplosionUtil.getDamageTo(mc.player, crystalPos,
                    blockDestruction.get(),
                    selfExtrapolate.get() ? extrapolateTicks.get() : 0,
                    assumeBestArmor.get());
            if (playerDamageCheck(self)) continue;

            for (Entity target : entities) {
                if (!isValidTarget(target) || target == mc.player) continue;
                double dmg = ExplosionUtil.getDamageTo(target, crystalPos,
                        blockDestruction.get(),
                        extrapolateTicks.get(),
                        assumeBestArmor.get());
                if (dmg < minDamage.get() && !(target instanceof LivingEntity le && isCrystalLethalTo(dmg, le))) continue;
                DamageData<EndCrystalEntity> data = new DamageData<>(crystal, target, dmg, self, crystal.getBlockPos().down(), false);
                if (best == null || dmg > best.getDamage()) best = data;
            }
        }
        return best;
    }

    private DamageData<BlockPos> calculatePlaceCrystal(List<Entity> entities) {
        Vec3d origin = placeRangeEye.get() ? mc.player.getEyePos()
                : new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        List<BlockPos> possible = getSphere(origin);
        DamageData<BlockPos> best = null;
        for (BlockPos pos : possible) {
            if (!canUseCrystalOnBlock(pos) || placeRangeCheck(pos)) continue;
            if (stuckCrystals.stream().anyMatch(d -> d.blockPos().equals(pos.up()))) continue;

            Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            double self = ExplosionUtil.getDamageTo(mc.player, crystalPos,
                    blockDestruction.get(),
                    selfExtrapolate.get() ? extrapolateTicks.get() : 0,
                    assumeBestArmor.get());
            if (playerDamageCheck(self)) continue;

            for (Entity target : entities) {
                if (!isValidTarget(target) || target == mc.player) continue;
                double dmg = ExplosionUtil.getDamageTo(target, crystalPos,
                        blockDestruction.get(),
                        extrapolateTicks.get(),
                        assumeBestArmor.get());
                if (dmg < minDamage.get() && !(target instanceof LivingEntity le && isCrystalLethalTo(dmg, le))) continue;
                DamageData<BlockPos> data = new DamageData<>(pos, target, dmg, self, pos, false);
                if (best == null || dmg > best.getDamage()) best = data;
            }
        }
        return best;
    }

    private boolean placeRangeCheck(BlockPos pos) {
        double range = placeRange.get();
        double wallRange = placeWallRange.get();
        Vec3d player = placeRangeEye.get() ? mc.player.getEyePos()
                : new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double dist = placeRangeCenter.get() ? player.squaredDistanceTo(pos.toCenterPos())
                : pos.getSquaredDistance(player.x, player.y, player.z);

        if (dist > range * range) return true;

        Vec3d raytrace = new Vec3d(pos.getX() + 0.5, pos.getY() + 2.7, pos.getZ() + 0.5);
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), raytrace, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        boolean isWall = result != null && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos);
        return isWall && dist > wallRange * wallRange;
    }

    private boolean attackRangeCheck(EndCrystalEntity crystal) {
        double range = breakRange.get();
        double wallRange = breakWallRange.get();
        Vec3d player = mc.player.getEyePos();
        double dist = player.squaredDistanceTo(crystal.getX(), crystal.getY(), crystal.getZ());
        if (dist > range * range) return true;
        if (Math.abs(crystal.getY() - mc.player.getY()) > maxYOffset.get()) return true;

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                player, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        return result.getType() != HitResult.Type.MISS && dist > wallRange * wallRange;
    }

    private boolean playerDamageCheck(double selfDamage) {
        if (mc.player.isCreative()) return false;
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return selfDamage > maxLocalDamage.get() || (selfDamage >= health + 0.5f && !safetyOverride.get());
    }

    private boolean isCrystalLethalTo(double damage, LivingEntity entity) {
        float health = entity.getHealth() + entity.getAbsorptionAmount();
        if (damage >= health * lethalMultiplier.get()) return true;

        if (armorBreaker.get()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = entity.getEquippedStack(slot);
                if (!armor.isEmpty()) {
                    float durability = ((armor.getMaxDamage() - armor.getDamage()) / (float) armor.getMaxDamage()) * 100.0f;
                    if (durability < armorScale.get()) return true;
                }
            }
        }
        if (shulkers.get() && entity instanceof PlayerEntity) {
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            for (BlockPos pos : getSphere(3.0f, entityPos)) {
                if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) return true;
            }
        }
        return false;
    }

    private Vec3d crystalDamageVec(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    private boolean canUseCrystalOnBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) && isCrystalHitboxClear(pos);
    }

    private boolean isCrystalHitboxClear(BlockPos pos) {
        BlockPos above = pos.up();
        if (!mc.world.isAir(above) && !mc.world.getBlockState(above).isOf(Blocks.FIRE)) return false;
        List<Entity> entities = mc.world.getOtherEntities(null, new Box(above));
        for (Entity e : entities) {
            if (e instanceof EndCrystalEntity) {
                if (stuckCrystals.stream().anyMatch(d -> d.id() == e.getId())) continue;
                if (forcePlace.get() != ForcePlace.PRE) return false;
            } else if (e instanceof ItemEntity && forcePlace.get() == ForcePlace.POST && e.age < 10) {
                continue;
            } else if (e.isAlive() && !(e instanceof ExperienceOrbEntity)) {
                return false;
            }
        }
        return true;
    }

    private EndCrystalEntity intersectingCrystalCheck(BlockPos pos) {
        return mc.world.getOtherEntities(null, new Box(pos.up())).stream()
                .filter(e -> e instanceof EndCrystalEntity)
                .map(e -> (EndCrystalEntity) e)
                .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e.getX(), e.getY(), e.getZ())))
                .orElse(null);
    }

    private List<BlockPos> getSphere(Vec3d origin) {
        return getSphere(placeRange.get(), origin);
    }

    private List<BlockPos> getSphere(double rad, Vec3d origin) {
        List<BlockPos> blocks = new ArrayList<>();
        int ceilRad = (int) Math.ceil(rad);
        double radSq = rad * rad;

        for (int x = -ceilRad; x <= ceilRad; x++) {
            for (int y = -ceilRad; y <= ceilRad; y++) {
                for (int z = -ceilRad; z <= ceilRad; z++) {
                    if ((x * x + y * y + z * z) <= radSq) {
                        blocks.add(new BlockPos((int) (origin.x + x), (int) (origin.y + y), (int) (origin.z + z)));
                    }
                }
            }
        }
        return blocks;
    }

    private boolean canHoldCrystal() {
        return isHoldingCrystal() || findCrystalSlot() != -1;
    }

    private boolean isHoldingCrystal() {
        return mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL
                || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    private Hand getCrystalHand() {
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) return Hand.OFF_HAND;
        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) return Hand.MAIN_HAND;
        return null;
    }

    private boolean autoSwapNeeded() {
        return getCrystalHand() == null && findCrystalSlot() != -1;
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL || mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
            }
        }
        return -1;
    }

    private int getCrystalCount() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL || mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                    count += mc.player.getInventory().getStack(i).getCount();
                }
            }
        }
        return count;
    }

    private int getBestWeaponSlot() {
        int slot = -1;
        float best = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            Item item = stack.getItem();
            if (item.toString().toLowerCase().contains("sword") || item instanceof AxeItem) {
                float dmg = InventoryManager.getWeaponDamage(stack);
                if (dmg > best) { best = dmg; slot = i; }
            }
        }
        return slot;
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.world.isInBuildLimit(pos)) return Direction.DOWN;
        if (strictDirection.get()) {
            BlockHitResult res = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(), pos.toCenterPos(), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (res != null && res.getType() == HitResult.Type.BLOCK) return res.getSide();
        }
        return Direction.UP;
    }

    private float getBreakDelay() {
        return (float) (1000.0 - breakSpeed.get() * 50.0);
    }

    private int getBreakMs() {
        synchronized (breakLatencies) {
            if (breakLatencies.isEmpty()) return 0;
            long sum = 0;
            int size = 0;
            for (Long t : breakLatencies) {
                if (t != null) { sum += t; size++; }
            }
            return size == 0 ? 0 : (int) (sum / size);
        }
    }

    private boolean isValidTarget(Entity e) {
        if (e instanceof PlayerEntity) {
            if (!players.get()) return false;
            if (Friends.get().isFriend((PlayerEntity) e)) return false;
            return true;
        }
        if (EntityUtil.isMonster(e) && monsters.get()) return true;
        if (EntityUtil.isNeutral(e) && neutrals.get()) return true;
        if (EntityUtil.isPassive(e) && animals.get()) return true;
        return false;
    }

    private Color toMeteorColor(SettingColor sc) {
        return new Color(sc.r, sc.g, sc.b, sc.a);
    }

    public enum YawStepMode { FULL, SEMI, OFF }
    public enum SwapMode { NORMAL, SILENT }
    public enum ForcePlace { PRE, POST, NONE }

    private record AntiStuckData(int id, BlockPos blockPos, Vec3d pos, double stuckDist) {}

    private static class DamageData<T> {
        private T damageData;
        private Entity attackTarget;
        private BlockPos blockPos;
        private double damage, selfDamage;
        private boolean antiSurround;

        public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos, boolean antiSurround) {
            this.damageData = damageData;
            this.attackTarget = attackTarget;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.blockPos = blockPos;
            this.antiSurround = antiSurround;
        }

        public T getDamageData() { return damageData; }
        public Entity getAttackTarget() { return attackTarget; }
        public double getDamage() { return damage; }
        public BlockPos getBlockPos() { return blockPos; }
        public boolean isAntiSurround() { return antiSurround; }
    }

    private static class Animation {
        private boolean active;
        private long start;
        private int duration;
        public Animation(boolean active, int duration) {
            this.active = active; this.start = System.currentTimeMillis(); this.duration = duration;
        }
        public float getFactor() {
            if (!active) return 0;
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= duration) { active = false; return 0; }
            return 1.0f - (float) elapsed / duration;
        }
        public void setState(boolean active) { this.active = active; if (active) start = System.currentTimeMillis(); }
    }

    private static class PerSecondCounter {
        private long lastUpdate = System.currentTimeMillis();
        private int count = 0;
        public void updateCounter() { count++; }
        public int getPerSecond() {
            long now = System.currentTimeMillis();
            if (now - lastUpdate > 1000) { lastUpdate = now; count = 0; }
            return count;
        }
    }
}