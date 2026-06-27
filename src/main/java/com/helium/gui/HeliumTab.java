package com.helium.gui;

import com.helium.HeliumAddon;
import com.helium.util.HeliumBackgroundConfig;
import com.helium.util.HeliumConfig;
import com.helium.util.HeliumMenuFogConfig;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.util.List;

import static com.helium.util.LogUtil.info;

public class HeliumTab extends Tab {
    public HeliumTab() {
        super("Helium");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new HeliumConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof HeliumConfigScreen;
    }

    private static class HeliumConfigScreen extends WindowTabScreen {
        private final Settings colorSettings = new Settings();
        private final SettingGroup sgColor = colorSettings.getDefaultGroup();

        private final Setting<SettingColor> splashColor = sgColor.add(new ColorSetting.Builder()
                .name("splash-color")
                .description("Color of the custom splash text.")
                .defaultValue(new SettingColor(
                        (HeliumConfig.getSplashColor() >> 16) & 0xFF,
                        (HeliumConfig.getSplashColor() >> 8) & 0xFF,
                        HeliumConfig.getSplashColor() & 0xFF,
                        255,
                        HeliumConfig.getSplashRainbow()
                ))
                .onChanged(c -> {
                    HeliumConfig.setSplashColor((c.r << 16) | (c.g << 8) | c.b);
                    HeliumConfig.setSplashRainbow(c.rainbow);
                })
                .build()
        );

        public HeliumConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WHorizontalList topBar = add(theme.horizontalList()).expandX().widget();
            topBar.add(theme.label("Helium v 1.0.0")).widget();
            topBar.add(theme.label("by Donalp012")).expandX().widget();

            WButton githubBtn = topBar.add(theme.button("GitHub")).widget();
            githubBtn.action = () -> Util.getOperatingSystem().open("https://github.com/26140070-cpu/Helium-Addon-master/");

            WButton discordBtn = topBar.add(theme.button("Discord")).widget();
            discordBtn.action = () -> Util.getOperatingSystem().open("https://discord.gg/G6CwQHBJF6");

            add(theme.horizontalSeparator()).expandX();

            add(theme.settings(colorSettings)).expandX();
            add(theme.horizontalSeparator()).expandX();

            WHorizontalList inputArea = add(theme.horizontalList()).expandX().widget();
            WTextBox splashInput = inputArea.add(theme.textBox("Enter new splash text...")).expandX().widget();
            WButton addBtn = inputArea.add(theme.button("Add")).widget();

            add(theme.horizontalSeparator()).expandX();

            WVerticalList splashList = add(theme.verticalList()).expandX().widget();
            renderSplashes(splashList);

            addBtn.action = () -> {
                String text = splashInput.get().trim();
                if (!text.isEmpty()) {
                    HeliumConfig.addText(text);
                    splashInput.set("");
                    renderSplashes(splashList);
                }
            };

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList bgEnabledRow = add(theme.horizontalList()).expandX().widget();
            bgEnabledRow.add(theme.label("Custom Background:")).widget();
            WCheckbox bgEnabledCheckbox = bgEnabledRow.add(theme.checkbox(HeliumBackgroundConfig.enabled)).widget();
            WButton applyBgBtn = bgEnabledRow.add(theme.button("Apply")).widget();

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList imageRow = add(theme.horizontalList()).expandX().widget();
            imageRow.add(theme.label("Image:")).widget();
            List<String> imageFiles = HeliumBackgroundConfig.getImageFiles();
            if (imageFiles.isEmpty()) imageFiles.add("No images found");
            WDropdown<String> imageDropdown = imageRow.add(theme.dropdown(imageFiles.toArray(new String[0]),
                    HeliumBackgroundConfig.imageFileName.isEmpty() ? imageFiles.get(0) : HeliumBackgroundConfig.imageFileName)).expandX().widget();

            WHorizontalList audioRow = add(theme.horizontalList()).expandX().widget();
            audioRow.add(theme.label("Audio:")).widget();
            List<String> audioFiles = HeliumBackgroundConfig.getAudioFiles();
            if (audioFiles.isEmpty()) audioFiles.add("No audio found");
            WDropdown<String> audioDropdown = audioRow.add(theme.dropdown(audioFiles.toArray(new String[0]),
                    HeliumBackgroundConfig.audioFileName.isEmpty() ? audioFiles.get(0) : HeliumBackgroundConfig.audioFileName)).expandX().widget();

            applyBgBtn.action = () -> {
                HeliumBackgroundConfig.enabled = bgEnabledCheckbox.checked;

                String selectedImage = imageDropdown.get();
                if (selectedImage != null && !selectedImage.equals("No images found")) {
                    HeliumBackgroundConfig.imageFileName = selectedImage;
                }

                String selectedAudio = audioDropdown.get();
                if (selectedAudio != null && !selectedAudio.equals("No audio found")) {
                    HeliumBackgroundConfig.audioFileName = selectedAudio;
                }

                HeliumBackgroundConfig.save();
                info("Background settings applied.");
            };

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList fogRow = add(theme.horizontalList()).expandX().widget();
            fogRow.add(theme.label("Menu Fog:")).widget();
            WCheckbox fogCheckbox = fogRow.add(theme.checkbox(HeliumMenuFogConfig.enabled)).widget();
            WButton applyFog = fogRow.add(theme.button("Apply")).widget();

            applyFog.action = () -> {
                HeliumMenuFogConfig.enabled = fogCheckbox.checked;
                HeliumMenuFogConfig.save();
                info("Menu Fog " + (HeliumMenuFogConfig.enabled ? "enabled" : "disabled") + ".");
            };

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList cleanerRow = add(theme.horizontalList()).expandX().widget();
            cleanerRow.add(theme.label("Title Cleaner:")).widget();
            WCheckbox cleanerCheckbox = cleanerRow.add(theme.checkbox(HeliumBackgroundConfig.cleanerEnabled)).widget();
            WButton applyCleaner = cleanerRow.add(theme.button("Apply")).widget();

            applyCleaner.action = () -> {
                HeliumBackgroundConfig.cleanerEnabled = cleanerCheckbox.checked;
                HeliumBackgroundConfig.save();
                info("Title Cleaner " + (HeliumBackgroundConfig.cleanerEnabled ? "enabled" : "disabled") + ".");
            };
        }

        private void renderSplashes(WVerticalList list) {
            list.clear();
            for (int i = 0; i < HeliumConfig.splashTexts.size(); i++) {
                final int index = i;
                String text = HeliumConfig.splashTexts.get(index);

                WHorizontalList row = list.add(theme.horizontalList()).expandX().widget();
                row.add(theme.label("- " + text)).expandX();

                WButton deleteBtn = row.add(theme.button("X")).widget();
                deleteBtn.action = () -> {
                    HeliumConfig.removeText(index);
                    renderSplashes(list);
                };
            }
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}