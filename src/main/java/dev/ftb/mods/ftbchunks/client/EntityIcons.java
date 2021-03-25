package dev.ftb.mods.ftbchunks.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class EntityIcons extends SimplePreparableReloadListener<Map<EntityType<?>, ResourceLocation>> {
	public static final ResourceLocation INVISIBLE = new ResourceLocation("ftbchunks:textures/faces/invisible.png");
	public static final ResourceLocation NORMAL = new ResourceLocation("ftbchunks:textures/faces/normal.png");
	public static final ResourceLocation HOSTILE = new ResourceLocation("ftbchunks:textures/faces/hostile.png");
	public static final Map<EntityType<?>, ResourceLocation> ENTITY_ICONS = new HashMap<>();

	@Override
	protected Map<EntityType<?>, ResourceLocation> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<EntityType<?>, ResourceLocation> map = new HashMap<>();

		for (EntityType<?> t : ForgeRegistries.ENTITIES) {
			if (t.getCategory() == MobCategory.MISC) {
				continue;
			}

			ResourceLocation invisible = new ResourceLocation("ftbchunks:textures/faces/" + t.getRegistryName().getNamespace() + "/" + t.getRegistryName().getPath() + ".invisible");

			if (resourceManager.hasResource(invisible)) {
				map.put(t, INVISIBLE);
				continue;
			}

			ResourceLocation texture = new ResourceLocation("ftbchunks:textures/faces/" + t.getRegistryName().getNamespace() + "/" + t.getRegistryName().getPath() + ".png");

			if (resourceManager.hasResource(texture)) {
				map.put(t, texture);
			}
		}

		return map;
	}

	@Override
	protected void apply(Map<EntityType<?>, ResourceLocation> object, ResourceManager resourceManager, ProfilerFiller profiler) {
		ENTITY_ICONS.clear();
		ENTITY_ICONS.putAll(object);
	}
}
