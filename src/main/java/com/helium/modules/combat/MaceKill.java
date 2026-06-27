package com.helium.modules.combat;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class MaceKill extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgTotem = settings.createGroup("Totem Bypass (Paper Only)");

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-arm")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> packetDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-when-blocked")
            .description("Prevents death by not sending movement packets if the attack was blocked.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
            .name("fall-height")
            .description("Simulated fall distance in blocks.")
            .defaultValue(22)
            .sliderRange(1, 169)
            .min(1)
            .max(169)
            .build()
    );

    private final Setting<Integer> paperPackets = sgGeneral.add(new IntSetting.Builder()
            .name("packet-spam")
            .description("Packets to send (4 for vanilla, up to 17 for Paper).")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 17)
            .build()
    );

    private final Setting<Boolean> useOffset = sgGeneral.add(new BoolSetting.Builder()
            .name("use-offset")
            .description("Helps prevent fall damage on packet loss.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> offsetHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-offset")
            .defaultValue(0.05)
            .min(0.0)
            .sliderMax(0.99)
            .visible(useOffset::get)
            .build()
    );

    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
            .name("y-offset")
            .defaultValue(0.01)
            .min(0.0)
            .sliderMax(0.99)
            .visible(useOffset::get)
            .build()
    );

    private final Setting<Boolean> totemBypass = sgTotem.add(new BoolSetting.Builder()
            .name("totem-bypass")
            .description("Sends multiple attacks to bypass totems (Paper only).")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> totemAttacks = sgTotem.add(new IntSetting.Builder()
            .name("totem-attacks")
            .defaultValue(3)
            .sliderRange(1, 3)
            .min(1)
            .visible(totemBypass::get)
            .build()
    );

    private final Setting<Integer> totemHeightIncrease = sgTotem.add(new IntSetting.Builder()
            .name("totem-height-increase")
            .description("Blocks to add per attack to beat invulnerability frames.")
            .defaultValue(9)
            .sliderRange(1, 100)
            .min(1)
            .visible(totemBypass::get)
            .build()
    );

    private Vec3d previousPos;
    private boolean sendingAttacks = false;
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
    private final Map<Vec3d, Boolean> positionCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Vec3d, Boolean> eldest) {
            return size() > 256;
        }
    };

    public MaceKill() {
        super(HeliumAddon.CATEGORY, "MaceKill", "Makes the Mace deal massive damage via simulated fall height.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingAttacks || mc.player == null) return;
        if (mc.player.hasVehicle() || mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (!(packet.meteor$getEntity() instanceof LivingEntity target)) return;

        if (packetDisable.get() && (target.isBlocking() || target.isInvulnerable() || target.isInCreativeMode())) return;
        if (!target.isAlive()) return;

        int baseBlocks = getMaxHeightAbovePlayer();
        if (baseBlocks == 0) {
            error("No valid space above you for MaceKill.");
            return;
        }

        event.cancel();
        previousPos = mc.player.getEntityPos();
        int currentHeight = baseBlocks;
        int attackCount = totemBypass.get() ? totemAttacks.get() : 1;

        for (int i = 0; i < paperPackets.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision
            ));
        }

        try {
            boolean targetPosValid = true;
            for (int i = 0; i < attackCount; i++) {
                int blocks = (i == 0) ? baseBlocks : currentHeight;
                if (mc.world == null || mc.player.getY() + blocks > mc.world.getTopYInclusive() - 1) {
                    targetPosValid = false;
                    continue;
                }

                Vec3d targetPos = new Vec3d(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
                sendMove(targetPos);
                sendMove(previousPos);
                mc.player.setPosition(previousPos);

                sendingAttacks = true;
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                currentHeight += totemHeightIncrease.get();
            }

            positionCache.clear();
            if (targetPosValid && useOffset.get()) {
                Vec3d offsetHome = getOffset(previousPos);
                sendMove(offsetHome);
                mc.player.setPosition(offsetHome);
            }
        } finally {
            sendingAttacks = false;
        }
    }

    private void sendMove(Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        PlayerMoveC2SPacket movePacket = new PlayerMoveC2SPacket.Full(
                pos.getX(), pos.getY(), pos.getZ(), mc.player.getYaw(), mc.player.getPitch(),
                false, mc.player.horizontalCollision
        );
        ((IPlayerMoveC2SPacket) movePacket).meteor$setTag(1337);
        mc.player.networkHandler.sendPacket(movePacket);
    }

    private Vec3d getOffset(Vec3d base) {
        double dx = offsetHorizontal.get();
        double dy = offsetY.get();
        Vec3d[] offsets = {
                base.add( dx, dy,  0), base.add(-dx, dy,  0),
                base.add( 0, dy,  dx), base.add( 0, dy, -dx),
                base.add( dx, dy,  dx), base.add(-dx, dy, -dx),
                base.add(-dx, dy,  dx), base.add( dx, dy, -dx)
        };
        Collections.shuffle(Arrays.asList(offsets));
        for (Vec3d pos : offsets) if (!invalid(pos)) return pos;
        Vec3d noHorizontal = base.add(0, dy, 0);
        return !invalid(noHorizontal) ? noHorizontal : base;
    }

    private boolean invalid(Vec3d pos) {
        if (mc.world == null) return true;
        double clampedY = MathHelper.clamp(pos.y, mc.world.getBottomY(), mc.world.getTopYInclusive() - 1);
        if (clampedY != pos.y) return true;
        BlockPos floored = BlockPos.ofFloored(pos);
        if (mc.world.getChunkManager().getWorldChunk(floored.getX() >> 4, floored.getZ() >> 4) == null) return true;
        if (positionCache.containsKey(pos)) return positionCache.get(pos);

        Entity entity = mc.player;
        Vec3d delta = pos.subtract(entity.getEntityPos());
        Box box = entity.getBoundingBox().offset(delta);
        mutablePos.set(floored);
        for (int x = -1; x <= 1; x++) {
            mutablePos.setX(floored.getX() + x);
            for (int y = -1; y <= 1; y++) {
                mutablePos.setY(floored.getY() + y);
                for (int z = -1; z <= 1; z++) {
                    mutablePos.setZ(floored.getZ() + z);
                    BlockState state = mc.world.getBlockState(mutablePos);
                    if (state.isOf(Blocks.LAVA) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)
                            || state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.CAMPFIRE)
                            || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.POWDER_SNOW)) {
                        positionCache.put(pos, true);
                        return true;
                    }
                }
            }
        }
        for (Entity e : mc.world.getOtherEntities(entity, box)) {
            if (e.isCollidable(entity)) { positionCache.put(pos, true); return true; }
        }
        boolean collides = mc.world.getBlockCollisions(entity, box).iterator().hasNext();
        positionCache.put(pos, collides);
        return collides;
    }

    private int getMaxHeightAbovePlayer() {
        if (mc.world == null) return 0;
        int worldTop = mc.world.getTopYInclusive() - 1;
        int maxBlocks = (int)(worldTop - mc.player.getY());
        return Math.min(fallHeight.get(), maxBlocks);
    }
}