package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksClient
{
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");

	public FTBChunksClient()
	{
		MinecraftForge.EVENT_BUS.addListener(this::customClick);
		MinecraftForge.EVENT_BUS.addListener(this::loggedOut);
		MinecraftForge.EVENT_BUS.addListener(this::respawned);
	}

	private void customClick(CustomClickEvent event)
	{
		if (MinimapData.instance == null)
		{
			MinimapData.instance = new MinimapData(new UUID(0L, 0L));
		}

		if (event.getId().equals(BUTTON_ID))
		{
			new GuiClaimedChunks(MinimapData.instance).openGui();
		}
	}

	private void loggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
		if (MinimapData.instance != null)
		{
			MinimapData.instance.releaseData();
		}
	}

	private void respawned(ClientPlayerNetworkEvent.RespawnEvent event)
	{
		if (MinimapData.instance != null)
		{
			MinimapData.instance.releaseData();
		}
	}
}