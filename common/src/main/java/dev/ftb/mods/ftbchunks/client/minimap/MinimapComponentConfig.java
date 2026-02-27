package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.client.gui.MinimapInfoSortScreen;
import dev.ftb.mods.ftblibrary.client.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.config.value.Config;
import dev.ftb.mods.ftblibrary.config.value.StringMapValue;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class MinimapComponentConfig extends StringMapValue {
    public MinimapComponentConfig(@Nullable Config parent, String name, Map<String, String> def) {
        super(parent, name, def);
    }

    @Override
    protected @Nullable EditableConfigValue<?> fillClientConfig(EditableConfigGroup group) {
        return group.add(key, new MinimapComponentConfigValue(), get(), stringBooleanMap -> {}, defaultValue);
    }

    public static class MinimapComponentConfigValue extends EditableConfigValue<Map<String, String>> {
        @Override
        public void onClicked(Widget clickedWidget, MouseButton button, ConfigCallback callback) {
            new MinimapInfoSortScreen().openGui();
        }

        @Override
        public Component getStringForGUI(Map<String, String> v) {
            return Component.translatable("ftbchunks.gui.sort_minimap_info");
        }
    }
}
