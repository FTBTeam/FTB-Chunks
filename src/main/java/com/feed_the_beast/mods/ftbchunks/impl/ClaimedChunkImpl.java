package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;

/**
 * @author LatvianModder
 */
public class ClaimedChunkImpl implements ClaimedChunk
{
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
		sendUpdateToAll();
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

	@Override
	public boolean canEdit(ServerPlayerEntity player, BlockState state)
	{
		return FTBChunksAPIImpl.EDIT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockEditMode, false) || player.hasPermissionLevel(2);
	}

	@Override
	public boolean canInteract(ServerPlayerEntity player, BlockState state)
	{
		return FTBChunksAPIImpl.INTERACT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockInteractMode, false) || player.hasPermissionLevel(2);
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
		ServerWorld world = getPlayerData().getManager().getMinecraftServer().getWorld(getPos().dimension);

		if (world != null)
		{
			world.forceChunk(getPos().x, getPos().z, load);
			sendUpdateToAll();
		}
	}

	public void sendUpdateToAll()
	{
		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.owner = playerData.getUuid();
		packet.chunk = new SendChunkPacket.SingleChunk(new Date(), pos.x, pos.z, this);
		FTBChunksNet.MAIN.send(PacketDistributor.ALL.noArg(), packet);
	}
}