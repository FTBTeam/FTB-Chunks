package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Comparator;

/**
 * @author LatvianModder
 */
public class ClaimedChunkImpl implements ClaimedChunk
{
	public static TicketType<ChunkPos> TICKET_TYPE = TicketType.create("ftbchunks", Comparator.comparingLong(ChunkPos::asLong));
	public static final int MAGIC_NUMBER = 2;

	public final ClaimedChunkPlayerDataImpl playerData;
	public final ChunkDimPos pos;
	public Instant forceLoaded;
	public ClaimedChunkGroupImpl group;
	public Instant time;

	public ClaimedChunkImpl(ClaimedChunkPlayerDataImpl p, ChunkDimPos cp)
	{
		playerData = p;
		pos = cp;
		forceLoaded = null;
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

	@Nullable
	@Override
	public ClaimedChunkGroupImpl getGroup()
	{
		return group;
	}

	@Override
	public Instant getTimeClaimed()
	{
		return time;
	}

	@Override
	public void setClaimedTime(Instant t)
	{
		time = t;
	}

	@Override
	@Nullable
	public Instant getForceLoadedTime()
	{
		return forceLoaded;
	}

	@Override
	public void setForceLoadedTime(@Nullable Instant time)
	{
		forceLoaded = time;
	}

	private boolean isAlly(ServerPlayerEntity player)
	{
		return player.getServer().isSinglePlayer() || player.hasPermissionLevel(2) || playerData.isAlly(player);
	}

	@Override
	public boolean canEdit(ServerPlayerEntity player, BlockState blockState)
	{
		return isAlly(player) || FTBChunksAPIImpl.EDIT_TAG.contains(blockState.getBlock());
	}

	@Override
	public boolean canInteract(ServerPlayerEntity player, BlockState blockState)
	{
		return isAlly(player) || FTBChunksAPIImpl.INTERACT_TAG.contains(blockState.getBlock());
	}

	@Override
	public boolean canEntitySpawn(Entity entity)
	{
		return true;
	}

	@Override
	public boolean allowExplosions()
	{
		return false;
	}

	public void postSetForceLoaded(boolean load)
	{
		ServerWorld world = ChunkDimPos.getWorld(getPlayerData().getManager().getMinecraftServer(), getPos().dimension);

		if (world != null)
		{
			world.forceChunk(getPos().x, getPos().z, load);
		}
	}
}