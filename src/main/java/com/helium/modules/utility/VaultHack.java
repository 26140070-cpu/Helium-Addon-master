package com.helium.modules.utility;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.VaultBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.enums.VaultState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultHack extends Module {

    private final Setting<List<Item>> triggerItems = settings.getDefaultGroup().add(new ItemListSetting.Builder()
            .name("trigger-items")
            .description("items to trigger")
            .defaultValue(Items.HEAVY_CORE)
            .build()
    );

    private final Setting<Double> openRange = settings.getDefaultGroup().add(new DoubleSetting.Builder()
            .name("open-range")
            .description("radius for vault search")
            .defaultValue(5.0)
            .min(1.0).max(10.0)
            .sliderMin(1.0).sliderMax(10.0)
            .build()
    );

    private final Setting<Integer> openDelay = settings.getDefaultGroup().add(new IntSetting.Builder()
            .name("open-delay")
            .description("ticks to wait before opening")
            .defaultValue(0)
            .min(0).max(200)
            .sliderMin(0).sliderMax(100)
            .build()
    );

    private final Setting<Boolean> chatNotify = settings.getDefaultGroup().add(new BoolSetting.Builder()
            .name("chat-notify")
            .description("notify about items in chat")
            .defaultValue(true)
            .build()
    );

    private BlockPos pendingVaultPos = null;
    private int pendingTicks = -1;
    private final Map<Long, Item> lastKnownItems = new HashMap<>();

    public VaultHack() {
        super(HeliumAddon.CATEGORY, "VaultHack", "Auto open vaults.");
    }

    @Override
    public void onDeactivate() {
        pendingVaultPos = null;
        pendingTicks = -1;
        lastKnownItems.clear();
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (chatNotify.get()) scanVaults();

        if (pendingVaultPos != null) {
            if (!isVaultValid(pendingVaultPos)) { reset(); return; }
            if (--pendingTicks > 0) return;
            openVault(pendingVaultPos);
            reset();
            return;
        }

        for (BlockPos pos : nearbyVaults()) {
            ItemStack display = getDisplay(pos);
            if (display == null || !triggerItems.get().contains(display.getItem())) continue;

            if (openDelay.get() <= 0) {
                openVault(pos);
            } else {
                pendingVaultPos = pos;
                pendingTicks = openDelay.get();
            }
            break;
        }
    }

    private void reset() {
        pendingVaultPos = null;
        pendingTicks = -1;
    }

    private List<BlockPos> nearbyVaults() {
        int ri = (int) Math.ceil(openRange.get());
        BlockPos origin = mc.player.getBlockPos();
        Vec3d eye = mc.player.getEyePos();
        List<BlockPos> result = new ArrayList<>();

        for (int x = -ri; x <= ri; x++)
            for (int y = -ri; y <= ri; y++)
                for (int z = -ri; z <= ri; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (Vec3d.ofCenter(pos).distanceTo(eye) > openRange.get()) continue;
                    if (isVaultValid(pos)) result.add(pos);
                }
        return result;
    }

    private boolean isVaultValid(BlockPos pos) {
        var state = mc.world.getBlockState(pos);
        if (state.getBlock() != Blocks.VAULT) return false;
        if (!state.get(VaultBlock.OMINOUS)) return false;
        if (state.get(VaultBlock.VAULT_STATE) != VaultState.ACTIVE) return false;
        if (Vec3d.ofCenter(pos).distanceTo(mc.player.getEyePos()) > openRange.get()) return false;
        return mc.world.getBlockEntity(pos) instanceof VaultBlockEntity;
    }

    private ItemStack getDisplay(BlockPos pos) {
        BlockEntity be = mc.world.getBlockEntity(pos);
        if (!(be instanceof VaultBlockEntity vault)) return null;
        ItemStack display = vault.getSharedData().getDisplayItem();
        return display.isEmpty() ? null : display;
    }

    private void scanVaults() {
        for (BlockPos pos : nearbyVaults()) {
            ItemStack display = getDisplay(pos);
            long key = pos.asLong();

            if (display == null) { lastKnownItems.remove(key); continue; }

            Item current = display.getItem();
            if (current == lastKnownItems.get(key)) continue;

            lastKnownItems.put(key, current);
            boolean trigger = triggerItems.get().contains(current);
            MutableText msg = Text.literal(
                    "[Vault] " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                            + " : " + display.getName().getString()
            );
            if (trigger) msg.append(Text.literal(" [triggered]").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            ChatUtils.sendMsg(msg);
        }
    }

    private void openVault(BlockPos pos) {
        FindItemResult key = InvUtils.findInHotbar(Items.OMINOUS_TRIAL_KEY);
        if (!key.found()) return;

        InvUtils.swap(key.slot(), false);
        mc.interactionManager.interactBlock(
                mc.player,
                Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.NORTH, pos, false)
        );
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}