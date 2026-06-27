package com.helium.modules.utility;

import com.helium.HeliumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

import javax.sound.sampled.*;
import java.io.File;
import java.util.Random;

public class DeathSounds extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> deathSoundEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("death-sound")
            .description("Plays a sound upon death.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> deathSoundFile = sgGeneral.add(new StringSetting.Builder()
            .name("death-file")
            .description("The name of the .wav file for death.")
            .defaultValue("death.wav")
            .visible(deathSoundEnabled::get)
            .build()
    );

    private final Setting<Boolean> respawnSoundEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("respawn-sound")
            .description("Plays a sound upon respawning.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> respawnSoundFile = sgGeneral.add(new StringSetting.Builder()
            .name("respawn-file")
            .description("The name of the .wav file for respawning.")
            .defaultValue("respawn.wav")
            .visible(respawnSoundEnabled::get)
            .build()
    );

    private final Setting<Boolean> randomSound = sgGeneral.add(new BoolSetting.Builder()
            .name("random-sound")
            .description("Chooses a random .wav from the folder instead of a fixed one.")
            .defaultValue(false)
            .visible(() -> deathSoundEnabled.get() || respawnSoundEnabled.get())
            .build()
    );

    private final Setting<Double> volume = sgGeneral.add(new DoubleSetting.Builder()
            .name("volume")
            .description("Volume of the sounds.")
            .defaultValue(0.8)
            .min(0.0)
            .max(1.0)
            .build()
    );

    private static final File SOUNDS_DIR = new File(
            MinecraftClient.getInstance().runDirectory, "meteor-client/helium-sounds"
    );
    private final Random random = new Random();
    private boolean wasDead = false;

    private Clip currentClip;

    public DeathSounds() {
        super(HeliumAddon.CATEGORY, "death-sounds", "Plays sounds upon death and respawn.");
        if (!SOUNDS_DIR.exists()) {
            SOUNDS_DIR.mkdirs();
        }
    }

    @Override
    public void onActivate() {
        wasDead = false;
        if (!SOUNDS_DIR.exists()) {
            boolean created = SOUNDS_DIR.mkdirs();
            if (created) {
                info("Folder created: " + SOUNDS_DIR.getAbsolutePath());
            } else {
                error("Could not create the folder. Check your permissions.");
            }
        }
    }

    @Override
    public void onDeactivate() {
        stopCurrentSound();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        float health = mc.player.getHealth();
        boolean isDead = health <= 0.0f;

        if (!wasDead && isDead) {
            if (deathSoundEnabled.get()) playSound(deathSoundFile.get());
            wasDead = true;
        }

        if (wasDead && !isDead) {
            if (respawnSoundEnabled.get()) playSound(respawnSoundFile.get());
            wasDead = false;
        }
    }

    private synchronized void playSound(String defaultFile) {
        stopCurrentSound();

        File file;
        if (randomSound.get()) {
            File[] files = SOUNDS_DIR.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
            if (files == null || files.length == 0) return;
            file = files[random.nextInt(files.length)];
        } else {
            file = new File(SOUNDS_DIR, defaultFile);
            if (!file.exists()) return;
        }

        new Thread(() -> {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float vol = volume.get().floatValue();
                    float dB = (float) (Math.log(vol <= 0 ? 0.0001 : vol) / Math.log(10.0) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
                synchronized (DeathSounds.this) {
                    if (currentClip != clip) {
                        currentClip = clip;
                    } else {
                        clip.close();
                        return;
                    }
                }
                clip.start();
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        synchronized (DeathSounds.this) {
                            if (currentClip == clip) currentClip = null;
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private synchronized void stopCurrentSound() {
        if (currentClip != null) {
            if (currentClip.isRunning()) currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
    }
}