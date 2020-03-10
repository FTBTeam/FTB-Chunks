package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunkData;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.ClientUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon
{
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");
	public static final Map<Block, Color4I> COLOR_MAP = new HashMap<>();

	public void init()
	{
		MinecraftForge.EVENT_BUS.addListener(this::customClick);
		MinecraftForge.EVENT_BUS.addListener(this::loggedOut);
		MinecraftForge.EVENT_BUS.addListener(this::respawned);
	}

	@Override
	public void setMapData(NetClaimedChunkData data)
	{
		ChunkScreen screen = ClientUtils.getCurrentGuiAs(ChunkScreen.class);

		if (screen != null)
		{
			screen.setData(data);
		}
	}

	@Override
	public void setColorMap(Map<ResourceLocation, Integer> m)
	{
		COLOR_MAP.clear();

		for (Map.Entry<ResourceLocation, Integer> entry : m.entrySet())
		{
			Block b = ForgeRegistries.BLOCKS.getValue(entry.getKey());

			if (b != null && b != Blocks.AIR)
			{
				COLOR_MAP.put(b, Color4I.rgb(entry.getValue()));
			}
		}
	}

	private void customClick(CustomClickEvent event)
	{
		if (event.getId().equals(BUTTON_ID))
		{
			new ChunkScreen().openGui();
		}
	}

	private void loggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
	}

	private void respawned(ClientPlayerNetworkEvent.RespawnEvent event)
	{
	}
}