package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.client.gui.MinimapInfoSortScreen;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.snbt.config.StringMapValue;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MinimapComponentConfig extends StringMapValue {

    public MinimapComponentConfig(@Nullable SNBTConfig c, String n, Map<String, String> def) {
        super(c, n, def);
    }


    @Override
    public void createClientConfig(ConfigGroup group) {
        group.add(key, new MinimapComponentConfigValue(), get(), stringBooleanMap -> {
        }, defaultValue);
    }

    public static class MinimapComponentConfigValue extends ConfigValue<Map<String, String>> {

        @Override
        public void onClicked(Widget clickedWidget, MouseButton button, ConfigCallback callback) {
            new MinimapInfoSortScreen().openGui();
        }

        @Override
        public Component getStringForGUI(@Nullable Map<String, String> v) {
            if (v == null) {
                return super.getStringForGUI(null);
            }
            return Component.translatable("ftbchunks.gui.sort_minimap_info");
        }
    }
}
