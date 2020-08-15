package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class UpdateChunkTask implements MapTask
{
	private final World world;
	private final XZ chunkPosition;

	public UpdateChunkTask(World w, XZ pos)
	{
		world = w;
		chunkPosition = pos;
	}

	@Override
	public void runMapTask()
	{
		List<ServerPlayerEntity> players = FTBChunksAPIImpl.manager.server.getPlayerList().getPlayers();

		if (players.isEmpty())
		{
			return;
		}

		String dimId = ChunkDimPos.getID(world);

		if (dimId.isEmpty())
		{
			return;
		}

		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = dimId;
		packet.x = chunkPosition.x;
		packet.z = chunkPosition.z;

		ClaimedChunkImpl claimedChunk = FTBChunksAPIImpl.manager.getChunk(chunkPosition.dim(dimId));

		if (claimedChunk != null)
		{
			long now = new Date().getTime();
			packet.owner = claimedChunk.getDisplayName();
			packet.color = 0xFF000000 | claimedChunk.getColor();
			packet.relativeTimeClaimed = now - Date.from(claimedChunk.getTimeClaimed()).getTime();
			packet.forceLoaded = claimedChunk.isForceLoaded();

			if (packet.forceLoaded)
			{
				packet.relativeTimeForceLoaded = now - Date.from(claimedChunk.getForceLoadedTime()).getTime();
			}
		}

		for (ServerPlayerEntity player : players)
		{
			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), packet);
		}
	}
}