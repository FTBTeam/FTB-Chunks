package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkManager
{
	UUID SERVER_ID = new UUID(0L, 0L);

	MinecraftServer getMinecraftServer();

	ClaimedChunkPlayerData getData(UUID id, String name);

	default ClaimedChunkPlayerData getData(ServerPlayerEntity player)
	{
		return getData(player.getUniqueID(), player.getGameProfile().getName());
	}

	default ClaimedChunkPlayerData getServerData()
	{
		return getData(SERVER_ID, "Server");
	}

	@Nullable
	ClaimedChunk getChunk(ChunkDimPos pos);

	Collection<ClaimedChunk> getAllClaimedChunks();
}