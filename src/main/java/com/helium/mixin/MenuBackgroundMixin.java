package com.helium.mixin;

import com.helium.util.HeliumBackgroundConfig;
import com.helium.util.HeliumScreenBackground;
import com.helium.util.PanoramaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MenuBackgroundMixin {

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) return;

        Screen self = (Screen) (Object) this;
        if (self instanceof TitleScreen || self instanceof SelectWorldScreen || self instanceof MultiplayerScreen) {
            if (PanoramaConfig.enabled) {
                HeliumScreenBackground.stopAudio();
                return;
            }

            if (HeliumBackgroundConfig.enabled && !HeliumBackgroundConfig.imageFileName.isEmpty()) {
                int width = client.getWindow().getScaledWidth();
                int height = client.getWindow().getScaledHeight();
                context.fill(0, 0, width, height, 0xFF000000);
                HeliumScreenBackground.render(context, width, height);
                ci.cancel();
            }
        }
    }
}