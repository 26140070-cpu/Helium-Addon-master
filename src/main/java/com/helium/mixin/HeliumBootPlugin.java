package com.helium.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class HeliumBootPlugin implements IMixinConfigPlugin {

    private boolean hasConflict = false;

    @Override
    public void onLoad(String mixinPackage) {
        hasConflict = FabricLoader.getInstance().isModLoaded("catppuccin-addon")
                && FabricLoader.getInstance().isModLoaded("vicore-addon");

        if (hasConflict) {
            System.out.println("[Helium] Conflict detected between Catppuccin and Vicore. Resolving interfaces.");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (hasConflict && mixinClassName.contains("vicore") && mixinClassName.contains("GuiThemeMixin")) {
            System.out.println("[Helium] Bypassed Vicore GuiThemeMixin to prevent system initialization crash.");
            return false;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}