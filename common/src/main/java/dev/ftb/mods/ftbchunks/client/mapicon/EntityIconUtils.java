package dev.ftb.mods.ftbchunks.client.mapicon;

import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader.EntityIconSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public class EntityIconUtils {
    public static boolean canTypeRender(EntityType<?> type) {
        return type != EntityType.PLAYER && EntityIconLoader.getSettings(type).isPresent();
    }

    public static boolean shouldEntityRender(Entity entity, Player player) {
        if (!canTypeRender(entity.getType()) || entity.isInvisibleTo(player)) {
            return false;
        }

        return BuiltInRegistries.ENTITY_TYPE.getResourceKey(entity.getType())
                .map(key -> isIconEnabled(key, entity.getType()))
                .orElse(false);
    }

    private static boolean isIconEnabled(ResourceKey<EntityType<?>> key, EntityType<?> type) {
        if (!FTBChunksClientConfig.ENTITY_ICON.get().containsKey(key.identifier().toString())) {
            // entity not listed in the config (most likely a new mod was added) - get its defaults if possible
            EntityIconSettings settings = EntityIconLoader.getSettings(type).orElse(EntityIconSettings.legacy());
            FTBChunksClientConfig.ENTITY_ICON.get().put(key.identifier().toString(), settings.defaultEnabled());
            FTBChunksClientConfig.saveConfig();
            return settings.defaultEnabled();
        } else {
            return FTBChunksClientConfig.ENTITY_ICON.get().get(key.identifier().toString());
        }
    }
}
