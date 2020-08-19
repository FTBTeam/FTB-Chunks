package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.PlayerDeathPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendAllChunksPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import net.minecraft.world.server.ServerWorld;

/**
 * @author LatvianModder
 */
public class FTBChunksCommon
{
	public void init()
	{
	}

	public void login(LoginDataPacket loginData)
	{
	}

	public void updateGeneralData(SendGeneralDataPacket packet)
	{
	}

	public void updateChunk(SendChunkPacket packet)
	{
	}

	public void updateAllChunks(SendAllChunksPacket packet)
	{
	}

	public void openPlayerList(SendPlayerListPacket packet)
	{
	}

	public void updateVisiblePlayerList(SendVisiblePlayerListPacket packet)
	{
	}

	public void importWorldMap(ServerWorld world)
	{
	}

	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data)
	{
	}

	public void playerDeath(PlayerDeathPacket packet)
	{
	}
}