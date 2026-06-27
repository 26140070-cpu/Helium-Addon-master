package com.helium.modules.utility;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class UltraSurround extends Module {

    public enum SurroundMode { Normal, Double }
    public enum CenterMode { Fast, Legit, None }

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgToggles = settings.createGroup("Toggles");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<SurroundMode> surroundMode = sgPlace.add(new EnumSetting.Builder<SurroundMode>()
            .name("mode")
            .defaultValue(SurroundMode.Normal)
            .build()
    );

    private final Setting<CenterMode> centerMode = sgPlace.add(new EnumSetting.Builder<CenterMode>()
            .name("center")
            .defaultValue(CenterMode.Legit)
            .build()
    );

    private final Setting<Integer> tickDelay = sgPlace.add(new IntSetting.Builder()
            .name("tick-delay")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgPlace.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .defaultValue(2)
            .min(1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> antiCrystal = sgToggles.add(new BoolSetting.Builder()
            .name("anti-crystal-aura")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiCev = sgToggles.add(new BoolSetting.Builder()
            .name("anti-cev-breaker")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgToggles.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> disableOnJump = sgToggles.add(new BoolSetting.Builder()
            .name("disable-on-jump")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgToggles.add(new BoolSetting.Builder()
            .name("rotate")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .defaultValue(ShapeMode.Lines)
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(render::get)
            .build()
    );

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int tickTimer;
    private int actionsThisTick;

    private static final List<Vec3d> NORMAL_OFFSETS = List.of(
            new Vec3d(0, -1, 0),
            new Vec3d(1, 0, 0), new Vec3d(-1, 0, 0),
            new Vec3d(0, 0, 1), new Vec3d(0, 0, -1)
    );

    private static final List<Vec3d> DOUBLE_EXTRA = List.of(
            new Vec3d(1, 1, 0), new Vec3d(-1, 1, 0),
            new Vec3d(0, 1, 1), new Vec3d(0, 1, -1)
    );

    public UltraSurround() {
        super(HeliumAddon.CATEGORY, "ultra-surround", "Elite surround module with anti-kick and smart center.");
    }

    @Override
    public void onActivate() {
        tickTimer = 0;
        actionsThisTick = 0;
        if (centerMode.get() != CenterMode.None) applyCenter(CenterMode.Fast.equals(centerMode.get()) ? 0.3 : 0.185);
    }

    @Override
    public void onDeactivate() {
        actionsThisTick = 0;
    }

    private void applyCenter(double strength) {
        double x = mc.player.getX();
        double z = mc.player.getZ();
        double decX = Math.abs(x - Math.floor(x));
        double decZ = Math.abs(z - Math.floor(z));
        double moveX = 0, moveZ = 0;

        if (decX > 0.7) moveX = -strength;
        else if (decX < 0.3) moveX = strength;

        if (decZ > 0.7) moveZ = -strength;
        else if (decZ < 0.3) moveZ = strength;

        if (moveX != 0 || moveZ != 0) {
            double newX = mc.player.getX() + moveX;
            double newZ = mc.player.getZ() + moveZ;
            mc.player.updatePosition(newX, mc.player.getY(), newZ);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround(), mc.player.horizontalCollision
            ));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (disableOnJump.get() && mc.options.jumpKey.isPressed()) {
            if (isActive()) toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        if (tickTimer < tickDelay.get()) {
            tickTimer++;
            return;
        }
        tickTimer = 0;
        actionsThisTick = 0;

        BlockPos playerPos = mc.player.getBlockPos();
        List<Vec3d> design = getFullDesign();

        for (Vec3d offset : design) {
            if (actionsThisTick >= blocksPerTick.get()) break;

            BlockPos targetPos = playerPos.add((int) offset.x, (int) offset.y, (int) offset.z);
            if (mc.world.getBlockState(targetPos).isAir()) {
                placeBlock(targetPos);
            }
        }
    }

    private List<Vec3d> getFullDesign() {
        List<Vec3d> design = new ArrayList<>(NORMAL_OFFSETS);
        if (surroundMode.get() == SurroundMode.Double) design.addAll(DOUBLE_EXTRA);
        if (antiCev.get()) design.addAll(List.of(
                new Vec3d(1, 1, 0), new Vec3d(-1, 1, 0),
                new Vec3d(0, 1, 1), new Vec3d(0, 1, -1),
                new Vec3d(0, 2, 0), new Vec3d(0, 3, 0)
        ));
        if (antiCrystal.get()) design.addAll(List.of(
                new Vec3d(1, -1, 0), new Vec3d(-1, -1, 0),
                new Vec3d(0, -1, 1), new Vec3d(0, -1, -1)
        ));
        return design;
    }

    private void placeBlock(BlockPos pos) {
        int slot = InvUtils.findInHotbar(Items.OBSIDIAN).slot();
        if (slot == -1) return;

        int prevSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        silentSwap(slot);
        BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, false);
        silentSwap(prevSlot);
        actionsThisTick++;
    }

    private void silentSwap(int slot) {
        ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || mc.player == null) return;
        BlockPos p = mc.player.getBlockPos();
        for (Vec3d offset : getFullDesign()) {
            BlockPos bp = p.add((int) offset.x, (int) offset.y, (int) offset.z);
            if (mc.world.getBlockState(bp).isAir()) {
                event.renderer.box(bp, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}