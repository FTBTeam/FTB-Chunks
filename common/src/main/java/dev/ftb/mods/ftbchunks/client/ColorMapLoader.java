package dev.ftb.mods.ftbchunks.client;

import com.google.gson.*;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.client.map.color.CustomBlockColor;
import dev.ftb.mods.ftblibrary.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.MapColor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ColorMapLoader extends SimplePreparableReloadListener<JsonObject> {
	private static final Map<Identifier, BlockColor> BLOCK_ID_TO_COLOR_MAP = new HashMap<>();
	private static final String BLOCK_COLOR_FILE = "ftbchunks_block_colors.json";

	@Override
	protected JsonObject prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
		JsonObject object = new JsonObject();

		for (String namespace : resourceManager.getNamespaces()) {
			try {
				for (Resource resource : resourceManager.getResourceStack(Identifier.fromNamespaceAndPath(namespace, BLOCK_COLOR_FILE))) {
					try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
						for (Map.Entry<String, JsonElement> entry : gson.fromJson(reader, JsonObject.class).entrySet()) {
							if (entry.getKey().startsWith("#")) {
								object.add("#" + namespace + ":" + entry.getKey().substring(1), entry.getValue());
							} else {
								object.add(namespace + ":" + entry.getKey(), entry.getValue());
							}
						}
					} catch (IOException ex) {
						FTBChunks.LOGGER.error("can't load {}: {} / {}", BLOCK_COLOR_FILE, ex.getClass(), ex.getMessage());
					}
				}
			} catch (Exception ignored) {
			}
		}

		return object;
	}

	@Override
	protected void apply(JsonObject object, ResourceManager resourceManager, ProfilerFiller profiler) {
		BLOCK_ID_TO_COLOR_MAP.clear();

		for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
			Block block = entry.getValue();
			Identifier id = entry.getKey().identifier();

            if (block instanceof AirBlock || block instanceof FireBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.IGNORED);
            } else if (block instanceof GrassBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.GRASS);
            } else if (block instanceof LeavesBlock || block instanceof VineBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.FOLIAGE);
            } else if (block instanceof FlowerPotBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, CustomBlockColor.FLOWER_POT);
            } else if (Platform.get().misc().isRailBlock(block)) {
                BLOCK_ID_TO_COLOR_MAP.put(id, CustomBlockColor.RAIL);
            } else if (block.defaultMapColor() != MapColor.NONE) {
                BLOCK_ID_TO_COLOR_MAP.put(id, CustomBlockColor.ofMapColor(block.defaultMapColor()));
            }
        }

		object.asMap().forEach((blockIdStr, colorId) -> {
			if (colorId.isJsonPrimitive()) {
				BlockColor col = BlockColors.getFromType(colorId.getAsString());
				if (col != null) {
					Identifier blockId = Identifier.tryParse(blockIdStr);
					if (blockId != null) {
						BLOCK_ID_TO_COLOR_MAP.put(blockId, col);
					} else {
						FTBChunks.LOGGER.error("Bad block ID {} in block colors", blockIdStr);
					}
				} else {
					FTBChunks.LOGGER.error("Unknown color type {} -> {} in block colors", blockIdStr, colorId.getAsString());
				}
			} else {
				FTBChunks.LOGGER.error("Bad Json element for {} in block colors (expected primitive)", blockIdStr);
			}
		});

		FTBChunks.LOGGER.debug("Loaded {} color entries from {} resource pack files", BLOCK_ID_TO_COLOR_MAP.size(), BLOCK_COLOR_FILE);
	}

	public static BlockColor getBlockColor(Identifier id) {
		return BLOCK_ID_TO_COLOR_MAP.getOrDefault(id, BlockColors.IGNORED);
	}
}
