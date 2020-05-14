package com.feed_the_beast.mods.ftbchunks.client;

import net.minecraft.client.resources.ReloadListener;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class EntityIcons extends ReloadListener<Map<EntityType<?>, ResourceLocation>>
{
	public static final ResourceLocation INVISIBLE = new ResourceLocation("ftbchunks:textures/faces/invisible.png");
	public static final ResourceLocation NORMAL = new ResourceLocation("ftbchunks:textures/faces/normal.png");
	public static final ResourceLocation HOSTILE = new ResourceLocation("ftbchunks:textures/faces/hostile.png");
	public static final Map<EntityType<?>, ResourceLocation> ENTITY_ICONS = new HashMap<>();

	@Override
	protected Map<EntityType<?>, ResourceLocation> prepare(IResourceManager resourceManager, IProfiler profiler)
	{
		Map<EntityType<?>, ResourceLocation> map = new HashMap<>();

		for (EntityType<?> t : ForgeRegistries.ENTITIES)
		{
			if (t.getClassification() == EntityClassification.MISC)
			{
				continue;
			}

			ResourceLocation invisible = new ResourceLocation("ftbchunks:textures/faces/" + t.getRegistryName().getNamespace() + "/" + t.getRegistryName().getPath() + ".invisible");

			if (resourceManager.hasResource(invisible))
			{
				map.put(t, INVISIBLE);
				continue;
			}

			ResourceLocation texture = new ResourceLocation("ftbchunks:textures/faces/" + t.getRegistryName().getNamespace() + "/" + t.getRegistryName().getPath() + ".png");

			if (resourceManager.hasResource(texture))
			{
				map.put(t, texture);
			}
		}

		return map;
	}

	@Override
	protected void apply(Map<EntityType<?>, ResourceLocation> object, IResourceManager resourceManager, IProfiler profiler)
	{
		ENTITY_ICONS.clear();
		ENTITY_ICONS.putAll(object);
	}
}
