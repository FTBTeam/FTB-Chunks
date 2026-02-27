package dev.ftb.mods.ftbchunks;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.ftb.mods.ftbchunks.client.gui.EntityIconSettingsScreen;
import dev.ftb.mods.ftblibrary.client.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.config.value.AbstractMapValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

public class EntityTypeBoolMapValue extends AbstractMapValue<Boolean> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EntityTypeBoolMapValue(Config parent, String key, Map<String, Boolean> defaultValue) {
        super(parent, key, defaultValue, Codec.BOOL);
    }

    @Override
    protected @Nullable EditableConfigValue<?> fillClientConfig(EditableConfigGroup group) {
        return group.add(key, new EntityTypeBoolMapConfigValue(), get(), stringBooleanMap -> {}, defaultValue);
    }

    public static class EntityTypeBoolMapConfigValue extends EditableConfigValue<Map<String, Boolean>> {
        @Override
        public void onClicked(Widget clickedWidget, MouseButton button, ConfigCallback callback) {
            new EntityIconSettingsScreen(false).openGui();
        }

        @Override
        public Component getStringForGUI(Map<String, Boolean> v) {
            MutableInt enabled = new MutableInt();
            MutableInt disabled = new MutableInt();
            for (String entityTypeResourceKey : v.keySet()) {
                BuiltInRegistries.ENTITY_TYPE.get(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(entityTypeResourceKey))).ifPresent(holder -> {
                    if (v.get(entityTypeResourceKey)) {
                        enabled.increment();
                    } else {
                        disabled.increment();
                    }
                });
            }
            return Component.translatable("ftbchunks.gui.enabled_disabled_count", enabled.get(), disabled.get());
        }
    }

}
