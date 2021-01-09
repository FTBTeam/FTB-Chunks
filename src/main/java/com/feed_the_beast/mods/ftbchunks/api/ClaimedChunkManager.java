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
	UUID SERVER_PLAYER_ID = new UUID(0L, 0L);

	MinecraftServer getMinecraftServer();

	UUID getServerId();

	ClaimedChunkPlayerData getData(UUID id, String name);

	default ClaimedChunkPlayerData getData(ServerPlayerEntity player)
	{
		return getData(player.getUUID(), player.getGameProfile().getName());
	}

	default ClaimedChunkPlayerData getServerData()
	{
		return getData(SERVER_PLAYER_ID, "Server");
	}

	@Nullable
	ClaimedChunk getChunk(ChunkDimPos pos);

	Collection<ClaimedChunk> getAllClaimedChunks();
}