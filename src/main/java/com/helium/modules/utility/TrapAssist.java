package com.helium.modules.utility;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class TrapAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .defaultValue(4.5)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .defaultValue(3)
            .min(1)
            .sliderMax(5)
            .build()
    );

    public TrapAssist() {
        super(HeliumAddon.CATEGORY, "trap-assist", "Traps the enemy leaving a hole.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target == null) return;

        FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obby.found()) return;

        List<BlockPos> trapPositions = getTrapPositions(target);

        int blocksPlaced = 0;
        int previousSlot = ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getSelectedSlot();
        boolean swapped = false;

        for (BlockPos pos : trapPositions) {
            if (blocksPlaced >= blocksPerTick.get()) break;

            if (mc.world.getBlockState(pos).isReplaceable()) {
                if (!swapped) {
                    ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(obby.slot());
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    swapped = true;
                }

                double hX = pos.getX() + 0.5;
                double hY = pos.getY() + 0.5;
                double hZ = pos.getZ() + 0.5;
                Vec3d hitVec = new Vec3d(hX, hY, hZ);
                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);

                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
                blocksPlaced++;
            }
        }

        if (swapped) {
            ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(previousSlot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
        }
    }

    private List<BlockPos> getTrapPositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();

        int tX = (int) Math.floor(target.getX());
        int tY = (int) Math.floor(target.getY());
        int tZ = (int) Math.floor(target.getZ());

        positions.add(new BlockPos(tX, tY, tZ - 1));
        positions.add(new BlockPos(tX, tY, tZ + 1));
        positions.add(new BlockPos(tX + 1, tY, tZ));
        positions.add(new BlockPos(tX - 1, tY, tZ));

        positions.add(new BlockPos(tX, tY + 2, tZ));

        positions.add(new BlockPos(tX, tY + 1, tZ - 1));

        return positions;
    }
}