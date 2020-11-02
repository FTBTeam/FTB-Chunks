package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColorProvider;
import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColors;
import com.feed_the_beast.mods.ftbchunks.client.map.color.CustomBlockColor;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.extensions.IAbstractRailBlock;
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

		for (String namespace : resourceManager.getResourceNamespaces())
		{
			try
			{
				for (IResource resource : resourceManager.getAllResources(new ResourceLocation(namespace, "ftbchunks_block_colors.json")))
				{
					try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
					{
						for (Map.Entry<String, JsonElement> entry : gson.fromJson(reader, JsonObject.class).entrySet())
						{
							if (entry.getKey().startsWith("#"))
							{
								object.add("#" + namespace + ":" + entry.getKey().substring(1), entry.getValue());
							}
							else
							{
								object.add(namespace + ":" + entry.getKey(), entry.getValue());
							}
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
			}
		}

		return object;
	}

	@Override
	protected void apply(JsonObject object, IResourceManager resourceManager, IProfiler profiler)
	{
		for (Block block : ForgeRegistries.BLOCKS)
		{
			if (block instanceof BlockColorProvider)
			{
				BlockColorProvider p = (BlockColorProvider) block;
				p.setFTBCBlockColor(BlockColors.DEFAULT);

				if (block instanceof AirBlock
						|| block instanceof BushBlock
						|| block instanceof FireBlock
						|| block instanceof AbstractButtonBlock
						|| block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock)
				)
				{
					p.setFTBCBlockColor(BlockColors.IGNORED);
				}
				else if (block instanceof GrassBlock)
				{
					p.setFTBCBlockColor(BlockColors.GRASS);
				}
				else if (block instanceof LeavesBlock || block instanceof VineBlock)
				{
					p.setFTBCBlockColor(BlockColors.FOLIAGE);
				}
				else if (block instanceof FlowerPotBlock)
				{
					p.setFTBCBlockColor(new CustomBlockColor(Color4I.rgb(0x683A2D)));
				}
				else if (block instanceof IAbstractRailBlock)
				{
					p.setFTBCBlockColor(new CustomBlockColor(Color4I.rgb(0x888888)));
				}
			}
		}

		// Fire event Pre

		for (Map.Entry<String, JsonElement> entry : object.entrySet())
		{
			if (entry.getValue().isJsonPrimitive())
			{
				Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey()));

				if (block != Blocks.AIR && block instanceof BlockColorProvider)
				{
					((BlockColorProvider) block).setFTBCBlockColor(BlockColors.getFromType(entry.getValue().getAsString()));
				}
			}
		}

		// Fire event Post
	}
}