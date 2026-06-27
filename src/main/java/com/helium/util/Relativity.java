package com.helium.util;

import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Relativity {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void clickSlot(int slot, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
    }

    public static int invIndexToSlotId(int invIndex) {
        if (invIndex < 9 && invIndex != -1) return 44 - (8 - invIndex);
        return invIndex;
    }

    public static void swap(int slot) {
        if (slot != ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot() && slot >= 0 && slot < 9) {
            ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(slot);
        }
    }

    public static boolean equalsBlockPos(BlockPos p1, BlockPos p2) {
        if (!(p1 instanceof Vec3i) || !(p2 instanceof Vec3i)) return false;
        if (p1 == null || p2 == null) return false;
        if (p1.getX() != p2.getX()) return false;
        if (p1.getY() != p2.getY()) return false;
        if (p1.getZ() != p2.getZ()) return false;
        return true;
    }

    public static BlockPos SetRelative(int x, int y, int z) {
        return new BlockPos(
                (int) Math.floor(mc.player.getX() + x),
                (int) Math.floor(mc.player.getY() + y),
                (int) Math.floor(mc.player.getZ() + z)
        );
    }

    public static boolean BlockPlace(int x, int y, int z, int HotbarSlot, boolean Rotate) {
        return BlockPlace(new BlockPos(x, y, z), HotbarSlot, Rotate);
    }

    public static boolean BlockPlace(BlockPos blockPos, int HotbarSlot, boolean Rotate) {
        if (HotbarSlot == -1) return false;
        if (!BlockUtils.canPlace(blockPos, true)) return false;

        int preSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        Relativity.swap(HotbarSlot);

        if (Rotate) {
            Vec3d hitPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos));
        }

        BlockHitResult hitResult = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, blockPos, true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        Relativity.swap(preSlot);
        return true;
    }

    public static double DistanceTo(BlockPos pos) {
        return DistanceTo(pos.getX(), pos.getY(), pos.getZ());
    }

    public static double DistanceTo(int x, double y, double z) {
        double X = x;
        double Y = y;
        double Z = z;
        if (X >= 0) { X = X + 0.5; } else { X = X - 0.5; }
        if (Y >= 0) { Y = Y + 0.5; } else { Y = Y - 0.5; }
        if (Z >= 0) { Z = Z + 0.5; } else { Z = Z - 0.5; }
        double f = mc.player.getX() - X;
        double g = mc.player.getY() - Y;
        double h = mc.player.getZ() - Z;
        return Math.sqrt(f * f + g * g + h * h);
    }

    public static void interact(BlockPos pos, int HotbarSlot, Direction dir) {
        int preSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        Relativity.swap(HotbarSlot);

        BlockHitResult hitResult = new BlockHitResult(mc.player.getEyePos(), dir, pos, true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        Relativity.swap(preSlot);
    }

    public static void attackEntity(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
    }

    public static boolean isFriend(PlayerEntity player) {
        return Friends.get().isFriend(player);
    }

    public static boolean isFriend(String playerName) {
        return (Friends.get().get(playerName) != null);
    }

    public static double distanceToBlockAnge(BlockPos pos) {
        double x1 = mc.player.getX();
        double y1 = mc.player.getY() + 1;
        double z1 = mc.player.getZ();

        double x2 = pos.getX();
        double y2 = pos.getY();
        double z2 = pos.getZ();

        if (y2 == Relativity.floor(y1)) y2 = y1;
        if (x2 > 0 && x2 == Relativity.floor(x1)) x2 = x1;
        if (x2 < 0 && x2 + 1 == Relativity.floor(x1)) x2 = x1;
        if (z2 > 0 && z2 == Relativity.floor(z1)) z2 = z1;
        if (z2 < 0 && z2 + 1 == Relativity.floor(z1)) z2 = z1;
        if (x2 < x1) x2++;
        if (y2 < y1) y2++;
        if (z2 < z1) z2++;

        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double floor(double d) {
        return (long) d;
    }
}