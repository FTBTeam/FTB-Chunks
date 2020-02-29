package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkGroup;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author LatvianModder
 */
public class ClaimedChunkImpl implements ClaimedChunk
{
	public final ClaimedChunkPlayerDataImpl playerData;
	public final ChunkDimPos pos;
	public boolean forceLoaded;
	public ClaimedChunkGroup group;
	public Instant time;

	public ClaimedChunkImpl(ClaimedChunkPlayerDataImpl p, ChunkDimPos cp)
	{
		playerData = p;
		pos = cp;
		forceLoaded = false;
		time = Instant.now();
	}

	@Override
	public ClaimedChunkPlayerDataImpl getPlayerData()
	{
		return playerData;
	}

	@Override
	public ChunkDimPos getPos()
	{
		return pos;
	}

	@Override
	public boolean isForceLoaded()
	{
		return forceLoaded;
	}

	@Nullable
	@Override
	public ClaimedChunkGroup getGroup()
	{
		return group;
	}

	@Override
	public Instant getTime()
	{
		return time;
	}

	private boolean isAlly(ServerPlayerEntity player)
	{
		return playerData.getUuid().equals(player.getUniqueID()) || playerData.getName().equals(player.getGameProfile().getName()) || player.getServer().isSinglePlayer() || player.hasPermissionLevel(2);
	}

	@Override
	public boolean canEdit(ServerPlayerEntity player, BlockState blockState)
	{
		return isAlly(player);
	}

	@Override
	public boolean canInteract(ServerPlayerEntity player, BlockState blockState)
	{
		return isAlly(player);
	}

	@Override
	public boolean canEntitySpawn(Entity entity)
	{
		return true;
	}

	@Override
	public boolean allowExplosions()
	{
		return true;
	}
}