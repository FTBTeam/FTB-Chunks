package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.ChunkSendingUtils;
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
	long expiryTime;

	public ClaimedChunk(FTBChunksTeamData p, ChunkDimPos cp) {
		teamData = p;
		pos = cp;
		time = System.currentTimeMillis();
		forceLoaded = 0L;
		expiryTime = 0L;
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
		teamData.manager.clearForceLoadedCache();
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
		teamData.manager.clearForceLoadedCache();
		sendUpdateToAll();

		ServerLevel level = teamData.manager.getMinecraftServer().getLevel(pos.dimension);

		if (level != null) {
			if (forceLoaded > 0L) {
				level.getChunk(pos.x, pos.z);
			}

			ServerChunkCache cache = level.getChunkSource();
			ChunkPos chunkPos = pos.getChunkPos();

			if (cache != null) {
				FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, this.teamData.getTeamId(), chunkPos.x, chunkPos.z, forceLoaded > 0L);
				cache.save(false);
			} else {
				FTBChunks.LOGGER.warn("Failed to force-load chunk " + pos.x + ", " + pos.z + " @ " + pos.dimension.location() + "!");
			}
		}
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return teamData.allowExplosions();
	}

	public void sendUpdateToAll() {
		SendChunkPacket packet = new SendChunkPacket(pos.dimension, teamData.getTeamId(), new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, this));
		ChunkSendingUtils.sendChunkToAll(teamData.manager.getMinecraftServer(), teamData, packet);
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

		teamData.manager.unregisterClaim(pos);
		ClaimedChunkEvent.AFTER_UNCLAIM.invoker().after(source, this);
		teamData.save();

		if (sync) {
			SendChunkPacket packet = new SendChunkPacket(pos.dimension, Util.NIL_UUID, new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, null));
			packet.sendToAll(source.getServer());
		}
	}

	public boolean hasExpired(long now) {
		return expiryTime > 0L && expiryTime < now;
	}

	public long getExpiryTime() {
		return expiryTime;
	}

	@Override
	public String toString() {
		return "[ " + pos.toString() + " - " + teamData + " ]";
	}
}
