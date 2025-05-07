package dev.ftb.mods.ftbchunks.client.mapicon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Handles loading of Entity Icon Json or Image file
// Loads all entity types expect for misc, where then it will only show on the map if a json / image exists
public class EntityIcons extends SimplePreparableReloadListener<Map<EntityType<?>, EntityIcons.EntityIconSettings>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Icon NORMAL = Icon.getIcon("ftbchunks:textures/faces/normal.png");
    public static final Icon HOSTILE = Icon.getIcon("ftbchunks:textures/faces/hostile.png");

    private static final Map<EntityType<?>, Map<ResourceLocation, Icon>> ICON_CACHE = new HashMap<>();

    private static final Map<EntityType<?>, EntityIconSettings> ENTITY_SETTINGS = new HashMap<>();

    public static final Map<EntityType<?>, Boolean> IS_USING_NEW_JSON = new HashMap<>();

    @Override
    protected Map<EntityType<?>, EntityIconSettings> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<EntityType<?>, EntityIconSettings> map = new HashMap<>();

        for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : RegistrarManager.get(FTBChunks.MOD_ID).get(Registries.ENTITY_TYPE).entrySet()) {
            ResourceLocation id = entry.getKey().location();
            EntityType<?> entityType = entry.getValue();

            String basePath = getBasePath(id);

            ResourceLocation invisible = FTBChunksAPI.rl(basePath + ".invisible");

            EntityIconSettings entityIconSettings = null;

            if (resourceManager.getResource(invisible).isPresent()) {
                LOGGER.error("Entity {} is using legacy invisible texture, please update it to use the new system!", id);

                entityIconSettings = EntityIconSettings.OLD_HIDDEN;
            }

            Optional<Resource> resource = resourceManager.getResource(FTBChunksAPI.rl(basePath + ".json"));

            if (resource.isPresent()) {
                entityIconSettings = getEntitySetting(id, resource.get());
                IS_USING_NEW_JSON.put(entityType, true);
            } else {
                ResourceLocation rl = FTBChunksAPI.rl(basePath + ".png");
                Optional<Resource> pic = resourceManager.getResource(rl);
                if (pic.isPresent()) {
                    entityIconSettings = new EntityIconSettings(false, Optional.of(rl), Optional.empty(), List.of(), WidthHeight.DEFAULT, Optional.empty(), 1D, true);
                }
            }

            if (entityIconSettings == null && entityType.getCategory() != MobCategory.MISC) {
                if (FTBChunks.isDevMode()) {
                    LOGGER.error("Missing entity icon settings for {}", id);
                }
                entityIconSettings = EntityIconSettings.OLD_HIDDEN;
            }

            map.put(entityType, entityIconSettings);
        }

        return map;
    }

    public static boolean canTypeRenderer(EntityType<?> type) {
        return type != EntityType.PLAYER && getSettings(type).isPresent();
    }

    public static boolean shouldEntityRender(Entity entity, Player player) {
        if (!canTypeRenderer(entity.getType()) || entity.isInvisibleTo(player)) {
            return false;
        }

        return BuiltInRegistries.ENTITY_TYPE.getResourceKey(entity.getType())
                .map(key -> isIconEnabled(key, entity.getType()))
                .orElse(false);
    }

    private static boolean isIconEnabled(ResourceKey<EntityType<?>> key, EntityType<?> type) {
        if (!FTBChunksClientConfig.ENTITY_ICON.get().containsKey(key)) {
            // entity not listed in the config (most likely a new mod was added) - get its defaults if possible
            EntityIconSettings settings = ENTITY_SETTINGS.getOrDefault(type, EntityIconSettings.OLD_HIDDEN);
            FTBChunksClientConfig.ENTITY_ICON.get().put(key, settings.defaultEnabled);
            FTBChunksClientConfig.saveConfig();
            return settings.defaultEnabled;
        } else {
            return FTBChunksClientConfig.ENTITY_ICON.get().get(key);
        }
    }

    private static String getBasePath(ResourceLocation id) {
        return "textures/faces/" + id.getNamespace() + "/" + id.getPath();
    }

    private EntityIconSettings getEntitySetting(ResourceLocation id, Resource resource) {
        try {
            JsonElement jsonElement = GsonHelper.fromJson(GSON, resource.openAsReader(), JsonElement.class);
            DataResult<EntityIconSettings> settings = EntityIconSettings.CODEC.parse(JsonOps.INSTANCE, jsonElement);
            return settings.getOrThrow();
        } catch (IOException e) {
            LOGGER.error("Failed to load entity icon settings for {}", id, e);
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to parse entity icon settings for {}", id, e);
        }
        return null;
    }

    private static Optional<Icon> getIconCache(Entity entity) {
        return getSettings(entity.getType())
                .map(settings -> {
                    if (settings.useMobTexture) {
                        return getOrCreateIcon(entity.getType(), Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity), settings);
                    } else {
                        return settings.texture.map(resourceLocation -> getOrCreateIcon(entity.getType(), resourceLocation, settings)).orElse(null);
                    }
                });
    }

    public static Optional<EntityIconSettings> getSettings(EntityType<?> entityType) {
        return Optional.ofNullable(ENTITY_SETTINGS.get(entityType));
    }

    private static Icon getOrCreateIcon(EntityType<?> entityType, ResourceLocation texture, EntityIconSettings settings) {
        return ICON_CACHE
                .computeIfAbsent(entityType, i -> new HashMap<>())
                .computeIfAbsent(texture, t -> new EntityImageIcon(t, settings.mainSlice.orElse(null), settings.children, settings.defaultImageSize.orElse(null)));
    }

    @Override
    protected void apply(Map<EntityType<?>, EntityIconSettings> entityIconDataMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        ICON_CACHE.clear();
        ENTITY_SETTINGS.clear();
        ENTITY_SETTINGS.putAll(entityIconDataMap);
    }

    public record EntityIconSettings(
            boolean useMobTexture,
            Optional<ResourceLocation> texture,
            Optional<EntityImageIcon.Slice> mainSlice,
            List<EntityImageIcon.ChildIconData> children,
            WidthHeight widthHeight,
            Optional<EntityIcons.WidthHeight> defaultImageSize,
            double scale,
            boolean defaultEnabled) {

        private static final EntityIconSettings OLD_HIDDEN = new EntityIconSettings(
                false, Optional.empty(), Optional.empty(),
                List.of(), WidthHeight.DEFAULT, Optional.empty(), 1D, true
        );

        public static final Codec<EntityIconSettings> CODEC = RecordCodecBuilder.<EntityIconSettings>create(builder ->
                builder.group(
                                Codec.BOOL.optionalFieldOf("use_mob_texture", false).forGetter(s -> s.useMobTexture),
                                ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(s -> s.texture),
                                EntityImageIcon.Slice.CODEC.optionalFieldOf("slice").forGetter(entityIconData -> entityIconData.mainSlice),
                                EntityImageIcon.ChildIconData.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(entityIconData -> entityIconData.children),
                                WidthHeight.CODEC.optionalFieldOf("size", WidthHeight.DEFAULT).forGetter(s -> s.widthHeight),
                                WidthHeight.CODEC.optionalFieldOf("default_image_size").forGetter(s -> s.defaultImageSize),
                                Codec.DOUBLE.optionalFieldOf("scale", 1D).forGetter(s -> s.scale),
                                Codec.BOOL.optionalFieldOf("default_enabled", true).forGetter(s -> s.defaultEnabled))
                        .apply(builder, EntityIconSettings::new)
        ).validate(settings -> {
            if (settings.texture().isEmpty() && !settings.useMobTexture) {
                return DataResult.error(() -> "Texture is required if use_mob_texture is false");
            }
            return DataResult.success(settings);
        });

    }

    public record WidthHeight(int width, int height) {

        public static final WidthHeight DEFAULT = new WidthHeight(16, 16);

        public static final Codec<WidthHeight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("width").forGetter(WidthHeight::width),
                Codec.INT.fieldOf("height").forGetter(WidthHeight::height)
        ).apply(instance, WidthHeight::new));
    }

    public static Icon getIcon(Entity entity) {
        return getIconCache(entity).orElseGet(() -> entity instanceof Enemy ? EntityIcons.HOSTILE : EntityIcons.NORMAL);
    }

    public static Icon getIcon(EntityType<?> entityType) {
        Entity entity = entityType.create(Minecraft.getInstance().level);
        if (entity == null) {
            return EntityIcons.NORMAL;
        } else {
            return getIcon(entity);
        }
    }

}
