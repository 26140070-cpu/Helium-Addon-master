package com.helium;

import com.helium.hud.*;
import com.helium.modules.combat.*;
import com.helium.modules.render.*;
import com.helium.modules.shit.Autism;
import com.helium.modules.shit.Factspammer;
import com.helium.modules.utility.*;
import com.helium.modules.legit.UHC.*;
import com.helium.modules.legit.MacePvP.*;
import com.helium.util.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import static com.helium.hud.Watermark.MARKTEXT;

public class HeliumAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Helium", Items.WIND_CHARGE.getDefaultStack());
    public static final HudGroup Helium_Hud = new HudGroup("Helium");
    public static final Category LEGIT = new Category("Legit", Items.ENDER_PEARL.getDefaultStack());
    public static final Category LEGIT_MACE = new Category("Legit/MacePvP", Items.MACE.getDefaultStack());
    public static final Category LEGIT_MISC = new Category("Legit/Misc", Items.MACE.getDefaultStack());
    public static final Category LEGIT_UHC = new Category("Legit/UHC", Items.MACE.getDefaultStack());
    public static final Category LEGIT_VANILLA = new Category("Legit/Vanilla", Items.MACE.getDefaultStack());
    public static final Category SHIT = new Category("Shit", Items.SPECTRAL_ARROW.getDefaultStack());
    public static final String VERSION = "1.0.0";

    @Override
    public void onInitialize() {
        ChatUtils.registerCustomPrefix("com.helium", () -> Text.literal("Helium").formatted(Formatting.GOLD));
        LOG.info("Initializing Helium Addon");

        HeliumConfig.init();
        HeliumBackgroundConfig.init();
        HeliumMenuFogConfig.init();
        FontAutoInstaller.init();
        PanoramaConfig.init();
        MeteorClient.EVENT_BUS.subscribe(this);

        Modules.get().add(new HeliumRPC());
        Modules.get().add(new PackUtilToggle());
        Modules.get().add(new HeliumCrystalAura());
        Modules.get().add(new UltraOffhand());
        Modules.get().add(new UltraGapple());
        Modules.get().add(new BasicAnchorAura());
        Modules.get().add(new TrapAssist());
        Modules.get().add(new CustomLoadingScreen());
        Modules.get().add(new UltraSurround());
        Modules.get().add(new FastBreak());
        Modules.get().add(new VaultHack());
        Modules.get().add(new DeathSounds());
        Modules.get().add(new AutoEnchantTable());
        Modules.get().add(new SafeElytra());
        Modules.get().add(new PearlCatch());
        Modules.get().add(new LegitTotem());
        Modules.get().add(new StunLam());
        Modules.get().add(new WeatherChanger());
        Modules.get().add(new AutoCev());
        Modules.get().add(new AutoShieldBreak());
        Modules.get().add(new TriggerBot());
        Modules.get().add(new ComboTap());
        Modules.get().add(new Autism());
        Modules.get().add(new Factspammer());

        Hud.get().register(MARKTEXT);
        Hud.get().register(TotemsSecurity.INFO);
        HeliumUIRegistry.register();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (GuiThemes.get() != null && "JJS Style".equals(GuiThemes.get().name)) {
            if (Config.get() != null && !Config.get().customFont.get()) {
                Config.get().customFont.set(true);
            }
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(LEGIT);
        Modules.registerCategory(SHIT);
        Modules.registerCategory(LEGIT_MACE);
        Modules.registerCategory(LEGIT_MISC);
        Modules.registerCategory(LEGIT_UHC);
        Modules.registerCategory(LEGIT_VANILLA);
    }

    @Override
    public String getPackage() {
        return "com.helium";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "Helium Addon");
    }
}