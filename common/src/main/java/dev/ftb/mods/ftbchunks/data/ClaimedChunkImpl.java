package dev.ftb.mods.ftbchunks.data;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ClaimedChunkImpl implements ClaimedChunk {
	private ChunkTeamDataImpl teamData;
	private final ChunkDimPos pos;
	private long time;
	private long forceLoaded;
	private long forceLoadExpiryTime;

	public ClaimedChunkImpl(ChunkTeamDataImpl teamData, ChunkDimPos pos) {
		this.teamData = teamData;
		this.pos = pos;

		time = System.currentTimeMillis();
		forceLoaded = 0L;
		forceLoadExpiryTime = 0L;
	}

	@Override
	public ChunkTeamDataImpl getTeamData() {
		return teamData;
	}

	public void setTeamData(@NotNull ChunkTeamDataImpl teamData) {
		teamData.clearClaimCaches();
		this.teamData.clearClaimCaches();
		this.teamData = teamData;
	}

	@Override
	public ChunkDimPos getPos() {
		return pos;
	}

	@Override
	public long getTimeClaimed() {
		return time;
	}

	@Override
	public String getResultId() {
		return "ok";
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public MutableComponent getMessage() {
		return Component.literal("OK");
	}

	public void setClaimedTime(long t) {
		time = t;
		teamData.getManager().clearForceLoadedCache();
		teamData.clearClaimCaches();
		sendUpdateToAll();
	}

	@Override
	public long getForceLoadedTime() {
		return forceLoaded;
	}

	@Override
	public boolean isForceLoaded() {
		return forceLoaded > 0L;
	}

	@Override
	public boolean isActuallyForceLoaded() {
		return isForceLoaded() && teamData.canDoOfflineForceLoading();
	}

	public void setForceLoadedTime(long time) {
		if (forceLoaded == time) {
			return;
		}

		forceLoaded = time;
		teamData.getManager().clearForceLoadedCache();
		teamData.clearClaimCaches();
		sendUpdateToAll();

		ServerLevel level = teamData.getManager().getMinecraftServer().getLevel(pos.dimension());

		if (level != null) {
			if (forceLoaded > 0L) {
				level.getChunk(pos.x(), pos.z());
			}

			ServerChunkCache cache = level.getChunkSource();
			ChunkPos chunkPos = pos.chunkPos();

			if (cache != null) {
				FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, this.teamData.getTeamId(), chunkPos.x, chunkPos.z, forceLoaded > 0L);
				cache.save(false);
			} else {
				FTBChunks.LOGGER.warn("Failed to force-load chunk " + pos.x() + ", " + pos.z() + " @ " + pos.dimension().location() + "!");
			}
		}
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return teamData.canExplosionsDamageTerrain();
	}

	public boolean allowMobGriefing() {
		return teamData.allowMobGriefing();
	}

	public void sendUpdateToAll() {
		new SendChunkPacket(pos.dimension(), teamData.getTeamId(), ChunkSyncInfo.create(System.currentTimeMillis(), pos.x(), pos.z(), this))
				.sendToAll(teamData.getManager().getMinecraftServer(), teamData);
	}

	@Override
	public void unload(CommandSourceStack source) {
		if (isForceLoaded()) {
			setForceLoadedTime(0L);
			ClaimedChunkEvent.AFTER_UNLOAD.invoker().after(source, this);
			teamData.clearClaimCaches();
			teamData.markDirty();
			forceLoadExpiryTime = 0L;
		}
	}

	@Override
	public void unclaim(CommandSourceStack source, boolean sync) {
		unload(source);

		teamData.getManager().unregisterClaim(pos);
		ClaimedChunkEvent.AFTER_UNCLAIM.invoker().after(source, this);
		teamData.clearClaimCaches();
		teamData.markDirty();

		if (sync) {
			SendChunkPacket packet = new SendChunkPacket(pos.dimension(), Util.NIL_UUID, ChunkSyncInfo.create(System.currentTimeMillis(), pos.x(), pos.z(), null));
			NetworkManager.sendToPlayers(source.getServer().getPlayerList().getPlayers(), packet);
		}
	}

	@Override
	public long getForceLoadExpiryTime() {
		return forceLoadExpiryTime;
	}

	@Override
	public void setForceLoadExpiryTime(long forceLoadExpiryTime) {
		this.forceLoadExpiryTime = forceLoadExpiryTime;
		teamData.markDirty();
	}

	@Override
	public boolean hasForceLoadExpired(long now) {
		return forceLoadExpiryTime > 0L && forceLoadExpiryTime < now;
	}

	@Override
	public String toString() {
		return "[ " + pos.toString() + " - " + teamData + " ]";
	}

	public CompoundTag serializeNBT() {
		SNBTCompoundTag o = new SNBTCompoundTag();
		o.singleLine();
		o.putInt("x", getPos().x());
		o.putInt("z", getPos().z());
		o.putLong("time", getTimeClaimed());
		if (isForceLoaded()) {
			o.putLong("force_loaded", getForceLoadedTime());
		}
		if (getForceLoadExpiryTime() > 0L) {
			o.putLong("expiry_time", getForceLoadExpiryTime());
		}
		return o;
	}

	public static ClaimedChunkImpl deserializeNBT(ChunkTeamDataImpl data, ResourceKey<Level> dimKey, CompoundTag tag) {
		ClaimedChunkImpl chunk = new ClaimedChunkImpl(data, new ChunkDimPos(dimKey, tag.getInt("x"), tag.getInt("z")));
		chunk.time = tag.getLong("time");
		chunk.forceLoaded = tag.getLong("force_loaded");
		chunk.forceLoadExpiryTime = tag.getLong("expiry_time");
		return chunk;
	}

}
