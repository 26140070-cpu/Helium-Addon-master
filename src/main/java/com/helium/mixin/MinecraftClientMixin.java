package com.helium.mixin;

import com.helium.gui.WelcomeWindow;
import com.helium.util.HeliumBackgroundConfig;
import com.helium.util.HeliumScreenBackground;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!HeliumBackgroundConfig.enabled) return;
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (screen instanceof WidgetScreen) {
            return;
        }
        if (screen == null) {
            if (client.world != null) {
                HeliumScreenBackground.stopAudio();
            }
        } else if (!HeliumBackgroundConfig.audioFileName.isEmpty() && client.world == null) {
            HeliumScreenBackground.ensureAudio();
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onMinecraftStop(CallbackInfo ci) {
        WelcomeWindow.playExitSound();
    }
}