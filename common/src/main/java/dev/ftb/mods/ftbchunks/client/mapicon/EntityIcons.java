package dev.ftb.mods.ftbchunks.client.mapicon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.CombinedIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
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
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntityIcons extends SimplePreparableReloadListener<Map<EntityType<?>, EntityIcons.EntityIconData>> {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final Icon NORMAL = Icon.getIcon("ftbchunks:textures/faces/normal.png");
	public static final Icon HOSTILE = Icon.getIcon("ftbchunks:textures/faces/hostile.png");

	private static final Map<ResourceLocation, Icon> ICON_CACHE = new HashMap<>();

	private static final Map<EntityType<?>, EntityIconData> ENTITY_ICONS = new HashMap<>();

	@Override
	protected Map<EntityType<?>, EntityIconData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<EntityType<?>, EntityIconData> map = new HashMap<>();

		for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : RegistrarManager.get(FTBChunks.MOD_ID).get(Registries.ENTITY_TYPE).entrySet()) {
			ResourceLocation id = entry.getKey().location();
			EntityType<?> entityType = entry.getValue();

			//Todo change this so that no tag but misc mobs need a texture to render
            if (entityType.getCategory() == MobCategory.MISC) {
                if (!entityType.is(FTBChunksTags.Entities.MINIMAP_ALLOWED_MISC_MOBS)) {
                    continue;
                }
            }

			String basePath = getBasePath(id);

			ResourceLocation invisible = FTBChunksAPI.rl(basePath + ".invisible");
			EntityIconSettings entityIconSettings = EntityIconSettings.DEFAULT;
			if (resourceManager.getResource(invisible).isPresent()) {
				//Todo Docs?
				LOGGER.error("Entity {} is using legacy invisible texture, please update it to use the new system!", id);
				entityIconSettings = EntityIconSettings.DEFAULT_DISABLED;
			}

			Optional<Resource> resource = resourceManager.getResource(FTBChunksAPI.rl(basePath + ".json"));
			if(resource.isPresent()) {
				try {
					entityIconSettings = getEntitySetting(id, resource.get());
				}catch (IOException e) {
					LOGGER.error("Failed to load entity icon settings for {}", id, e);
				}catch (IllegalStateException e) {
					LOGGER.error("Failed to parse entity icon settings for {}", id, e);
				}
			}else {
				LOGGER.error("Missing entity icon settings for {}", id);
				continue;
			}


			ResourceLocation mobTexture = entityIconSettings.texture;

			Map<String, Boolean> stringBooleanMap = FTBChunksClientConfig.ENTITY_ICON.get();
			if(!stringBooleanMap.containsKey(entityType.arch$registryName().toString())) {
				stringBooleanMap.put(entityType.arch$registryName().toString(), entityIconSettings.defaultEnabled);
				FTBChunksClientConfig.saveConfig();
			}

			Icon combinedIcon = CombinedIcon.getCombined(new HashSet<>(entityIconSettings.icon));
			map.put(entityType, new EntityIconData(combinedIcon, entityIconSettings));
			if (resourceManager.getResource(mobTexture).isPresent()) {
			}else if(!entityIconSettings.equals(EntityIconSettings.DEFAULT)) {
				LOGGER.error("Failed to load entity icon texture {} for {}", mobTexture, id);
			}
		}

		return map;
	}

	private static String getBasePath(ResourceLocation id) {
		return "textures/faces/" + id.getNamespace() + "/" + id.getPath();
	}

	public record EntityIconData(Icon icon, EntityIconSettings settings) {}

	private EntityIconSettings getEntitySetting(ResourceLocation id, Resource resource) throws IOException, IllegalStateException {
		JsonElement jsonElement = GsonHelper.fromJson(GSON, resource.openAsReader(), JsonElement.class);
		DataResult<EntityIconSettings> settings = EntityIconSettings.CODEC.parse(JsonOps.INSTANCE, jsonElement);
		return settings.getOrThrow();
	}

	public static Optional<Icon> getIcon(Entity entity) {
		return getIcon(entity.getType());
//		EntityIconData iconData = ENTITY_ICONS.get(entity.getType());
//		if(iconData != null) {
//			Optional<TextureData> right = iconData.settings.texture.right();
//			if(right.isPresent() && right.get().useMobTexture()) {
//				EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
//				return Optional.ofNullable(getOrCreateIcon(renderer.getTextureLocation(entity), iconData.settings));
//			}else {
//				return Optional.ofNullable(iconData.icon());
//			}
//		}else {
//			return Optional.empty();
//		}
	}

	public static Optional<Icon> getIcon(EntityType<?> entityType) {
		return Optional.ofNullable(ENTITY_ICONS.get(entityType)).map(data -> data.icon);
	}

	@Override
	protected void apply(Map<EntityType<?>, EntityIconData> entityIconDataMap, ResourceManager resourceManager, ProfilerFiller profiler) {
		ENTITY_ICONS.clear();
		ENTITY_ICONS.putAll(entityIconDataMap);

		Minecraft.getInstance().submit(() -> {
			for (EntityIconData icon : entityIconDataMap.values()) {
				if (icon.icon instanceof ImageIcon imageIcon) {
					imageIcon.bindTexture();
//					RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//					RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				}
			}
		});
	}

	public record EntityIconSettings(
			ResourceLocation texture,
			List<EntityImageIcon> icon,
			double scale,
			boolean defaultEnabled) {

		public static final EntityIconSettings DEFAULT = new EntityIconSettings(null, List.of(), 1D, true);

		public static final EntityIconSettings DEFAULT_DISABLED = new EntityIconSettings(null, List.of(), 1D, false);

		public static final Codec<EntityIconSettings> CODEC = RecordCodecBuilder.<EntityIconSettings>create(builder -> builder.group(
						ResourceLocation.CODEC.fieldOf("texture").forGetter(s -> s.texture),
						EntityImageIcon.CODEC.listOf().fieldOf("icon").forGetter(s -> s.icon),
						Codec.DOUBLE.optionalFieldOf("scale", 1D).forGetter(s -> s.scale),
						Codec.BOOL.optionalFieldOf("default_enabled", true).forGetter(s -> s.defaultEnabled))
				.apply(builder, EntityIconSettings::new)).validate(settings -> {
//			if (settings.texture.left().isPresent() && settings.slice.isPresent()) {
//				return DataResult.error(() -> "A texture width and height must be provided when using a slice");
//			}
			return DataResult.success(settings);
		});
	}

	public record TextureData(
			boolean useMobTexture,
			ResourceLocation texture,
			int width,
			int height) {

		public static final Codec<TextureData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.BOOL.optionalFieldOf("use_mob_texture", false).forGetter(s -> s.useMobTexture),
				ResourceLocation.CODEC.fieldOf("location").forGetter(s -> s.texture),
				Codec.intRange(1, Integer.MAX_VALUE).fieldOf("width").forGetter(s -> s.width),
				Codec.intRange(1, Integer.MAX_VALUE).fieldOf("height").forGetter(s -> s.height)
		).apply(builder, TextureData::new));
	}



	public static Optional<EntityIconData> getData(EntityType<?> entityType) {
		return Optional.ofNullable(ENTITY_ICONS.get(entityType));
	}

	public static Icon get(Entity entity) {
		return getIcon(entity)
				.orElseGet(() -> entity instanceof Enemy ? EntityIcons.HOSTILE : EntityIcons.NORMAL);
	}
}
