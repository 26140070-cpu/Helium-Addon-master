package com.helium.mixin;

import com.helium.modules.combat.BasicAnchorAura;
import com.helium.modules.combat.HeliumCrystalAura;
import com.helium.modules.utility.UltraOffhand;
import com.helium.modules.render.ImpactFramesModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    private void onEntitySpawnInstaPop(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if (packet.getEntityType() == EntityType.END_CRYSTAL) {
            HeliumCrystalAura.lastEntityId = packet.getEntityId();
        }
    }

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void onExplosionInstant(ExplosionS2CPacket packet, CallbackInfo ci) {
        UltraOffhand ultraOffhand = Modules.get().get(UltraOffhand.class);
        if (ultraOffhand != null && ultraOffhand.isActive()) {
            ultraOffhand.handleInstantExplosion(packet);
        }

        BasicAnchorAura anchorAura = Modules.get().get(BasicAnchorAura.class);
        if (anchorAura != null && anchorAura.isActive()) {
        }
    }

    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    private void onEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ImpactFramesModule impactFrames = Modules.get().get(ImpactFramesModule.class);
        if (impactFrames == null || !impactFrames.isActive()) return;

        int attackerId = packet.sourceDirectId();
        int targetId = packet.entityId();

        if (attackerId == client.player.getId()) {
            Entity targetEntity = client.world.getEntityById(targetId);
            if (targetEntity instanceof LivingEntity livingTarget) {
                impactFrames.markAsAttacked(livingTarget);
                impactFrames.triggerHit();
            }
        }
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        ImpactFramesModule impactFrames = Modules.get().get(ImpactFramesModule.class);
        if (impactFrames == null || !impactFrames.isActive()) return;

        int status = packet.getStatus();
        Entity entity = packet.getEntity(client.world);

        if (impactFrames.debugMode.get()) {
            impactFrames.info("EntityStatus received: status=" + status + ", entity=" + (entity != null ? entity.getName().getString() : "null"));
        }

        if (status == 35 && entity instanceof PlayerEntity player && entity != client.player) {
            impactFrames.triggerTotemPop();
        } else if (status == 3 && entity instanceof LivingEntity livingEntity && entity != client.player) {
            if (impactFrames.wasAttackedByUs(livingEntity)) {
                impactFrames.triggerKill();
            }
        }
    }
}