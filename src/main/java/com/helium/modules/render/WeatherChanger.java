package com.helium.modules.render;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class WeatherChanger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Time mode")
            .defaultValue(Mode.Clear)
            .build()
    );

    private final Setting<Boolean> disableRain = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-rain")
            .description("Disables rain rendering")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> fixedTime = sgGeneral.add(new IntSetting.Builder()
            .name("fixed-time")
            .description("Fixed time of day (0-24000)")
            .defaultValue(6000)
            .min(0)
            .max(24000)
            .sliderMin(0)
            .sliderMax(24000)
            .visible(() -> mode.get() == Mode.Fixed)
            .build()
    );

    private final Setting<Double> timeSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("time-speed")
            .description("Time speed multiplier")
            .defaultValue(1.0)
            .min(0.1)
            .max(10.0)
            .sliderMin(0.1)
            .sliderMax(10.0)
            .visible(() -> mode.get() == Mode.Custom)
            .build()
    );

    public WeatherChanger() {
        super(HeliumAddon.CATEGORY, "weather-changer", "Changes weather and time client-side");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null) return;

        if (disableRain.get()) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }

        long newTime;
        switch (mode.get()) {
            case Clear -> newTime = 6000;
            case Day -> newTime = 1000;
            case Night -> newTime = 13000;
            case Midnight -> newTime = 18000;
            case Fixed -> newTime = fixedTime.get();
            case Custom -> {
                long current = mc.world.getTimeOfDay();
                newTime = current + (long)(timeSpeed.get() * 20);
            }
            default -> newTime = mc.world.getTimeOfDay();
        }

        mc.world.getLevelProperties().setTimeOfDay(newTime);
    }

    public enum Mode {
        Clear,
        Day,
        Night,
        Midnight,
        Fixed,
        Custom
    }
}
