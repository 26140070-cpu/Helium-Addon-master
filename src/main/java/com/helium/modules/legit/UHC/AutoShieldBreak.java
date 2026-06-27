package com.helium.modules.legit.UHC;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.registry.tag.ItemTags;

public class AutoShieldBreak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switch to axe when target is blocking")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> switchBackDelay = sgGeneral.add(new IntSetting.Builder()
            .name("switch-back-delay")
            .description("Ticks before switching back to sword after breaking shield")
            .defaultValue(2)
            .min(0)
            .max(10)
            .sliderMax(10)
            .visible(autoSwitch::get)
            .build()
    );

    private int switchBackTicks = 0;
    private int originalSlot = -1;
    private boolean axeEquipped = false;

    public AutoShieldBreak() {
        super(HeliumAddon.LEGIT_UHC, "auto-shield-break", "Automatically breaks enemy shields with an axe and switches back to your sword.");
    }

    @Override
    public void onDeactivate() {
        switchBackTicks = 0;
        if (originalSlot != -1 && mc.player != null) {
            InvUtils.swap(originalSlot, false);
            originalSlot = -1;
        }
        axeEquipped = false;
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null) return;
        if (!(event.entity instanceof PlayerEntity target) || !target.isAlive()) return;

        if (target.isBlocking() && autoSwitch.get() && !isHoldingAxe()) {
            FindItemResult axe = findAxe();
            if (axe.found()) {
                originalSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
                InvUtils.swap(axe.slot(), false);
                axeEquipped = true;
                switchBackTicks = 0;
            }
        }

        if (axeEquipped && !target.isBlocking()) {
            switchBackTicks = switchBackDelay.get();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (switchBackTicks > 0) {
            switchBackTicks--;
            if (switchBackTicks == 0 && originalSlot != -1 && axeEquipped) {
                InvUtils.swap(originalSlot, false);
                originalSlot = -1;
                axeEquipped = false;
            }
        }
    }

    private boolean isHoldingAxe() {
        return mc.player.getMainHandStack().getItem() instanceof AxeItem;
    }

    private FindItemResult findAxe() {
        return InvUtils.findInHotbar(item -> item.getItem() instanceof AxeItem);
    }

    private FindItemResult findSword() {
        return InvUtils.findInHotbar(item -> item.isIn(ItemTags.SWORDS));
    }
}