package com.helium.mixin;

import com.example.addon.modules.PackUtilModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.helium.modules.utility.PackUtilToggle;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.util.List;

@Mixin(value = PackUtilModule.class, remap = false)
public class PackUtilModuleMixin {

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void overrideIsActive(CallbackInfoReturnable<Boolean> cir) {
        PackUtilToggle meteorModule = Modules.get().get(PackUtilToggle.class);

        if (meteorModule != null) {
            cir.setReturnValue(meteorModule.isActive());
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        PackUtilToggle module = Modules.get().get(PackUtilToggle.class);
        if (module == null || !module.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"), cancellable = true)
    private void onGameJoin(CallbackInfo ci) {
        PackUtilToggle module = Modules.get().get(PackUtilToggle.class);
        if (module == null || !module.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameLeft", at = @At("HEAD"), cancellable = true)
    private void onGameLeft(CallbackInfo ci) {
        PackUtilToggle module = Modules.get().get(PackUtilToggle.class);
        if (module == null || !module.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, List<Text> lines, CallbackInfo ci) {
        PackUtilToggle module = Modules.get().get(PackUtilToggle.class);
        if (module == null || !module.isActive()) {
            ci.cancel();
        }
    }
}