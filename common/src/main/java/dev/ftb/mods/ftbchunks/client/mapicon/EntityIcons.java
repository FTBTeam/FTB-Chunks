package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class EntityIcons extends SimplePreparableReloadListener<Map<EntityType<?>, Icon>> {
	public static final Icon NORMAL = Icon.getIcon("ftbchunks:textures/faces/normal.png");
	public static final Icon HOSTILE = Icon.getIcon("ftbchunks:textures/faces/hostile.png");
	public static final Map<EntityType<?>, Icon> ENTITY_ICONS = new HashMap<>();

	@Override
	protected Map<EntityType<?>, Icon> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<EntityType<?>, Icon> map = new HashMap<>();

		for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : RegistrarManager.get(FTBChunks.MOD_ID).get(Registries.ENTITY_TYPE).entrySet()) {
			ResourceLocation id = entry.getKey().location();
			EntityType<?> t = entry.getValue();
			if (t.getCategory() == MobCategory.MISC) {
				continue;
			}

			ResourceLocation invisible = new ResourceLocation("ftbchunks:textures/faces/" + id.getNamespace() + "/" + id.getPath() + ".invisible");

			if (resourceManager.getResource(invisible).isPresent()) {
				map.put(t, Color4I.empty());
				continue;
			}

			ResourceLocation texture = new ResourceLocation("ftbchunks:textures/faces/" + id.getNamespace() + "/" + id.getPath() + ".png");

			if (resourceManager.getResource(texture).isPresent()) {
				map.put(t, Icon.getIcon(texture));
			}
		}

		return map;
	}

	@Override
	protected void apply(Map<EntityType<?>, Icon> object, ResourceManager resourceManager, ProfilerFiller profiler) {
		ENTITY_ICONS.clear();
		ENTITY_ICONS.putAll(object);

		Minecraft.getInstance().submit(() -> {
			for (Icon icon : object.values()) {
				if (icon instanceof ImageIcon) {
					((ImageIcon) icon).bindTexture();
					RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
					RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				}
			}
		});
	}

	public static Icon get(Entity entity) {
		Icon texture = EntityIcons.ENTITY_ICONS.get(entity.getType());

		if (texture == null || !FTBChunksClientConfig.MINIMAP_ENTITY_HEADS.get()) {
			if (entity instanceof Enemy) {
				return EntityIcons.HOSTILE;
			} else {
				return EntityIcons.NORMAL;
			}
		}

		return texture;
	}
}
