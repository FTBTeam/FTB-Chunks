package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ColorMapLoader extends ReloadListener<JsonObject>
{
	@Override
	protected JsonObject prepare(IResourceManager resourceManager, IProfiler profiler)
	{
		Gson gson = new GsonBuilder().setLenient().create();
		JsonObject object = new JsonObject();

		try
		{
			for (IResource resource : resourceManager.getAllResources(new ResourceLocation("ftbchunks", "ftbchunks_colors.json")))
			{
				try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
				{
					for (Map.Entry<String, JsonElement> entry : gson.fromJson(reader, JsonObject.class).entrySet())
					{
						object.add(entry.getKey(), entry.getValue());
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return object;
	}

	@Override
	protected void apply(JsonObject object, IResourceManager resourceManager, IProfiler profiler)
	{
		FTBChunksAPIImpl.COLOR_MAP.clear();

		for (Map.Entry<String, JsonElement> entry : object.entrySet())
		{
			if (entry.getValue().isJsonPrimitive())
			{
				Color4I color = Color4I.fromJson(entry.getValue());

				if (color.isEmpty())
				{
					continue;
				}

				if (entry.getKey().startsWith("#"))
				{
					ITag<Block> tag = BlockTags.getCollection().get(new ResourceLocation(entry.getKey().substring(1)));

					if (tag != null)
					{
						for (Block block : tag.getAllElements())
						{
							FTBChunksAPIImpl.COLOR_MAP.put(block, color);
						}
					}
				}
				else
				{
					Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey()));

					if (block != Blocks.AIR)
					{
						FTBChunksAPIImpl.COLOR_MAP.put(block, color);
					}
				}
			}
		}
	}
}