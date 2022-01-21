package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.core.ChunkLoadingHelper;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/**
 * @author LatvianModder
 */
public class ClaimedChunk implements ClaimResult {
	public FTBChunksTeamData teamData;
	public final ChunkDimPos pos;
	public long time;
	public long forceLoaded;

	public ClaimedChunk(FTBChunksTeamData p, ChunkDimPos cp) {
		teamData = p;
		pos = cp;
		time = System.currentTimeMillis();
		forceLoaded = 0L;
	}

	public FTBChunksTeamData getTeamData() {
		return teamData;
	}

	public ChunkDimPos getPos() {
		return pos;
	}

	public long getTimeClaimed() {
		return time;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public void setClaimedTime(long t) {
		time = t;
		teamData.manager.updateForceLoadedChunks();
		sendUpdateToAll();
	}

	public long getForceLoadedTime() {
		return forceLoaded;
	}

	public boolean isForceLoaded() {
		return forceLoaded > 0L;
	}

	public boolean isActuallyForceLoaded() {
		return isForceLoaded() && teamData.canForceLoadChunks();
	}

	@Override
	public void setForceLoadedTime(long time) {
		forceLoaded = time;
		teamData.manager.updateForceLoadedChunks();
		sendUpdateToAll();

		ServerLevel level = teamData.manager.getMinecraftServer().getLevel(pos.dimension);

		if (level != null) {
			if (forceLoaded > 0L) {
				level.getChunk(pos.x, pos.z);
			}

			ServerChunkCache cache = level.getChunkSource();
			ChunkPos chunkPos = pos.getChunkPos();

			if (cache != null) {
				if (forceLoaded > 0L) {
					cache.addRegionTicket(ChunkLoadingHelper.FTBCHUNKS_FORCE_LOADED, chunkPos, 2, chunkPos);
				} else {
					cache.removeRegionTicket(ChunkLoadingHelper.FTBCHUNKS_FORCE_LOADED, chunkPos, 2, chunkPos);
				}
			} else {
				FTBChunks.LOGGER.warn("Failed to force-load chunk " + pos.x + ", " + pos.z + " @ " + pos.dimension.location() + "!");
			}
		}
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return false;
	}

	public void sendUpdateToAll() {
		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.teamId = teamData.getTeamId();
		packet.chunk = new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, this);
		packet.sendToAll(teamData.manager.getMinecraftServer());
	}

	public void unload(CommandSourceStack source) {
		if (isForceLoaded()) {
			setForceLoadedTime(0L);
			ClaimedChunkEvent.AFTER_UNLOAD.invoker().after(source, this);
			teamData.save();
		}
	}

	public void unclaim(CommandSourceStack source, boolean sync) {
		unload(source);

		teamData.manager.claimedChunks.remove(pos);
		ClaimedChunkEvent.AFTER_UNCLAIM.invoker().after(source, this);
		teamData.save();

		if (sync) {
			SendChunkPacket packet = new SendChunkPacket();
			packet.dimension = pos.dimension;
			packet.teamId = Util.NIL_UUID;
			packet.chunk = new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, null);
			packet.sendToAll(source.getServer());
		}
	}
}