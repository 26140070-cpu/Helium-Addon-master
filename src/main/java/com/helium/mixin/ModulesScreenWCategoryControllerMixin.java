package com.helium.mixin;

import com.helium.mixin.accessor.WidgetScreenAccessor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(targets = "meteordevelopment.meteorclient.gui.screens.ModulesScreen$WCategoryController", remap = false)
public abstract class ModulesScreenWCategoryControllerMixin {

    @Shadow
    public List<WWindow> windows;

    @Shadow
    private Cell<WWindow> favorites;

    @Shadow
    private ModulesScreen this$0;

    @Overwrite
    public void init() {
        ModulesScreen screen = this$0;
        GuiTheme theme = ((WidgetScreenAccessor) screen).getTheme();

        Map<String, Map<String, List<Module>>> grouped = new LinkedHashMap<>();
        for (Category category : Modules.loopCategories()) {
            for (Module module : Modules.get().getGroup(category)) {
                if (Config.get().hiddenModules.get().contains(module)) continue;

                String fullName = category.name;
                String parent, child;
                int slash = fullName.indexOf('/');
                if (slash == -1) {
                    parent = fullName;
                    child = "";
                } else {
                    parent = fullName.substring(0, slash);
                    child = fullName.substring(slash + 1);
                }
                grouped
                        .computeIfAbsent(parent, k -> new LinkedHashMap<>())
                        .computeIfAbsent(child, k -> new ArrayList<>())
                        .add(module);
            }
        }

        WContainer self = (WContainer) (Object) this;

        for (Map.Entry<String, Map<String, List<Module>>> parentEntry : grouped.entrySet()) {
            String parent = parentEntry.getKey();
            Map<String, List<Module>> children = parentEntry.getValue();

            WWindow window = theme.window(parent);
            window.id = parent;

            self.add(window);
            windows.add(window);

            for (Map.Entry<String, List<Module>> childEntry : children.entrySet()) {
                String child = childEntry.getKey();
                List<Module> modules = childEntry.getValue();

                if (!child.isEmpty()) {
                    window.add(theme.horizontalSeparator(child)).expandX();
                }

                for (Module module : modules) {
                    window.add(theme.module(module)).expandX();
                }
            }
        }

        WWindow searchWindow = theme.window("Search");
        searchWindow.id = "search";
        if (theme.categoryIcons()) {
            searchWindow.beforeHeaderInit = wContainer -> wContainer.add(theme.item(new ItemStack(Items.COMPASS))).pad(2);
        }
        self.add(searchWindow);
        windows.add(searchWindow);

        if (searchWindow.view != null) {
            searchWindow.view.scrollOnlyWhenMouseOver = true;
            searchWindow.view.hasScrollBar = false;
            searchWindow.view.maxHeight -= 20;
        }

        WVerticalList searchList = theme.verticalList();
        WTextBox searchTextBox = theme.textBox("");
        searchTextBox.setFocused(true);
        searchWindow.add(searchTextBox).minWidth(140).expandX();
        searchWindow.add(searchList).expandX();

        searchTextBox.action = () -> {
            searchList.clear();
            String text = searchTextBox.get().trim().toLowerCase();

            if (!text.isEmpty()) {
                List<Module> found = new ArrayList<>();
                for (Module module : Modules.get().getAll()) {
                    if (module.name.toLowerCase().contains(text)) {
                        found.add(module);
                    }
                }

                if (!found.isEmpty()) {
                    searchList.add(theme.horizontalSeparator("Modules")).expandX();
                    for (Module module : found) {
                        searchList.add(theme.module(module)).expandX();
                    }
                }
            }
        };
    }
}