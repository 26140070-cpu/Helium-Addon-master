package com.helium.modules.utility;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FastBreak extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .defaultValue(20)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .defaultValue(5)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<Boolean> autoCity = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-city-break")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> crystalBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("crystal-aura-break")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> smash = sgGeneral.add(new BoolSetting.Builder()
            .name("smash")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> obbyOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("only-obsidian")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder()
            .name("silent-swap")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> maxPackets = sgGeneral.add(new IntSetting.Builder()
            .name("max-packets")
            .description("Maximum packets per tick to avoid kicks.")
            .defaultValue(4)
            .min(1)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .defaultValue(new SettingColor(152, 251, 152, 40))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .defaultValue(new SettingColor(152, 251, 152, 255))
            .build()
    );

    public FastBreak() {
        super(HeliumAddon.CATEGORY, "Fast Break", "Elite fast block break with smart tool swap and crystal integration.");
    }

    private PlayerEntity target;
    private BlockPos pos;
    private BlockPos lastRenderedPos;
    private int ticks;
    private int renderTicks;

    @Override
    public void onActivate() {
        pos = null;
        lastRenderedPos = null;
        ticks = 0;
        renderTicks = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        BlockPos drawPos = (pos != null) ? pos : (renderTicks > 0 ? lastRenderedPos : null);
        if (drawPos != null) {
            event.renderer.box(drawPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet && autoCity.get()) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                pos = packet.getPos();
            }
        }
    }

    @EventHandler
    public void onStartBreakingBlock(StartBreakingBlockEvent event) {
        pos = event.blockPos;
        Block block = mc.world.getBlockState(pos).getBlock();

        if (obbyOnly.get() && block != Blocks.OBSIDIAN) {
            pos = null;
            return;
        }

        if (block == Blocks.BEDROCK || block == Blocks.NETHER_PORTAL || block == Blocks.END_GATEWAY
                || block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME || block == Blocks.BARRIER) {
            pos = null;
            return;
        }

        if (smash.get() && mc.player.isOnGround() && canInstaMine(block)) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP
            ));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
            ));
        }
    }

    private boolean canInstaMine(Block block) {
        var mainHandItem = mc.player.getMainHandStack().getItem();
        var state = block.getDefaultState();

        if (block.asItem().toString().contains("_leaves") || block.asItem().toString().contains("_wart")) return false;

        return (mainHandItem == Items.NETHERITE_PICKAXE || mainHandItem == Items.DIAMOND_PICKAXE) && state.isIn(BlockTags.PICKAXE_MINEABLE)
                || (mainHandItem == Items.NETHERITE_AXE || mainHandItem == Items.DIAMOND_AXE) && state.isIn(BlockTags.AXE_MINEABLE);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (renderTicks > 0) renderTicks--;
        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (pos == null) return;

        if (ticks < delay.get()) {
            ticks++;
            return;
        }
        ticks = 0;

        int prevSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        int actions = 0;
        int maxActions = maxPackets.get();

        if (crystalBreak.get() && canPlaceCrystalOn(pos)) {
            int crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).slot();
            if (crystalSlot != -1 && crystalSlot < 9 && actions < maxActions) {
                doSwap(crystalSlot);
                Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
                actions++;

                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity && entity.getBlockPos().down().equals(pos)) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        mc.player.swingHand(Hand.MAIN_HAND);
                        actions++;
                        break;
                    }
                }
            }
        }

        int bestToolSlot = InvUtils.findFastestTool(mc.world.getBlockState(pos)).slot();
        if (bestToolSlot != -1 && bestToolSlot < 9 && actions < maxActions) {
            doSwap(bestToolSlot);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
            ));
            actions++;
        }

        if (silent.get()) doSwap(prevSlot);

        lastRenderedPos = pos;
        renderTicks = 6;
        pos = null;
    }

    private void doSwap(int slot) {
        ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private boolean canPlaceCrystalOn(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
    }
}