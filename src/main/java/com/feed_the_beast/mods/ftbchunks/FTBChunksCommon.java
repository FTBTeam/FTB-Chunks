package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.net.SendGeneralData;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;

import java.util.List;

/**
 * @author LatvianModder
 */
public class FTBChunksCommon
{
	public void init()
	{
	}

	public void updateGeneralData(SendGeneralData data)
	{
	}

	public void updateChunk(int chunkX, int chunkZ, byte[] imageData)
	{
	}

	public void openPlayerList(List<SendPlayerListPacket.NetPlayer> players, int allyMode)
	{
	}
}