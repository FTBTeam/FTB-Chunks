package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunkData;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.ClientUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
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
	}

	public static void openGui()
	{
		new ChunkScreen().openGui();
	}

	private void customClick(CustomClickEvent event)
	{
		if (event.getId().equals(BUTTON_ID))
		{
			openGui();
		}
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

	@Override
	public void openPlayerList(List<SendPlayerListPacket.NetPlayer> players)
	{
		new PlayerListScreen(players).openGui();
	}
}