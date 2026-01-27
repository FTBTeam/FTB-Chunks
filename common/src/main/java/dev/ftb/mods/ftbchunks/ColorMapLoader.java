package dev.ftb.mods.ftbchunks;

import com.google.gson.*;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.client.map.color.CustomBlockColor;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ColorMapLoader extends SimplePreparableReloadListener<JsonObject> {
	private static final Map<Identifier, BlockColor> BLOCK_ID_TO_COLOR_MAP = new HashMap<>();

	@Override
	protected JsonObject prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
		JsonObject object = new JsonObject();

		for (String namespace : resourceManager.getNamespaces()) {
			try {
				for (Resource resource : resourceManager.getResourceStack(Identifier.fromNamespaceAndPath(namespace, "ftbchunks_block_colors.json"))) {
					try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
						for (Map.Entry<String, JsonElement> entry : gson.fromJson(reader, JsonObject.class).entrySet()) {
							if (entry.getKey().startsWith("#")) {
								object.add("#" + namespace + ":" + entry.getKey().substring(1), entry.getValue());
							} else {
								object.add(namespace + ":" + entry.getKey(), entry.getValue());
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
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

		for (Map.Entry<ResourceKey<Block>, Block> entry : FTBChunks.BLOCK_REGISTRY.entrySet()) {
			Block block = entry.getValue();
			Identifier id = entry.getKey().identifier();

            if (block instanceof AirBlock
                    || block instanceof BushBlock
                    || block instanceof FireBlock
                    || block instanceof ButtonBlock
                    || block instanceof TorchBlock
                    || block instanceof StainedGlassPaneBlock
            ) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.IGNORED);
            } else if (block instanceof GrassBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.GRASS);
            } else if (block instanceof LeavesBlock || block instanceof VineBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, BlockColors.FOLIAGE);
            } else if (block instanceof FlowerPotBlock) {
                BLOCK_ID_TO_COLOR_MAP.put(id, new CustomBlockColor(Color4I.rgb(0x683A2D)));
            } else if (FTBCUtils.isRail(block)) {
                BLOCK_ID_TO_COLOR_MAP.put(id, new CustomBlockColor(Color4I.rgb(0x888888)));
            } else if (block.defaultMapColor() != null) {
                BLOCK_ID_TO_COLOR_MAP.put(id, new CustomBlockColor(Color4I.rgb(block.defaultMapColor().col)));
            } else {
                BLOCK_ID_TO_COLOR_MAP.put(id, new CustomBlockColor(Color4I.RED));
            }
        }

		// Fire event Pre

		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			if (entry.getValue().isJsonPrimitive()) {
				BlockColor col = BlockColors.getFromType(entry.getValue().getAsString());

				if (col != null) {
					BLOCK_ID_TO_COLOR_MAP.put(Identifier.tryParse(entry.getKey()), col);
				}
			}
		}

		// Fire event Post
	}

	public static BlockColor getBlockColor(Identifier id) {
		return BLOCK_ID_TO_COLOR_MAP.getOrDefault(id, BlockColors.IGNORED);
	}
}
