package com.helium.modules.utility;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

public class HeliumRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> customText = sgGeneral.add(new StringSetting.Builder()
            .name("custom-text")
            .description("Custom RPC text.")
            .defaultValue("Join NOW to dominate the anarchics servers: https://discord.gg/rGABBNJz8s")
            .build()
    );

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("indicator-threshold")
            .description("Health threshold to show the danger icon.")
            .defaultValue(10.0)
            .min(1.0)
            .sliderMax(36.0)
            .build()
    );

    private static final long APP_ID = 1491241028181561425L;
    private final RichPresence rpc = new RichPresence();
    private int ticks = 0;
    private boolean rpcStarted = false;

    private final String DANGER_KEY = "danger_icon";
    private final String FINE_KEY = "fine_icon";
    private final String LARGE_IMAGE_KEY = "helium_icon";

    public HeliumRPC() {
        super(HeliumAddon.CATEGORY, "Helium RPC", "Displays Rich Presence for Helium Addon.");
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(APP_ID, null);
        rpc.setStart(System.currentTimeMillis() / 1000L);
        rpc.setLargeImage(LARGE_IMAGE_KEY, "Helium Addon");
        rpcStarted = true;
        ticks = 0;
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
        rpcStarted = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!rpcStarted) return;

        ticks++;
        if (ticks >= 20) {
            updateRPC();
            ticks = 0;
        }
    }

    private void updateRPC() {
        rpc.setDetails(customText.get());

        if (mc.player != null) {
            double health = PlayerUtils.getTotalHealth();
            String iconKey = health <= healthThreshold.get() ? DANGER_KEY : FINE_KEY;
            String state = String.format("Health: %.1f", health);

            rpc.setState(state);
            rpc.setSmallImage(iconKey, state);
        } else {
            rpc.setState("In Main Menu");
            rpc.setSmallImage(FINE_KEY, "Main Menu");
        }

        DiscordIPC.setActivity(rpc);
    }
}