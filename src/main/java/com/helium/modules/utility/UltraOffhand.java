package com.helium.modules.utility;

import com.helium.HeliumAddon;
import com.helium.mixin.accessor.PlayerInventoryAccessor;
import com.helium.modules.combat.HeliumCrystalAura;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.Field;

public class UltraOffhand extends Module {

    public enum OffhandItem {
        Totem,
        Crystal,
        Gapple,
        Shield
    }

    public enum SwitchMode {
        Silent,
        Normal,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgProtection = settings.createGroup("Protection");

    private final Setting<OffhandItem> defaultItem = sgGeneral.add(new EnumSetting.Builder<OffhandItem>().name("default-item").defaultValue(OffhandItem.Crystal).build());
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>().name("switch-mode").defaultValue(SwitchMode.Silent).build());
    private final Setting<Boolean> instantReaction = sgProtection.add(new BoolSetting.Builder().name("instant-reaction").defaultValue(true).build());
    private final Setting<Boolean> predictCrystals = sgProtection.add(new BoolSetting.Builder().name("predict-crystals").defaultValue(true).build());
    private final Setting<Boolean> predictTnt = sgProtection.add(new BoolSetting.Builder().name("predict-tnt").defaultValue(true).build());
    private final Setting<Boolean> predictAnchors = sgProtection.add(new BoolSetting.Builder().name("predict-anchors").defaultValue(true).build());
    private final Setting<Boolean> heliumSync = sgProtection.add(new BoolSetting.Builder().name("helium-sync").defaultValue(true).build());
    private final Setting<Boolean> fallProtection = sgProtection.add(new BoolSetting.Builder().name("fall-protection").defaultValue(true).build());
    private final Setting<Double> lethalMultiplier = sgProtection.add(new DoubleSetting.Builder().name("lethal-multiplier").defaultValue(1.0).min(0.0).max(2.0).build());
    private final Setting<Boolean> armorProtection = sgProtection.add(new BoolSetting.Builder().name("armor-protection").defaultValue(true).build());
    private final Setting<Integer> armorThreshold = sgProtection.add(new IntSetting.Builder().name("armor-threshold").defaultValue(10).min(1).max(100).visible(armorProtection::get).build());

    private int delayTimer = 0;
    private Item currentServerOffhand = null;
    private boolean isFaking = false;

    public UltraOffhand() {
        super(HeliumAddon.CATEGORY, "Ultra Offhand", "Advanced offhand manager that protects you from absolute everything.");
    }

    @Override
    public void onActivate() {
        delayTimer = 0;
        currentServerOffhand = null;
        isFaking = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        boolean forceTotem = false;
        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (currentHealth <= 1.0) {
            forceTotem = true;
        }

        if (!forceTotem && fallProtection.get()) {
            double fallDistance = mc.player.fallDistance;
            if (fallDistance > 3.0) {
                double fallDamage = Math.max(0.0, fallDistance - 3.0);
                if (currentHealth - fallDamage <= 0.5) {
                    forceTotem = true;
                }
            }
        }

        if (!forceTotem && armorProtection.get()) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : armorSlots) {
                ItemStack armor = mc.player.getEquippedStack(slot);
                if (!armor.isEmpty() && armor.isDamageable()) {
                    if (((armor.getMaxDamage() - armor.getDamage()) * 100 / armor.getMaxDamage()) <= armorThreshold.get()) {
                        forceTotem = true;
                        break;
                    }
                }
            }
        }

        if (!forceTotem && predictAnchors.get()) {
            BlockPos playerPos = mc.player.getBlockPos();
            anchorLoop:
            for (int x = -10; x <= 10; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -10; z <= 10; z++) {
                        if (mc.world.getBlockState(playerPos.add(x, y, z)).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            forceTotem = true;
                            break anchorLoop;
                        }
                    }
                }
            }
        }

        if (!forceTotem && (predictCrystals.get() || predictTnt.get() || heliumSync.get())) {
            double totalIncomingDamage = 0.0;

            if (heliumSync.get()) {
                try {
                    Field field = HeliumCrystalAura.class.getDeclaredField("incomingDamage");
                    field.setAccessible(true);
                    totalIncomingDamage += field.getDouble(null);
                } catch (Exception ignored) {
                }
            }

            if (predictCrystals.get()) {
                for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(12.0), entity -> true)) {
                    totalIncomingDamage += DamageUtils.crystalDamage(mc.player, new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()));
                }
            }

            if (predictTnt.get()) {
                for (TntEntity tnt : mc.world.getEntitiesByClass(TntEntity.class, mc.player.getBoundingBox().expand(12.0), entity -> true)) {
                    totalIncomingDamage += DamageUtils.crystalDamage(mc.player, new Vec3d(tnt.getX(), tnt.getY(), tnt.getZ()));
                }
            }

            if (totalIncomingDamage > 0.0 && (currentHealth - (totalIncomingDamage * lethalMultiplier.get())) <= 0.5) {
                forceTotem = true;
            }
        }

        Item targetItem;
        if (forceTotem) {
            targetItem = Items.TOTEM_OF_UNDYING;
        } else {
            targetItem = switch (defaultItem.get()) {
                case Totem -> Items.TOTEM_OF_UNDYING;
                case Crystal -> Items.END_CRYSTAL;
                case Gapple -> Items.ENCHANTED_GOLDEN_APPLE;
                case Shield -> Items.SHIELD;
            };

            if (targetItem == Items.ENCHANTED_GOLDEN_APPLE && mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))) {
                targetItem = Items.TOTEM_OF_UNDYING;
            }
        }

        Item actualOffhand = mc.player.getOffHandStack().getItem();
        if (switchMode.get() == SwitchMode.Silent && isFaking && currentServerOffhand != null) {
            actualOffhand = currentServerOffhand;
        }

        if (actualOffhand != targetItem) {
            FindItemResult result = InvUtils.find(targetItem);
            if (result.found()) {
                doSwap(result, targetItem);
            } else if (!forceTotem) {
                FindItemResult totemFallback = InvUtils.find(Items.TOTEM_OF_UNDYING);
                if (totemFallback.found() && actualOffhand != Items.TOTEM_OF_UNDYING) {
                    doSwap(totemFallback, Items.TOTEM_OF_UNDYING);
                }
            }
        }
    }

    public void handleInstantExplosion(ExplosionS2CPacket packet) {
        if (!instantReaction.get() || mc.player == null || mc.world == null) return;

        double x = packet.center().getX();
        double y = packet.center().getY();
        double z = packet.center().getZ();

        Vec3d explosionPos = new Vec3d(x, y, z);
        double damage = DamageUtils.crystalDamage(mc.player, explosionPos);
        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (damage > 0.0 && (currentHealth - (damage * lethalMultiplier.get())) <= 0.5) {
            forceEquipTotemInstantly();
        }
    }

    private void forceEquipTotemInstantly() {
        Item actualOffhand = mc.player.getOffHandStack().getItem();
        if (switchMode.get() == SwitchMode.Silent && isFaking && currentServerOffhand != null) {
            actualOffhand = currentServerOffhand;
        }

        if (actualOffhand == Items.TOTEM_OF_UNDYING) return;

        FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (result.found()) {
            doSwap(result, Items.TOTEM_OF_UNDYING);
        }
    }

    private void doSwap(FindItemResult result, Item itemToTrack) {
        boolean inScreen = mc.currentScreen != null;

        if (result.isHotbar() && switchMode.get() != SwitchMode.None && !inScreen) {
            int slot = result.slot() >= 36 ? result.slot() - 36 : result.slot();
            int currentSlot = ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getSelectedSlot();

            if (switchMode.get() == SwitchMode.Silent) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
                currentServerOffhand = itemToTrack;
                isFaking = true;
            } else if (switchMode.get() == SwitchMode.Normal) {
                ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).setSelectedSlot(slot);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                isFaking = false;
            }
        } else {
            InvUtils.move().from(result.slot()).toOffhand();
            isFaking = false;
        }
        delayTimer = 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.packet instanceof ExplosionS2CPacket packet) {
            handleInstantExplosion(packet);
        }

        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            if (predictAnchors.get() && packet.getState().getBlock() == Blocks.RESPAWN_ANCHOR) {
                BlockPos pos = packet.getPos();
                double dx = mc.player.getX() - (pos.getX() + 0.5);
                double dy = mc.player.getY() - (pos.getY() + 0.5);
                double dz = mc.player.getZ() - (pos.getZ() + 0.5);

                if ((dx * dx + dy * dy + dz * dz) <= 100.0) {
                    forceEquipTotemInstantly();
                }
            }
        }

        if (switchMode.get() == SwitchMode.Silent && isFaking) {
            if (event.packet instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
                if (packet.getSyncId() == 0 && packet.getSlot() == 45) {
                    event.cancel();
                }
            }

            if (event.packet instanceof EntityStatusS2CPacket packet) {
                if (packet.getEntity(mc.world) == mc.player && packet.getStatus() == 35) {
                    currentServerOffhand = Items.AIR;
                }
            }
        }
    }
}