package com.helium.mixin;

import com.helium.util.HeliumMenuFog;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WidgetScreen.class)
public abstract class WidgetScreenMixin extends Screen {

    protected WidgetScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void afterRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HeliumMenuFog.render(context, this.width, this.height);
    }
}