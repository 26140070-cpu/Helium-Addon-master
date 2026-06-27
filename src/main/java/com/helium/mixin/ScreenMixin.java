package com.helium.mixin;

import com.helium.util.HeliumBackgroundConfig;
import com.helium.util.HeliumScreenBackground;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!HeliumBackgroundConfig.enabled || HeliumBackgroundConfig.imageFileName.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) return;
        Screen self = (Screen) (Object) this;
        if (self instanceof HandledScreen) return;
        if (self instanceof WidgetScreen) return;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        context.fill(0, 0, width, height, 0xFF000000);
        HeliumScreenBackground.render(context, width, height);
        ci.cancel();
    }
}