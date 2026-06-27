package com.helium.modules.combat;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BasicAnchorAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .defaultValue(5.0)
            .min(1.0)
            .sliderMax(6.0)
            .build()
    );

    private final Setting<Boolean> fastMode = sgGeneral.add(new BoolSetting.Builder()
            .name("fast-mode")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> bypassRestrictions = sgGeneral.add(new BoolSetting.Builder()
            .name("bypass-restrictions")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air-place")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotations = sgGeneral.add(new BoolSetting.Builder()
            .name("silent-rotations")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoRefill = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-refill")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-suicide")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> safeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("safe-distance")
            .defaultValue(4.0)
            .min(1.0)
            .sliderMax(10.0)
            .visible(antiSuicide::get)
            .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-damage")
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
            .name("action-delay")
            .defaultValue(1)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    private int timer;
    private BlockPos currentPos;
    private int predictedCharge;

    public BasicAnchorAura() {
        super(HeliumAddon.CATEGORY, "HeliumBAA", "Anchor Aura.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        currentPos = null;
        predictedCharge = 0;
    }

    public boolean bypassesRestrictions() {
        return bypassRestrictions.get();
    }

    public void onExplosion(ExplosionS2CPacket packet) {
        if (currentPos == null) return;

        if (packet.center().distanceTo(new Vec3d(currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5)) < 2.0) {
            timer = 0;
            predictedCharge = 0;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.world.getRegistryKey() == World.NETHER) return;

        if (timer > 0) {
            timer--;
            return;
        }

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target == null) {
            currentPos = null;
            predictedCharge = 0;
            return;
        }

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double tX = target.getX();
        double tY = target.getY();
        double tZ = target.getZ();

        if (antiSuicide.get()) {
            double dx = pX - tX;
            double dy = pY - tY;
            double dz = pZ - tZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < safeDistance.get()) return;
        }

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (autoRefill.get()) {
            if (!anchor.found()) {
                FindItemResult invAnchor = InvUtils.find(Items.RESPAWN_ANCHOR);
                if (invAnchor.found()) InvUtils.move().from(invAnchor.slot()).toHotbar(8);
                anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            }
            if (!glowstone.found()) {
                FindItemResult invGlowstone = InvUtils.find(Items.GLOWSTONE);
                if (invGlowstone.found()) InvUtils.move().from(invGlowstone.slot()).toHotbar(7);
                glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
            }
        }

        if (!anchor.found() || !glowstone.found()) return;

        BlockPos optimal = getOptimalPos(target);
        if (optimal == null) return;

        if (currentPos != null && (currentPos.getX() != optimal.getX() || currentPos.getY() != optimal.getY() || currentPos.getZ() != optimal.getZ())) {
            predictedCharge = 0;
        }
        currentPos = optimal;

        BlockState state = mc.world.getBlockState(currentPos);

        if (fastMode.get() && state.isReplaceable()) {
            if (airPlace.get() && mc.world.getBlockState(currentPos.down()).isReplaceable()) {
                FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);
                if (obby.found()) silentInteract(currentPos.down(), obby.slot());
            }
            silentInteract(currentPos, anchor.slot());
            silentInteract(currentPos, glowstone.slot());
            silentInteract(currentPos, anchor.slot());
            predictedCharge = 0;
            timer = actionDelay.get();
            return;
        }

        if (airPlace.get() && state.isReplaceable() && mc.world.getBlockState(currentPos.down()).isReplaceable()) {
            FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);
            if (obby.found()) {
                silentInteract(currentPos.down(), obby.slot());
                return;
            }
        }

        if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
            int actualCharge = state.get(Properties.CHARGES);
            int charges = Math.max(actualCharge, predictedCharge);

            if (charges == 0) {
                silentInteract(currentPos, glowstone.slot());
                predictedCharge = 4;
            } else {
                silentInteract(currentPos, anchor.slot());
                predictedCharge = 0;
            }
        } else if (state.isReplaceable()) {
            silentInteract(currentPos, anchor.slot());
            predictedCharge = 0;
        }
    }

    private BlockPos getOptimalPos(PlayerEntity target) {
        double tX = target.getX();
        double tY = target.getY();
        double tZ = target.getZ();
        BlockPos targetBlock = BlockPos.ofFloored(tX, tY, tZ);

        BlockPos bestPos = null;
        double maxDmg = 0;

        BlockPos[] offsets = {
                targetBlock.down(),
                targetBlock,
                targetBlock.up(),
                targetBlock.up(2),
                targetBlock.north(),
                targetBlock.south(),
                targetBlock.east(),
                targetBlock.west()
        };

        for (BlockPos pos : offsets) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isReplaceable() && !state.isOf(Blocks.RESPAWN_ANCHOR)) continue;

            double cX = pos.getX() + 0.5;
            double cY = pos.getY() + 0.5;
            double cZ = pos.getZ() + 0.5;
            Vec3d expPos = new Vec3d(cX, cY, cZ);

            double damage = DamageUtils.crystalDamage(target, expPos);
            if (damage < minDamage.get()) continue;

            double selfDamage = DamageUtils.crystalDamage(mc.player, expPos);
            if (selfDamage > maxSelfDamage.get()) continue;

            if (damage > maxDmg) {
                maxDmg = damage;
                bestPos = pos;
            }
        }

        return bestPos;
    }

    private void silentInteract(BlockPos pos, int targetSlot) {
        int previousSlot = ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getSelectedSlot();

        ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(targetSlot);
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

        double hX = pos.getX() + 0.5;
        double hY = pos.getY() + 0.5;
        double hZ = pos.getZ() + 0.5;

        if (rotations.get()) {
            double dX = hX - mc.player.getX();
            double dY = hY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dZ = hZ - mc.player.getZ();
            double dist = Math.sqrt(dX * dX + dZ * dZ);
            float yaw = (float) Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(dY, dist));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), false));
        }

        Vec3d hitVec = new Vec3d(hX, hY, hZ);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));

        ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(previousSlot);
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

        timer = actionDelay.get();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && currentPos != null) {
            event.renderer.box(currentPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}