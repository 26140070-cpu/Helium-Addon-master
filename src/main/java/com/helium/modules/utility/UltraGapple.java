package com.helium.modules.utility;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class UltraGapple extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder().name("health").defaultValue(15.0).min(0.0).max(36.0).build());
    private final Setting<Boolean> weaponOnly = sgGeneral.add(new BoolSetting.Builder().name("weapon-only").defaultValue(false).build());
    private final Setting<Boolean> rightClick = sgGeneral.add(new BoolSetting.Builder().name("right-click-weapon").defaultValue(true).build());
    private final Setting<Boolean> egapple = sgGeneral.add(new BoolSetting.Builder().name("prefer-egap").defaultValue(true).build());

    private boolean isEating = false;
    private int previousSlot = -1;

    public UltraGapple() {
        super(HeliumAddon.CATEGORY, "Ultra Gapple", "Advanced auto gapple.");
    }

    @Override
    public void onDeactivate() {
        stopEating();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem()) {
            Item activeItem = mc.player.getActiveItem().getItem();
            if (activeItem != Items.ENCHANTED_GOLDEN_APPLE && activeItem != Items.GOLDEN_APPLE) {
                return;
            }
        }

        boolean shouldEat = false;
        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (currentHealth <= health.get()) {
            if (!weaponOnly.get() || isHoldingWeapon()) {
                shouldEat = true;
            }
        }

        if (rightClick.get() && isHoldingWeapon() && mc.options.useKey.isPressed()) {
            shouldEat = true;
        }

        if (shouldEat) {
            eat();
        } else {
            stopEating();
        }
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        String itemName = mc.player.getMainHandStack().getItem().toString().toLowerCase();
        return itemName.contains("sword") || itemName.contains("axe") || itemName.contains("mace") || itemName.contains("trident");
    }

    private void eat() {
        Item preferred = egapple.get() ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
        Item backup = egapple.get() ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE;
        Item offhandItem = mc.player.getOffHandStack().getItem();

        boolean hasInOffhand = (offhandItem == preferred) || (offhandItem == backup);

        if (!isEating) {
            if (!hasInOffhand) {
                previousSlot = ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getSelectedSlot();
            }
            isEating = true;
        }

        if (!hasInOffhand) {
            FindItemResult gapple = InvUtils.findInHotbar(preferred);
            if (!gapple.found()) gapple = InvUtils.findInHotbar(backup);

            if (!gapple.found()) {
                stopEating();
                return;
            }

            if (((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getSelectedSlot() != gapple.slot()) {
                ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(gapple.slot());
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(gapple.slot()));
            }
        }

        mc.options.useKey.setPressed(true);
        if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, hasInOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        }
    }

    private void stopEating() {
        if (!isEating) return;

        mc.options.useKey.setPressed(false);
        isEating = false;

        if (previousSlot != -1 && previousSlot < 9) {
            ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(previousSlot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            previousSlot = -1;
        }
    }
}