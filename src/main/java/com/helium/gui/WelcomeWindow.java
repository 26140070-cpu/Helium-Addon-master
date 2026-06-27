package com.helium.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class WelcomeWindow {

    public static void showWindow() {
        String ascii =
                "                                                                                                    \n" +
                        "                                                                                                    \n" +
                        "HHHHHHHHH     HHHHHHHHH                   lllllll   iiii                                            \n" +
                        "H:::::::H     H:::::::H                   l:::::l  i::::i                                           \n" +
                        "H:::::::H     H:::::::H                   l:::::l   iiii                                            \n" +
                        "HH::::::H     H::::::HH                   l:::::l                                                   \n" +
                        "  H:::::H     H:::::H      eeeeeeeeeeee    l::::l iiiiiii uuuuuu    uuuuuu     mmmmmmm    mmmmmmm   \n" +
                        "  H:::::H     H:::::H    ee::::::::::::ee  l::::l i:::::i u::::u    u::::u   mm:::::::m  m:::::::mm \n" +
                        "  H::::::HHHHH::::::H   e::::::eeeee:::::eel::::l  i::::i u::::u    u::::u  m::::::::::mm::::::::::m\n" +
                        "  H:::::::::::::::::H  e::::::e     e:::::el::::l  i::::i u::::u    u::::u  m::::::::::::::::::::::m\n" +
                        "  H:::::::::::::::::H  e:::::::eeeee::::::el::::l  i::::i u::::u    u::::u  m:::::mmm::::::mmm:::::m\n" +
                        "  H::::::HHHHH::::::H  e:::::::::::::::::e l::::l  i::::i u::::u    u::::u  m::::m   m::::m   m::::m\n" +
                        "  H:::::H     H:::::H  e::::::eeeeeeeeeee  l::::l  i::::i u::::u    u::::u  m::::m   m::::m   m::::m\n" +
                        "  H:::::H     H:::::H  e:::::::e           l::::l  i::::i u:::::uuuu:::::u  m::::m   m::::m   m::::m\n" +
                        "HH::::::H     H::::::HHe::::::::e          l::::::li::::::iu:::::::::::::u  m::::m   m::::m   m::::m\n" +
                        "H:::::::H     H:::::::H e::::::::eeeeeeee  l::::::li::::::i u::::::::::::u  m::::m   m::::m   m::::m\n" +
                        "H:::::::H     H:::::::H  ee:::::::::::::e  l::::::li::::::i  uu::::::::u    m::::m   m::::m   m::::m\n" +
                        "HHHHHHHHH     HHHHHHHHH    eeeeeeeeeeeeee  lllllllliiiiiiii    uuuuuuuu     mmmmmm   mmmmmm   mmmmmm";

        System.out.println(ascii);
        playBlockingSound("/assets/helium/sounds/start.wav");

        new Thread(() -> {
            while (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().getToastManager() == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }

            String username = MinecraftClient.getInstance().getSession().getUsername();

            MinecraftClient.getInstance().getToastManager().add(
                    SystemToast.create(
                            MinecraftClient.getInstance(),
                            SystemToast.Type.PERIODIC_NOTIFICATION,
                            Text.literal("Helium Addon"),
                            Text.literal("Welcome back " + username + "!")
                    )
            );
        }).start();
    }

    public static void closeWindow() {
    }

    public static void playExitSound() {
        playBlockingSound("/assets/helium/sounds/end.wav");
    }

    private static void playBlockingSound(String resourcePath) {
        try (InputStream is = WelcomeWindow.class.getResourceAsStream(resourcePath)) {
            if (is == null) return;
            InputStream bufferedIn = new BufferedInputStream(is);
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(bufferedIn)) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000);
            }
        } catch (Exception ignored) {}
    }
}