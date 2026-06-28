package com.helium.modules.combat;

import com.helium.HeliumAddon;
import com.helium.util.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HeliumCrystalAura extends Module {
    public static int lastEntityId = -1;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");
    private final SettingGroup sgRotations = settings.createGroup("Rotations");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgTargets = settings.createGroup("Targets");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range").defaultValue(10.0).min(1.0).sliderMax(15.0).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());

    private final Setting<Boolean> instant = sgAdvanced.add(new BoolSetting.Builder()
            .name("instant").defaultValue(true).build());
    private final Setting<Boolean> instantCalc = sgAdvanced.add(new BoolSetting.Builder()
            .name("instant-calc").defaultValue(false).visible(() -> instant.get()).build());
    private final Setting<Double> instantDamage = sgAdvanced.add(new DoubleSetting.Builder()
            .name("instant-damage").defaultValue(6.0).min(1.0).sliderMax(10.0).visible(() -> instantCalc.get()).build());
    private final Setting<Boolean> instantMax = sgAdvanced.add(new BoolSetting.Builder()
            .name("instant-max").defaultValue(true).visible(() -> instantCalc.get()).build());
    private final Setting<Boolean> predictIdSetting = sgAdvanced.add(new BoolSetting.Builder()
            .name("predict-id").defaultValue(true).build());
    private final Setting<Boolean> sequential = sgAdvanced.add(new BoolSetting.Builder()
            .name("sequential").defaultValue(true).build());
    private final Setting<Integer> maxPackets = sgAdvanced.add(new IntSetting.Builder()
            .name("max-packets").defaultValue(8).min(1).max(20).build());
    private final Setting<Integer> multiPlace = sgAdvanced.add(new IntSetting.Builder()
            .name("multi-place").defaultValue(2).min(1).max(5).build());
    private final Setting<Boolean> inhibit = sgAdvanced.add(new BoolSetting.Builder()
            .name("inhibit").defaultValue(true).build());
    private final Setting<Boolean> antiSurround = sgAdvanced.add(new BoolSetting.Builder()
            .name("anti-surround").defaultValue(false).build());

    private final Setting<Boolean> rotate = sgRotations.add(new BoolSetting.Builder()
            .name("rotate").defaultValue(true).build());
    private final Setting<YawStep> yawStep = sgRotations.add(new EnumSetting.Builder<YawStep>()
            .name("yaw-step").defaultValue(YawStep.SEMI).visible(rotate::get).build());
    private final Setting<Integer> yawStepLimit = sgRotations.add(new IntSetting.Builder()
            .name("yaw-step-limit").defaultValue(180).min(1).sliderMax(180).visible(() -> rotate.get() && yawStep.get() != YawStep.OFF).build());

    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
            .name("place").defaultValue(true).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range").defaultValue(4.5).min(0.1).max(6.0).visible(doPlace::get).build());
    private final Setting<Double> placeWallRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-wall-range").defaultValue(4.5).min(0.1).max(6.0).visible(doPlace::get).build());
    private final Setting<Boolean> placeRangeEye = sgPlace.add(new BoolSetting.Builder()
            .name("place-range-eye").defaultValue(false).visible(doPlace::get).build());
    private final Setting<Boolean> placeRangeCenter = sgPlace.add(new BoolSetting.Builder()
            .name("place-range-center").defaultValue(true).visible(doPlace::get).build());
    private final Setting<Boolean> breakValid = sgPlace.add(new BoolSetting.Builder()
            .name("break-valid").defaultValue(false).visible(doPlace::get).build());
    private final Setting<Boolean> strictDirection = sgPlace.add(new BoolSetting.Builder()
            .name("strict-direction").defaultValue(false).visible(doPlace::get).build());
    private final Setting<Swap> autoSwap = sgPlace.add(new EnumSetting.Builder<Swap>()
            .name("auto-swap").defaultValue(Swap.SILENT).visible(doPlace::get).build());

    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
            .name("break").defaultValue(true).build());
    private final Setting<Double> breakSpeed = sgBreak.add(new DoubleSetting.Builder()
            .name("break-speed").defaultValue(19.0).min(0.1).sliderMax(20.0).visible(doBreak::get).build());
    private final Setting<Double> attackDelay = sgBreak.add(new DoubleSetting.Builder()
            .name("attack-delay").defaultValue(0.0).min(0.0).sliderMax(5.0).visible(doBreak::get).build());
    private final Setting<Integer> attackFactor = sgBreak.add(new IntSetting.Builder()
            .name("attack-factor").defaultValue(0).min(0).sliderMax(3).visible(() -> doBreak.get() && attackDelay.get() > 0.0).build());
    private final Setting<Double> attackLimit = sgBreak.add(new DoubleSetting.Builder()
            .name("attack-limit").defaultValue(1.5).min(0.5).sliderMax(20.0).visible(doBreak::get).build());
    private final Setting<Boolean> breakDelayAdapt = sgBreak.add(new BoolSetting.Builder()
            .name("adaptive-break-delay").defaultValue(false).visible(doBreak::get).build());
    private final Setting<Double> breakTimeout = sgBreak.add(new DoubleSetting.Builder()
            .name("break-timeout").defaultValue(3.0).min(0.0).sliderMax(10.0).visible(() -> doBreak.get() && breakDelayAdapt.get()).build());
    private final Setting<Double> minBreakTimeout = sgBreak.add(new DoubleSetting.Builder()
            .name("min-break-timeout").defaultValue(5.0).min(0.0).sliderMax(20.0).visible(() -> doBreak.get() && breakDelayAdapt.get()).build());
    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder()
            .name("ticks-existed").defaultValue(0).min(0).sliderMax(10).visible(doBreak::get).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range").defaultValue(4.5).min(0.1).max(6.0).visible(doBreak::get).build());
    private final Setting<Double> breakWallRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-wall-range").defaultValue(4.5).min(0.1).max(6.0).visible(doBreak::get).build());
    private final Setting<Double> maxYOffset = sgBreak.add(new DoubleSetting.Builder()
            .name("max-y-offset").defaultValue(5.0).min(1.0).sliderMax(10.0).visible(doBreak::get).build());

    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min-damage").defaultValue(4.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Double> maxLocalDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max-self-damage").defaultValue(12.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Boolean> safetyOverride = sgDamage.add(new BoolSetting.Builder()
            .name("safety-override").defaultValue(false).build());
    private final Setting<Boolean> blockDestruction = sgDamage.add(new BoolSetting.Builder()
            .name("block-destruction").defaultValue(true).build());
    private final Setting<Boolean> selfExtrapolate = sgDamage.add(new BoolSetting.Builder()
            .name("self-extrapolate").defaultValue(false).build());
    private final Setting<Integer> extrapolateTicks = sgDamage.add(new IntSetting.Builder()
            .name("extrapolate-ticks").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Boolean> assumeBestArmor = sgDamage.add(new BoolSetting.Builder()
            .name("assume-best-armor").defaultValue(false).build());
    private final Setting<Boolean> armorBreaker = sgDamage.add(new BoolSetting.Builder()
            .name("armor-breaker").defaultValue(true).build());
    private final Setting<Double> armorScale = sgDamage.add(new DoubleSetting.Builder()
            .name("armor-scale").defaultValue(5.0).min(1.0).sliderMax(20.0).visible(armorBreaker::get).build());
    private final Setting<Double> lethalMultiplier = sgDamage.add(new DoubleSetting.Builder()
            .name("lethal-multiplier").defaultValue(1.5).min(0.0).sliderMax(4.0).build());
    private final Setting<Boolean> antiTotem = sgDamage.add(new BoolSetting.Builder()
            .name("lethal-totem").defaultValue(false).build());

    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder()
            .name("players").defaultValue(true).build());
    private final Setting<Boolean> monsters = sgTargets.add(new BoolSetting.Builder()
            .name("monsters").defaultValue(false).build());
    private final Setting<Boolean> neutrals = sgTargets.add(new BoolSetting.Builder()
            .name("neutrals").defaultValue(false).build());
    private final Setting<Boolean> animals = sgTargets.add(new BoolSetting.Builder()
            .name("animals").defaultValue(false).build());
    private final Setting<Boolean> shulkers = sgTargets.add(new BoolSetting.Builder()
            .name("shulkers").defaultValue(false).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render").defaultValue(true).build());
    private final Setting<SettingColor> placeColor = sgRender.add(new ColorSetting.Builder()
            .name("place-color").defaultValue(new SettingColor(255, 0, 0, 40)).visible(render::get).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color").defaultValue(new SettingColor(255, 0, 0)).visible(render::get).build());
    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
            .name("fade-time").defaultValue(250).min(0).sliderMax(1000).visible(render::get).build());

    private DamageData<EndCrystalEntity> attackCrystal;
    private DamageData<BlockPos> placeCrystal;
    private BlockPos renderPos;
    private double renderDamage;
    private long predictedEntityId = -1;

    private final CacheTimer lastBreakTimer = new CacheTimer();
    private final CacheTimer lastSwapTimer = new CacheTimer();
    private final Deque<Long> breakLatencies = new ArrayDeque<>(20);
    private final Map<Integer, Long> attackPackets = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> placePackets = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> antiStuck = new ConcurrentHashMap<>();
    private final List<StuckData> stuckCrystals = new CopyOnWriteArrayList<>();
    private final Map<BlockPos, FadeAnimation> fades = new HashMap<>();
    private final Map<PlayerEntity, Long> totemPopTimes = new HashMap<>();

    private Vec3d targetVec;
    private boolean rotated;
    private int actionsThisTick;

    public HeliumCrystalAura() {
        super(HeliumAddon.CATEGORY, "helium-crystal-aura", "Advanced crystal aura");
    }

    @Override
    public String getInfoString() {
        return debug.get() ? String.format("%dms %d", getBreakMs(), 0) : String.format("%d %d", getBreakMs(), 0);
    }

    @Override
    public void onActivate() { clear(); }
    @Override
    public void onDeactivate() { clear(); }

    private void clear() {
        attackCrystal = null; placeCrystal = null; renderPos = null;
        targetVec = null; rotated = false;
        attackPackets.clear(); placePackets.clear(); breakLatencies.clear();
        antiStuck.clear(); stuckCrystals.clear(); fades.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem() || mc.interactionManager.isBreakingBlock()) return;

        actionsThisTick = 0;
        List<Entity> entities = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .collect(Collectors.toList());

        if (doBreak.get()) attackCrystal = calculateAttackCrystal(entities);
        else attackCrystal = null;

        if (doPlace.get()) placeCrystal = calculatePlaceCrystal(entities);
        else placeCrystal = null;

        if (attackCrystal == null && placeCrystal != null) {
            EndCrystalEntity intersecting = getIntersectingCrystal(placeCrystal.getDamageData());
            if (intersecting != null) {
                double self = ExplosionUtil.getDamageTo(mc.player, new Vec3d(intersecting.getX(), intersecting.getY(), intersecting.getZ()),
                        blockDestruction.get(), selfExtrapolate.get() ? extrapolateTicks.get() : 0, assumeBestArmor.get());
                if (!playerDamageCheck(self)) {
                    attackCrystal = new DamageData<>(intersecting, placeCrystal.getAttackTarget(),
                            placeCrystal.getDamage(), self, placeCrystal.getBlockPos());
                }
            }
        }

        targetVec = null;
        rotated = false;
        if (rotate.get()) {
            if (attackCrystal != null) {
                EndCrystalEntity crystal = attackCrystal.getDamageData();
                targetVec = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
            } else if (placeCrystal != null && canHoldCrystal()) {
                targetVec = placeCrystal.getDamageData().toCenterPos().add(0, 0.5, 0);
            }

            if (targetVec != null) {
                float[] desired = RotationUtils.getRotationsTo(mc.player.getEyePos(), targetVec);
                float serverYaw = RotationUtils.getInstance().getWrappedYaw();

                if (yawStep.get() == YawStep.FULL || (yawStep.get() == YawStep.SEMI && attackCrystal != null)) {
                    float diff = MathHelper.wrapDegrees(desired[0] - serverYaw);
                    if (Math.abs(diff) > yawStepLimit.get()) {
                        desired[0] = serverYaw + (diff > 0 ? 1 : -1) * yawStepLimit.get();
                    } else {
                        rotated = true;
                    }
                } else {
                    rotated = true;
                }

                if (rotated || yawStep.get() == YawStep.OFF) {
                    RotationUtils.getInstance().setRotationSilent(desired[0], desired[1], Integer.MAX_VALUE);
                }
            }
        }

        float breakDelay = getBreakDelay();
        if (attackDelay.get() > 0) {
            breakDelay = (float) (attackDelay.get() * (50.0f / Math.max(1, attackFactor.get())));
        }
        if (breakDelayAdapt.get()) {
            breakDelay = (float) Math.max(minBreakTimeout.get() * 50.0, getBreakMs() + breakTimeout.get() * 50.0);
        }

        if (attackCrystal != null && lastBreakTimer.passed((long) breakDelay) && actionsThisTick < maxPackets.get()) {
            if (!inhibit.get() || !attackPackets.containsKey(attackCrystal.getDamageData().getId())) {
                if (!rotate.get() || rotated) {
                    Hand hand = getCrystalHand();
                    attackCrystal(attackCrystal.getDamageData(), hand);
                    lastBreakTimer.reset();
                    actionsThisTick++;

                    if (sequential.get() && placeCrystal != null && actionsThisTick < maxPackets.get()) {
                        placeCrystal(placeCrystal.getDamageData(), getCrystalHand());
                        actionsThisTick++;
                    }
                }
            }
        }

        if (placeCrystal != null && doPlace.get() && actionsThisTick < maxPackets.get()) {
            if (!rotate.get() || rotated) {
                Hand hand = getCrystalHand();
                int places = Math.min(multiPlace.get(), maxPackets.get() - actionsThisTick);
                for (int i = 0; i < places; i++) {
                    if (!isHoldingCrystal() && autoSwap.get() != Swap.OFF) {
                        int slot = findCrystalSlot();
                        if (slot != -1 && lastSwapTimer.passed(500)) {
                            if (autoSwap.get() == Swap.SILENT) InventoryManager.getInstance().setSlot(slot);
                            else InventoryManager.getInstance().setClientSlot(slot);
                            lastSwapTimer.reset();
                        }
                    }
                    if (isHoldingCrystal()) {
                        placeCrystal(placeCrystal.getDamageData(), hand);
                        actionsThisTick++;
                    }
                }
            }
        }

        if (placeCrystal != null && isHoldingCrystal()) {
            renderPos = placeCrystal.getDamageData();
            renderDamage = placeCrystal.getDamage();
        } else {
            renderPos = null;
        }
    }

    private DamageData<EndCrystalEntity> calculateAttackCrystal(List<Entity> entities) {
        DamageData<EndCrystalEntity> best = null;
        for (Entity e : entities) {
            if (!(e instanceof EndCrystalEntity crystal) || !e.isAlive()) continue;
            if (stuckCrystals.stream().anyMatch(d -> d.id == e.getId())) continue;
            if (crystal.age < ticksExisted.get()) continue;
            if (inhibit.get() && attackPackets.containsKey(e.getId())) continue;
            if (attackRangeCheck(crystal)) continue;

            double self = ExplosionUtil.getDamageTo(mc.player, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()),
                    blockDestruction.get(), selfExtrapolate.get() ? extrapolateTicks.get() : 0, assumeBestArmor.get());
            if (playerDamageCheck(self) && !safetyOverride.get()) continue;

            for (Entity target : entities) {
                if (!isValidTarget(target) || target == mc.player) continue;
                if (Friends.get().isFriend((PlayerEntity) target)) continue;

                double dmg = ExplosionUtil.getDamageTo(target, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()),
                        blockDestruction.get(), extrapolateTicks.get(), assumeBestArmor.get());
                if (dmg < minDamage.get() && !(target instanceof LivingEntity le && isLethal(dmg, le))) continue;

                DamageData<EndCrystalEntity> data = new DamageData<>(crystal, target, dmg, self, crystal.getBlockPos().down());
                if (best == null || dmg > best.getDamage()) best = data;
            }
        }
        return best;
    }

    private DamageData<BlockPos> calculatePlaceCrystal(List<Entity> entities) {
        Vec3d origin = placeRangeEye.get() ? mc.player.getEyePos() : new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        List<BlockPos> blocks = getSphere(origin);
        DamageData<BlockPos> best = null;
        List<DamageData<BlockPos>> validData = new ArrayList<>();

        for (BlockPos pos : blocks) {
            if (!canPlaceCrystal(pos) || placeRangeCheck(pos)) continue;
            if (stuckCrystals.stream().anyMatch(d -> d.blockPos.equals(pos.up()))) continue;

            Vec3d crystalPos = pos.toCenterPos().add(0, 1, 0);
            double self = ExplosionUtil.getDamageTo(mc.player, crystalPos,
                    blockDestruction.get(), selfExtrapolate.get() ? extrapolateTicks.get() : 0, assumeBestArmor.get());
            if (playerDamageCheck(self) && !safetyOverride.get()) continue;

            for (Entity target : entities) {
                if (!isValidTarget(target) || target == mc.player) continue;
                if (Friends.get().isFriend((PlayerEntity) target)) continue;

                double dmg = ExplosionUtil.getDamageTo(target, crystalPos,
                        blockDestruction.get(), extrapolateTicks.get(), assumeBestArmor.get());
                if (dmg < minDamage.get() && !(target instanceof LivingEntity le && isLethal(dmg, le))) continue;

                boolean antiSurroundFlag = false;
                if (antiSurround.get() && target instanceof PlayerEntity player) {
                    BlockPos feet = player.getBlockPos();
                    if (mc.world.getBlockState(feet).isReplaceable()) {
                        for (Direction dir : Direction.values()) {
                            if (!dir.getAxis().isHorizontal()) continue;
                            BlockPos neighbor = feet.offset(dir);
                            if (mc.world.getBlockState(neighbor).isReplaceable()) {
                                antiSurroundFlag = true;
                                break;
                            }
                        }
                    }
                }

                DamageData<BlockPos> data = new DamageData<>(pos, target, dmg, self, pos, antiSurroundFlag);
                validData.add(data);
                if (best == null || dmg > best.getDamage()) best = data;
            }
        }

        if (best == null && antiSurround.get()) {
            return validData.stream()
                    .filter(DamageData::isAntiSurround)
                    .min(Comparator.comparingDouble(d -> mc.player.squaredDistanceTo(d.getDamageData().toCenterPos())))
                    .orElse(null);
        }
        return best;
    }

    private boolean attackRangeCheck(EndCrystalEntity crystal) {
        Vec3d crystalPos = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
        double dist = mc.player.getEyePos().squaredDistanceTo(crystalPos);
        if (dist > breakRange.get() * breakRange.get()) return true;
        if (Math.abs(crystal.getY() - mc.player.getY()) > maxYOffset.get()) return true;

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), crystalPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() != HitResult.Type.MISS && dist > breakWallRange.get() * breakWallRange.get();
    }

    private boolean placeRangeCheck(BlockPos pos) {
        double dist = placeRangeCenter.get() ?
                mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) :
                pos.getSquaredDistance(mc.player.getEyePos());
        if (dist > placeRange.get() * placeRange.get()) return true;

        Vec3d ray = pos.toCenterPos().add(0, 2.7, 0);
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), ray, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        boolean isWall = result != null && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos);
        return breakValid.get() && isWall && dist > placeWallRange.get() * placeWallRange.get();
    }

    private boolean playerDamageCheck(double dmg) {
        if (mc.player.isCreative()) return false;
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return dmg > maxLocalDamage.get() || (dmg >= health + 0.5 && !safetyOverride.get());
    }

    private boolean isLethal(double dmg, LivingEntity entity) {
        float health = entity.getHealth() + entity.getAbsorptionAmount();
        if (dmg * lethalMultiplier.get() >= health) return true;

        if (antiTotem.get() && entity instanceof PlayerEntity p) {
            Long lastPop = totemPopTimes.get(p);
            if (lastPop != null && System.currentTimeMillis() - lastPop <= 500) {
                if (health - dmg < 0.5) return true;
            }
        }

        if (armorBreaker.get()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = entity.getEquippedStack(slot);
                if (!armor.isEmpty()) {
                    float dur = ((armor.getMaxDamage() - armor.getDamage()) / (float) armor.getMaxDamage()) * 100f;
                    if (dur < armorScale.get()) return true;
                }
            }
        }
        if (shulkers.get() && entity instanceof PlayerEntity) {
            Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            for (BlockPos p : getSphere(3.0f, pos)) {
                if (mc.world.getBlockState(p).getBlock() instanceof ShulkerBoxBlock) return true;
            }
        }
        return false;
    }

    private void attackCrystal(EndCrystalEntity crystal, Hand hand) {
        if (hand == null) return;
        attackInternal(crystal, hand);
    }

    private void attackInternal(EndCrystalEntity crystal, Hand hand) {
        attackPackets.put(crystal.getId(), System.currentTimeMillis());
        Integer count = antiStuck.get(crystal.getId());
        if (count != null) {
            antiStuck.put(crystal.getId(), count + 1);
            if (count + 1 > attackLimit.get() * 10) {
                stuckCrystals.add(new StuckData(crystal.getId(), crystal.getBlockPos(), new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ())));
                return;
            }
        } else {
            antiStuck.put(crystal.getId(), 1);
        }

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    private void placeCrystal(BlockPos pos, Hand hand) {
        if (pos == null || hand == null) return;
        if (!canPlaceCrystal(pos) || placeRangeCheck(pos)) return;

        Direction side = getPlaceDirection(pos);
        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), side, pos, false);

        if (!isHoldingCrystal() && autoSwap.get() != Swap.OFF) {
            int slot = findCrystalSlot();
            if (slot != -1 && lastSwapTimer.passed(500)) {
                if (autoSwap.get() == Swap.SILENT) InventoryManager.getInstance().setSlot(slot);
                else InventoryManager.getInstance().setClientSlot(slot);
                lastSwapTimer.reset();
            }
        }
        if (!isHoldingCrystal()) return;

        placePackets.put(pos, System.currentTimeMillis());
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        if (predictIdSetting.get() && predictedEntityId != -1) {
            int id = (int) predictedEntityId + 1;
            EndCrystalEntity fake = new EndCrystalEntity(mc.world, 0, 0, 0);
            fake.setId(id);
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(fake, false));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            attackPackets.put(id, System.currentTimeMillis());
        }
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return false;
        BlockPos up = pos.up();
        if (!mc.world.isAir(up) && !mc.world.getBlockState(up).isOf(Blocks.FIRE)) return false;
        return mc.world.getOtherEntities(null, new Box(up)).stream()
                .filter(e -> e.isAlive() && !(e instanceof ExperienceOrbEntity))
                .allMatch(e -> e instanceof ItemEntity || (e instanceof EndCrystalEntity && !canBreakCrystal((EndCrystalEntity) e)));
    }

    private boolean canBreakCrystal(EndCrystalEntity e) {
        return !attackRangeCheck(e) && !antiStuck.containsKey(e.getId());
    }

    private EndCrystalEntity getIntersectingCrystal(BlockPos pos) {
        return mc.world.getOtherEntities(null, new Box(pos.up())).stream()
                .filter(e -> e instanceof EndCrystalEntity).map(e -> (EndCrystalEntity) e)
                .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e.getX(), e.getY(), e.getZ())))
                .orElse(null);
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (strictDirection.get()) {
            BlockHitResult res = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(), pos.toCenterPos(), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (res != null && res.getType() == HitResult.Type.BLOCK) return res.getSide();
        }
        return Direction.UP;
    }

    private List<BlockPos> getSphere(Vec3d origin) {
        return getSphere(placeRange.get(), origin);
    }
    private List<BlockPos> getSphere(double rad, Vec3d origin) {
        List<BlockPos> list = new ArrayList<>();
        int r = (int) Math.ceil(rad);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x*x + y*y + z*z <= rad*rad) {
                        list.add(new BlockPos((int)origin.x + x, (int)origin.y + y, (int)origin.z + z));
                    }
                }
            }
        }
        return list;
    }

    private boolean isHoldingCrystal() {
        return mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ||
                mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    private Hand getCrystalHand() {
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) return Hand.OFF_HAND;
        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) return Hand.MAIN_HAND;
        return null;
    }

    private boolean canHoldCrystal() {
        return isHoldingCrystal() || findCrystalSlot() != -1;
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    private float getBreakDelay() {
        return (float) (1000.0 - breakSpeed.get() * 50.0);
    }

    private int getBreakMs() {
        synchronized (breakLatencies) {
            if (breakLatencies.isEmpty()) return 0;
            long sum = 0;
            for (Long t : breakLatencies) sum += t;
            return (int) (sum / breakLatencies.size());
        }
    }

    private boolean isValidTarget(Entity e) {
        if (e instanceof PlayerEntity p) return players.get() && !Friends.get().isFriend(p);
        if (EntityUtil.isMonster(e)) return monsters.get();
        if (EntityUtil.isNeutral(e)) return neutrals.get();
        if (EntityUtil.isPassive(e)) return animals.get();
        return false;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;
        Packet<?> p = event.packet;

        if (p instanceof ExplosionS2CPacket || p instanceof PlaySoundS2CPacket sound &&
                sound.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof EndCrystalEntity) {
                    Long time = attackPackets.remove(e.getId());
                    if (time != null) breakLatencies.add(System.currentTimeMillis() - time);
                    antiStuck.remove(e.getId());
                }
            }
        }
        if (p instanceof EntitiesDestroyS2CPacket destroy) {
            for (int id : destroy.getEntityIds()) {
                Long time = attackPackets.remove(id);
                if (time != null) breakLatencies.add(System.currentTimeMillis() - time);
                antiStuck.remove(id);
            }
        }

        if (p instanceof EntitySpawnS2CPacket spawn) {
            lastEntityId = spawn.getEntityId();
            predictedEntityId = spawn.getEntityId();
            if (!instant.get()) return;

            EndCrystalEntity crystal = null;
            BlockPos below = null;

            Entity entity = mc.world.getEntityById(spawn.getEntityId());
            if (entity instanceof EndCrystalEntity) {
                crystal = (EndCrystalEntity) entity;
                below = crystal.getBlockPos().down();
            } else {
                Vec3d pos = new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ());
                crystal = new EndCrystalEntity(mc.world, pos.x, pos.y, pos.z);
                crystal.setId(spawn.getEntityId());
                below = BlockPos.ofFloored(pos.add(0, -1, 0));
            }

            Long placeTime = placePackets.remove(below);
            if (placeTime == null) return;

            if (instantCalc.get()) {
                double self = ExplosionUtil.getDamageTo(mc.player, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()),
                        blockDestruction.get(), selfExtrapolate.get() ? extrapolateTicks.get() : 0, assumeBestArmor.get());
                if (playerDamageCheck(self)) return;

                DamageData<EndCrystalEntity> bestInstant = null;
                for (Entity target : mc.world.getEntities()) {
                    if (!isValidTarget(target) || target == mc.player) continue;
                    double dmg = ExplosionUtil.getDamageTo(target, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()),
                            blockDestruction.get(), extrapolateTicks.get(), assumeBestArmor.get());
                    if (dmg < instantDamage.get()) continue;
                    if (dmg >= minDamage.get() || isLethal(dmg, (LivingEntity) target)) {
                        DamageData<EndCrystalEntity> data = new DamageData<>(crystal, target, dmg, self, below);
                        if (bestInstant == null || (instantMax.get() && dmg > bestInstant.getDamage())) {
                            bestInstant = data;
                        }
                    }
                }
                if (bestInstant != null) {
                    if (!rotate.get() || rotated) {
                        attackCrystal(crystal, getCrystalHand());
                        lastBreakTimer.reset();
                        if (sequential.get() && placeCrystal != null) {
                            placeCrystal(placeCrystal.getDamageData(), getCrystalHand());
                        }
                    }
                }
            } else {
                if (!rotate.get() || rotated) {
                    attackCrystal(crystal, getCrystalHand());
                    lastBreakTimer.reset();
                    if (sequential.get() && placeCrystal != null) {
                        placeCrystal(placeCrystal.getDamageData(), getCrystalHand());
                    }
                }
            }
        }

        if (p instanceof EntityStatusS2CPacket status && status.getStatus() == 35) {
            if (status.getEntity(mc.world) instanceof PlayerEntity player) {
                totemPopTimes.put(player, System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        fades.entrySet().removeIf(e -> e.getValue().getFactor() <= 0);
        for (Map.Entry<BlockPos, FadeAnimation> entry : fades.entrySet()) {
            if (entry.getKey().equals(renderPos)) continue;
            float f = entry.getValue().getFactor();
            event.renderer.box(entry.getKey(),
                    new Color(placeColor.get().r, placeColor.get().g, placeColor.get().b, (int)(40 * f)),
                    new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, (int)(100 * f)),
                    ShapeMode.Both, 0);
        }

        if (renderPos != null && isHoldingCrystal()) {
            event.renderer.box(renderPos, placeColor.get(), lineColor.get(), ShapeMode.Both, 0);
            fades.put(renderPos, new FadeAnimation(true, fadeTime.get()));
        }
    }

    private enum YawStep { FULL, SEMI, OFF }
    private enum Swap { NORMAL, SILENT, OFF }

    private record StuckData(int id, BlockPos blockPos, Vec3d pos) {}

    private static class DamageData<T> {
        private final T damageData;
        private final Entity attackTarget;
        private final BlockPos blockPos;
        private final double damage, selfDamage;
        private final boolean antiSurround;

        public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos) {
            this(damageData, attackTarget, damage, selfDamage, blockPos, false);
        }

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

    private static class FadeAnimation {
        private boolean active; private long start; private int duration;
        public FadeAnimation(boolean active, int duration) {
            this.active = active; this.start = System.currentTimeMillis(); this.duration = duration;
        }
        public float getFactor() {
            if (!active) return 0;
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= duration) { active = false; return 0; }
            return 1.0f - (float) elapsed / duration;
        }
    }
}