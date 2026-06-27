package com.helium.mixin;

import com.helium.modules.render.CustomLoadingScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Shadow @Final private MinecraftClient client;

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"),
            index = 4,
            require = 0
    )
    private int overrideBackgroundColor(int originalColor) {
        CustomLoadingScreen cls = CustomLoadingScreen.INSTANCE;
        if (cls != null && cls.isActive()) {
            return cls.getBackgroundColor();
        }
        return originalColor;
    }
}