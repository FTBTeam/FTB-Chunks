package dev.ftb.mods.ftbchunks;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.client.gui.EntityIconSettingsScreen;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class EntityTypeBoolMapValue extends BaseValue<Map<ResourceKey<EntityType<?>>, Boolean>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public EntityTypeBoolMapValue(@Nullable SNBTConfig c, String n, Map<ResourceKey<EntityType<?>>, Boolean> def) {
        super(c, n, def);
        super.set(new HashMap<>(def));
    }

    @Override
    public void write(SNBTCompoundTag tag) {
        Map<ResourceKey<EntityType<?>>, Boolean> map = get();
        SNBTCompoundTag mapTag = new SNBTCompoundTag();

        for (Map.Entry<ResourceKey<EntityType<?>>, Boolean> entry : map.entrySet()) {
            mapTag.putBoolean(entry.getKey().location().toString(), entry.getValue());
        }

        tag.put(key, mapTag);
    }

    @Override
    public void read(SNBTCompoundTag tag) {
        Map<ResourceKey<EntityType<?>>, Boolean> map = new HashMap<>();

        SNBTCompoundTag compound = tag.getCompound(key);
        for (String key : compound.getAllKeys()) {
            try {
                ResourceLocation parse = ResourceLocation.parse(key);
                map.put(ResourceKey.create(Registries.ENTITY_TYPE, parse), compound.getBoolean(key));
            } catch (ResourceLocationException e) {
                LOGGER.error("Failed to parse {} skipping", key, e);
            }
        }

        set(map);
    }

    @Override
    public void createClientConfig(ConfigGroup group) {
        group.add(key, new EntityTypeBoolMapConfigValue(), get(), stringBooleanMap -> {
        }, defaultValue);
    }

    public static class EntityTypeBoolMapConfigValue extends ConfigValue<Map<ResourceKey<EntityType<?>>, Boolean>> {

        @Override
        public void onClicked(Widget clickedWidget, MouseButton button, ConfigCallback callback) {
            new EntityIconSettingsScreen(false).openGui();
        }

        @Override
        public Component getStringForGUI(@Nullable Map<ResourceKey<EntityType<?>>, Boolean> v) {
            if (v == null) {
                return super.getStringForGUI(null);
            }
            int enabled = 0;
            int disabled = 0;
            for (ResourceKey<EntityType<?>> entityTypeResourceKey : v.keySet()) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeResourceKey);
                if (entityType != null) {
                    if (v.get(entityTypeResourceKey)) {
                        enabled++;
                    } else {
                        disabled++;
                    }
                }
            }
            return Component.translatable("ftbchunks.gui.enabled_disabled_count", enabled, disabled);
        }
    }

}
