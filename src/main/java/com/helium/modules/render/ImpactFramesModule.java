package com.helium.modules.render;

import com.helium.HeliumAddon;
import com.helium.util.render.ImpactFramesHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ImpactFramesModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKill = settings.createGroup("Kill Effect");

    public final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
            .name("duration")
            .defaultValue(200)
            .min(50)
            .sliderMax(1000)
            .build()
    );

    public final Setting<Integer> startScalePct = sgGeneral.add(new IntSetting.Builder()
            .name("start-scale-pct")
            .defaultValue(50)
            .min(1)
            .max(100)
            .build()
    );

    public final Setting<Integer> endScalePct = sgGeneral.add(new IntSetting.Builder()
            .name("end-scale-pct")
            .defaultValue(100)
            .min(1)
            .max(300)
            .build()
    );

    public final Setting<String> hitFolder = sgGeneral.add(new StringSetting.Builder()
            .name("hit-folder")
            .defaultValue("hit")
            .build()
    );

    public final Setting<Integer> hitColor = sgGeneral.add(new IntSetting.Builder()
            .name("hit-color")
            .defaultValue(0x80FF0000)
            .min(0)
            .max(0xFFFFFFFF)
            .build()
    );

    public final Setting<String> totemFolder = sgGeneral.add(new StringSetting.Builder()
            .name("totem-folder")
            .defaultValue("totem_pop")
            .build()
    );

    public final Setting<Integer> totemColor = sgGeneral.add(new IntSetting.Builder()
            .name("totem-color")
            .defaultValue(0x80AAFF00)
            .min(0)
            .max(0xFFFFFFFF)
            .build()
    );

    public final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
            .name("debug-mode")
            .description("Show debug messages for impact frame triggers.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> killEnabled = sgKill.add(new BoolSetting.Builder()
            .name("kill-enabled")
            .defaultValue(true)
            .build()
    );

    public final Setting<String> killFolder = sgKill.add(new StringSetting.Builder()
            .name("kill-folder")
            .defaultValue("kill")
            .visible(killEnabled::get)
            .build()
    );

    public final Setting<Integer> killColor = sgKill.add(new IntSetting.Builder()
            .name("kill-color")
            .defaultValue(0xAAFFA500)
            .min(0)
            .max(0xFFFFFFFF)
            .visible(killEnabled::get)
            .build()
    );

    public final Setting<Boolean> killFlash = sgKill.add(new BoolSetting.Builder()
            .name("kill-flash")
            .defaultValue(true)
            .visible(killEnabled::get)
            .build()
    );

    private final Map<UUID, Long> recentlyAttackedEntities = new ConcurrentHashMap<>();

    public ImpactFramesModule() {
        super(HeliumAddon.CATEGORY, "Impact Frames", "Animated impact frames on hit, kill, and totem pop.");
    }

    @Override
    public void onActivate() {
        recentlyAttackedEntities.clear();
        ImpactFramesHandler.preloadTextures();
        if (debugMode.get()) {
            info("Impact Frames activated. Debug mode ON.");
        }
    }

    @Override
    public void onDeactivate() {
        recentlyAttackedEntities.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ImpactFramesHandler.update();

        long now = System.currentTimeMillis();
        recentlyAttackedEntities.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
    }

    public void markAsAttacked(LivingEntity target) {
        if (target != null) {
            recentlyAttackedEntities.put(target.getUuid(), System.currentTimeMillis());
            if (debugMode.get()) {
                info("Marked " + target.getName().getString() + " as attacked. Total tracked: " + recentlyAttackedEntities.size());
            }
        }
    }

    public boolean wasAttackedByUs(LivingEntity target) {
        if (target == null) return false;
        Long timestamp = recentlyAttackedEntities.get(target.getUuid());
        boolean result = timestamp != null && (System.currentTimeMillis() - timestamp) < 10000;
        if (debugMode.get()) {
            info("Checking if " + target.getName().getString() + " was attacked by us: " + result
                    + " (timestamp: " + (timestamp != null ? System.currentTimeMillis() - timestamp + "ms ago" : "null") + ")");
        }
        return result;
    }

    public void triggerHit() {
        if (debugMode.get()) info("Triggering HIT effect");
        ImpactFramesHandler.addImpactEffect(
                hitFolder.get(), hitColor.get(),
                startScalePct.get() / 100.0f, endScalePct.get() / 100.0f,
                duration.get(), false
        );
    }

    public void triggerKill() {
        if (killEnabled.get()) {
            if (debugMode.get()) info("Triggering KILL effect");

            ImpactFramesHandler.addImpactEffect(
                    killFolder.get(), killColor.get(),
                    0.0f, 1.0f,
                    duration.get(),
                    true  
            );

            if (killFlash.get()) {
                ImpactFramesHandler.addImpactEffect(
                        "", 
                        killColor.get(),
                        0.0f, 1.0f,
                        duration.get(),
                        true
                );
            }
        }
    }

    public void triggerTotemPop() {
        if (debugMode.get()) info("Triggering TOTEM POP effect");
        ImpactFramesHandler.addImpactEffect(
                totemFolder.get(), totemColor.get(),
                startScalePct.get() / 100.0f, endScalePct.get() / 100.0f,
                duration.get(), false
        );
    }
}