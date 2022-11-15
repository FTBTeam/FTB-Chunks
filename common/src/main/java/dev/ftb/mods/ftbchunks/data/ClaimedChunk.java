package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * @author LatvianModder
 */
public class ClaimedChunk implements ClaimResult {
	public FTBChunksTeamData teamData;
	public final ChunkDimPos pos;
	public long forceLoaded;
	public long time;

	public ClaimedChunk(FTBChunksTeamData p, ChunkDimPos cp) {
		teamData = p;
		pos = cp;
		forceLoaded = 0L;
		time = System.currentTimeMillis();
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
		sendUpdateToAll();
	}

	public long getForceLoadedTime() {
		return forceLoaded;
	}

	public boolean isForceLoaded() {
		return forceLoaded > 0L;
	}

	@Override
	public void setForceLoadedTime(long time) {
		forceLoaded = time;
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return teamData.allowExplosions();
	}

	public void postSetForceLoaded(boolean load) {
		ServerLevel world = getTeamData().getManager().getMinecraftServer().getLevel(getPos().dimension);

		if (world != null) {
			boolean changed = world.setChunkForced(getPos().x, getPos().z, load);
			FTBChunks.LOGGER.debug("set chunk {},{} forced={} change_made={}", getPos().x, getPos().z, load, changed);
			if (changed) sendUpdateToAll();
		}
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
			postSetForceLoaded(false);
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
