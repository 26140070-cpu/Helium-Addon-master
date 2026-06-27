
package com.helium.util;

import java.io.File;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.fabricmc.loader.api.FabricLoader;
import io.netty.util.internal.ThreadLocalRandom;

public class HeliumUtils {
    public static final boolean XAERO_AVAILABLE = FabricLoader.getInstance().isModLoaded("xaeroworldmap")
            && FabricLoader.getInstance().isModLoaded("xaerominimap");

    public enum RainbowColor {
        Reds(new String[]{"§c", "§4"}),
        Yellows(new String[]{"§e", "§6"}),
        Greens(new String[]{"§a", "§2"}),
        Cyans(new String[]{"§b", "§3"}),
        Blues(new String[]{"§9", "§1"}),
        Purples(new String[]{"§d", "§5"});

        public final String[] labels;

        RainbowColor(String[] labels) { this.labels = labels; }

        public static RainbowColor getFirst() {
            return RainbowColor.values()[ThreadLocalRandom.current().nextInt(RainbowColor.values().length)];
        }

        public static RainbowColor getNext(RainbowColor previous) {
            return switch (previous) {
                case Reds -> Yellows;
                case Yellows -> Greens;
                case Greens -> Cyans;
                case Cyans -> Blues;
                case Blues -> Purples;
                case Purples -> Reds;
            };
        }
    }

    public enum TextColor {
        Black("§0"), White("§f"), Gray("§8"), Light_Gray("§7"),
        Dark_Green("§2"), Green("§a"), Dark_Aqua("§3"), Aqua("§b"),
        Dark_Blue("§1"), Blue("§9"), Dark_Red("§4"), Red("§c"),
        Dark_Purple("§5"), Purple("§d"), Gold("§6"), Yellow("§e"),
        Random("");

        public final String label;

        TextColor(String label) {
            this.label = label;
        }
    }

    public enum TextFormat {
        Plain(""), Italic("§o"), Bold("§l"),
        Underline("§n"), Strikethrough("§m"),
        Obfuscated("§k");

        public final String label;

        TextFormat(String label) {
            this.label = label;
        }
    }

    /** Random Color-Code */
    public static String rCC() {
        String color = "§7";
        TextColor[] colors = TextColor.values();

        while (color.equals("§0") || color.equals("§8") || color.equals("§7")) {
            int luckyIndex = ThreadLocalRandom.current().nextInt(colors.length);
            color = colors[luckyIndex].label;
        }

        return color;
    }

    public static ItemStack chooseMenuIcon() {
        int luckyIndex = ThreadLocalRandom.current().nextInt(menuIcons.length);
        return menuIcons[luckyIndex];
    }

    private static final ItemStack[] discIcons = {
            Items.MUSIC_DISC_5.getDefaultStack(),
            Items.MUSIC_DISC_11.getDefaultStack(),
            Items.MUSIC_DISC_13.getDefaultStack(),
            Items.MUSIC_DISC_CAT.getDefaultStack(),
            Items.MUSIC_DISC_FAR.getDefaultStack(),
            Items.MUSIC_DISC_MALL.getDefaultStack(),
            Items.MUSIC_DISC_STAL.getDefaultStack(),
            Items.MUSIC_DISC_WARD.getDefaultStack(),
            Items.MUSIC_DISC_WAIT.getDefaultStack(),
            Items.MUSIC_DISC_CHIRP.getDefaultStack(),
            Items.MUSIC_DISC_STRAD.getDefaultStack(),
            Items.MUSIC_DISC_RELIC.getDefaultStack(),
            Items.MUSIC_DISC_BLOCKS.getDefaultStack(),
            Items.MUSIC_DISC_MELLOHI.getDefaultStack(),
            Items.MUSIC_DISC_PIGSTEP.getDefaultStack(),
            Items.MUSIC_DISC_CREATOR.getDefaultStack(),
            Items.MUSIC_DISC_PRECIPICE.getDefaultStack(),
            Items.MUSIC_DISC_OTHERSIDE.getDefaultStack(),
            Items.MUSIC_DISC_CREATOR_MUSIC_BOX.getDefaultStack(),
    };

    private static final ItemStack[] doorIcons = {
            Items.OAK_DOOR.getDefaultStack(),
            Items.IRON_DOOR.getDefaultStack(),
            Items.BIRCH_DOOR.getDefaultStack(),
            Items.BAMBOO_DOOR.getDefaultStack(),
            Items.CHERRY_DOOR.getDefaultStack(),
            Items.JUNGLE_DOOR.getDefaultStack(),
            Items.ACACIA_DOOR.getDefaultStack(),
            Items.SPRUCE_DOOR.getDefaultStack(),
            Items.WARPED_DOOR.getDefaultStack(),
            Items.COPPER_DOOR.getDefaultStack(),
            Items.CRIMSON_DOOR.getDefaultStack(),
            Items.MANGROVE_DOOR.getDefaultStack(),
            Items.DARK_OAK_DOOR.getDefaultStack(),
            Items.EXPOSED_COPPER_DOOR.getDefaultStack(),
            Items.OXIDIZED_COPPER_DOOR.getDefaultStack(),
            Items.WEATHERED_COPPER_DOOR.getDefaultStack()
    };

    private static final ItemStack[] menuIcons = {
            Items.CAKE.getDefaultStack(),
            Items.SPAWNER.getDefaultStack(),
            Items.BEDROCK.getDefaultStack(),
            Items.GOAT_HORN.getDefaultStack(),
            Items.HONEYCOMB.getDefaultStack(),
            Items.LODESTONE.getDefaultStack(),
            Items.DRAGON_EGG.getDefaultStack(),
            Items.FILLED_MAP.getDefaultStack(),
            Items.PINK_TULIP.getDefaultStack(),
            Items.TURTLE_EGG.getDefaultStack(),
            Items.NETHER_STAR.getDefaultStack(),
            Items.WITHER_ROSE.getDefaultStack(),
            Items.PINK_PETALS.getDefaultStack(),
            Items.WARPED_SIGN.getDefaultStack(),
            Items.CHERRY_SIGN.getDefaultStack(),
            Items.WIND_CHARGE.getDefaultStack(),
            Items.WRITTEN_BOOK.getDefaultStack(),
            Items.DAMAGED_ANVIL.getDefaultStack(),
            Items.CHERRY_SAPLING.getDefaultStack(),
            Items.JACK_O_LANTERN.getDefaultStack(),
            Items.KNOWLEDGE_BOOK.getDefaultStack(),
            Items.FIREWORK_ROCKET.getDefaultStack(),
            Items.TOTEM_OF_UNDYING.getDefaultStack(),
            Items.LIME_SHULKER_BOX.getDefaultStack(),
            Items.AMETHYST_CLUSTER.getDefaultStack(),
            Items.FLOWERING_AZALEA.getDefaultStack(),
            Items.PINK_SHULKER_BOX.getDefaultStack(),
            Items.GILDED_BLACKSTONE.getDefaultStack(),
            Items.OMINOUS_TRIAL_KEY.getDefaultStack(),
            Items.HEART_POTTERY_SHERD.getDefaultStack(),
            Items.LIGHT_BLUE_SHULKER_BOX.getDefaultStack(),
            Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(),
            Items.HEARTBREAK_POTTERY_SHERD.getDefaultStack(),
            Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack(),
            discIcons[ThreadLocalRandom.current().nextInt(discIcons.length)],
            doorIcons[ThreadLocalRandom.current().nextInt(doorIcons.length)]
    };
}