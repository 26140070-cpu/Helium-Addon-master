package com.helium.modules.legit.MacePvP;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class LegitTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRestock = settings.createGroup("Restock");

    private final Setting<Integer> hotbarSlot = sgGeneral.add(new IntSetting.Builder()
            .name("hotbar-slot")
            .description("Hotbar slot (0–8) for the backup totem.")
            .defaultValue(0)
            .min(0).max(8).sliderMax(8)
            .build()
    );

    private final Setting<Integer> swapDelay = sgGeneral.add(new IntSetting.Builder()
            .name("swap-delay")
            .description("Base ticks after totem pop before selecting backup slot and swapping hands.")
            .defaultValue(1)
            .min(0).max(10).sliderMax(10)
            .build()
    );

    private final Setting<Boolean> randomizeDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("randomize-delay")
            .description("Add slight random variation to delays.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoRestock = sgRestock.add(new BoolSetting.Builder()
            .name("auto-restock")
            .description("Automatically open inventory and move a totem to the backup slot if it's empty.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> restockOpenDelay = sgRestock.add(new IntSetting.Builder()
            .name("restock-open-delay")
            .description("Base ticks before opening inventory for restock.")
            .defaultValue(10)
            .min(1).max(40).sliderMax(40)
            .visible(autoRestock::get)
            .build()
    );

    private final Setting<Integer> restockMoveDelay = sgRestock.add(new IntSetting.Builder()
            .name("restock-move-delay")
            .description("Ticks after opening inventory before moving the totem.")
            .defaultValue(4)
            .min(1).max(20).sliderMax(20)
            .visible(autoRestock::get)
            .build()
    );

    private final Setting<Integer> restockCloseDelay = sgRestock.add(new IntSetting.Builder()
            .name("restock-close-delay")
            .description("Ticks after moving totem before closing inventory.")
            .defaultValue(6)
            .min(2).max(30).sliderMax(30)
            .visible(autoRestock::get)
            .build()
    );

    private final Setting<Boolean> allowHotbarMove = sgRestock.add(new BoolSetting.Builder()
            .name("allow-hotbar-move")
            .description("Also move totems from other hotbar slots to the backup slot.")
            .defaultValue(false)
            .visible(autoRestock::get)
            .build()
    );

    private final Setting<Boolean> disableLogs = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-logs")
            .description("Disable chat messages.")
            .defaultValue(true)
            .build()
    );

    private enum State {
        IDLE,
        WAITING_FOR_SWAP,
        RESTOCK_OPENING,
        RESTOCK_MOVING,
        RESTOCK_CLOSING
    }

    private State state = State.IDLE;
    private int timer = 0;
    private boolean backupSlotHasTotem = false;
    private boolean offhandHasTotem = false;

    private final Random random = new Random();

    public LegitTotem() {
        super(HeliumAddon.LEGIT_MISC, "auto-legit-totem", "Fast hotbar totem swap with auto restock.");
    }

    @Override
    public void onActivate() {
        updateStatus();
        if (!backupSlotHasTotem && hasTotemInInventory()) {
            scheduleRestock();
        }
    }

    @Override
    public void onDeactivate() {
        state = State.IDLE;
        timer = 0;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        onActivate();
    }

    @EventHandler
    private void onEntityStatus(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() == 35 && mc.player != null && packet.getEntity(mc.world) == mc.player) {
            onTotemPop();
        }
    }

    private void onTotemPop() {
        if (!offhandHasTotem) return;
        offhandHasTotem = false;
        backupSlotHasTotem = isBackupSlotTotem();
        if (backupSlotHasTotem) {
            state = State.WAITING_FOR_SWAP;
            timer = getDelay(swapDelay.get());
            if (!disableLogs.get()) info("Totem popped! Swapping to backup…");
        } else {
            if (!disableLogs.get()) info("Totem popped and backup empty! Restocking…");
            scheduleRestock();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        updateStatus();

        if (timer > 0) {
            timer--;
            if (timer == 0) {
                executeStateAction();
            }
        }
    }

    private void executeStateAction() {
        switch (state) {
            case WAITING_FOR_SWAP -> {
                performHotbarSwap();
                state = State.IDLE;
                backupSlotHasTotem = false;
                if (!backupSlotHasTotem && hasTotemInInventory()) {
                    scheduleRestock();
                }
            }
            case RESTOCK_OPENING -> {
                if (mc.currentScreen == null && mc.player != null) {
                    mc.setScreen(new InventoryScreen(mc.player));
                    state = State.RESTOCK_MOVING;
                    timer = getDelay(restockMoveDelay.get());
                } else {
                    state = State.IDLE;
                }
            }
            case RESTOCK_MOVING -> {
                moveTotemToBackupSlot();
                state = State.RESTOCK_CLOSING;
                timer = getDelay(restockCloseDelay.get());
            }
            case RESTOCK_CLOSING -> {
                if (mc.currentScreen instanceof InventoryScreen) {
                    mc.setScreen(null);
                }
                state = State.IDLE;
                backupSlotHasTotem = isBackupSlotTotem();
                if (!backupSlotHasTotem && hasTotemInInventory()) {
                    scheduleRestock();
                }
            }
        }
    }

    private void performHotbarSwap() {
        if (mc.player == null) return;
        ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(hotbarSlot.get());
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot.get()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (!disableLogs.get()) info("Swapped to backup totem.");
    }

    private void moveTotemToBackupSlot() {
        if (mc.player == null || !(mc.currentScreen instanceof InventoryScreen)) return;

        int sourceSlot = findTotemSlotForRestock();
        if (sourceSlot == -1) {
            if (!disableLogs.get()) warning("No totems found to restock!");
            return;
        }

        int containerSrc = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;
        int containerDst = 36 + hotbarSlot.get();

        try {
            mc.interactionManager.clickSlot(0, containerSrc, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, containerDst, 0, SlotActionType.PICKUP, mc.player);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(0, containerSrc, 0, SlotActionType.PICKUP, mc.player);
            }
            if (!disableLogs.get()) info("Backup slot restocked.");
        } catch (Exception e) {
            if (!disableLogs.get()) error("Restock failed: " + e.getMessage());
        }
    }

    private void scheduleRestock() {
        if (!autoRestock.get()) return;
        state = State.RESTOCK_OPENING;
        timer = getDelay(restockOpenDelay.get());
        if (!disableLogs.get()) info("Scheduled auto restock in %d ticks.", timer);
    }

    private void updateStatus() {
        if (mc.player == null) return;
        offhandHasTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        backupSlotHasTotem = isBackupSlotTotem();
    }

    private boolean isBackupSlotTotem() {
        return mc.player != null && mc.player.getInventory().getStack(hotbarSlot.get()).isOf(Items.TOTEM_OF_UNDYING);
    }

    private boolean hasTotemInInventory() {
        for (int i = 0; i < 36; i++) {
            if (i == hotbarSlot.get()) continue;
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private int findTotemSlotForRestock() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        if (allowHotbarMove.get()) {
            for (int i = 0; i < 9; i++) {
                if (i == hotbarSlot.get()) continue;
                if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
            }
        }
        return -1;
    }

    private int getDelay(int base) {
        if (randomizeDelay.get()) {
            return Math.max(0, base + random.nextInt(3) - 1);
        }
        return base;
    }
}