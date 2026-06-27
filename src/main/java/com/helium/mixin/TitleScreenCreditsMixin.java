package com.helium.mixin;

import com.helium.util.HeliumBackgroundConfig;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "meteordevelopment.meteorclient.utils.player.TitleScreenCredits")
public class TitleScreenCreditsMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onRender(DrawContext context, CallbackInfo ci) {
        if (HeliumBackgroundConfig.cleanerEnabled) {
            ci.cancel();
        }
    }
}