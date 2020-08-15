package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendWaypointsPacket;
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

	public void updateWaypoints(SendWaypointsPacket packet)
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
}