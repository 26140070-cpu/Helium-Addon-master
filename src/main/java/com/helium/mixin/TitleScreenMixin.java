package com.helium.mixin;

import com.helium.util.HeliumBackgroundConfig;
import com.helium.util.HeliumConfig;
import com.helium.util.HeliumScreenBackground;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    private @Nullable SplashTextRenderer splashText;

    @Unique
    private int timer = 0;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        updateSplash();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (HeliumConfig.getSplashRainbow()) {
            updateSplash();
        } else {
            timer++;
            if (timer >= 200) {
                timer = 0;
                HeliumConfig.updateIndex();
                updateSplash();
            }
        }
    }

    @Unique
    private void updateSplash() {
        String text = HeliumConfig.getCurrentSplash();
        int color;

        if (HeliumConfig.getSplashRainbow()) {
            color = RainbowColors.GLOBAL.getPacked();
        } else {
            color = HeliumConfig.getSplashColor();
        }

        this.splashText = new SplashTextRenderer(Text.literal(text).withColor(color));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (HeliumBackgroundConfig.enabled && !HeliumBackgroundConfig.imageFileName.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getSoundManager() != null) {
                client.getSoundManager().stopSounds(null, SoundCategory.MUSIC);
            }
            context.fill(0, 0, this.width, this.height, 0xFF000000);
            HeliumScreenBackground.render(context, this.width, this.height);
        } else {
            HeliumScreenBackground.stopAudio();
        }
    }
}