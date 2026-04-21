package dev.ftb.mods.ftbchunks.data;

import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbchunks.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NonNull;

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

	public void setTeamData(@NonNull ChunkTeamDataImpl teamData) {
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

            ChunkPos chunkPos = pos.chunkPos();
            FTBChunksAPIImpl.INSTANCE.getForceLoadHandler()
                    .updateForceLoadingForChunk(level, this.teamData.getTeamId(), chunkPos.x(), chunkPos.z(), forceLoaded > 0L);
            level.getChunkSource().save(false);
        }
	}

	public boolean canEntitySpawn(@UnknownNullability EntityType<?> entity) {
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
			NativeEventPosting.get().postEvent(new ChunkChangeEvent.Post.Data(source, this, ChunkChangeEvent.Operation.UNLOAD));
			teamData.clearClaimCaches();
			teamData.markDirty();
			forceLoadExpiryTime = 0L;
		}
	}

	@Override
	public void unclaim(CommandSourceStack source, boolean sync) {
		unload(source);

		teamData.getManager().unregisterClaim(pos);
		NativeEventPosting.get().postEvent(new ChunkChangeEvent.Post.Data(source, this, ChunkChangeEvent.Operation.UNCLAIM));
		teamData.clearClaimCaches();
		teamData.markDirty();

		if (sync) {
			SendChunkPacket packet = new SendChunkPacket(pos.dimension(), Util.NIL_UUID, ChunkSyncInfo.create(System.currentTimeMillis(), pos.x(), pos.z(), null));
			Server2PlayNetworking.sendToAllPlayers(source.getServer(), packet);
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

	public Json5Object toJson() {
		Json5Object o = new Json5Object();
//		o.singleLine();
		o.addProperty("x", getPos().x());
		o.addProperty("z", getPos().z());
		o.addProperty("time", getTimeClaimed());
		if (isForceLoaded()) {
			o.addProperty("force_loaded", getForceLoadedTime());
		}
		if (getForceLoadExpiryTime() > 0L) {
			o.addProperty("expiry_time", getForceLoadExpiryTime());
		}
		return o;
	}

	public static ClaimedChunkImpl fromJson(ChunkTeamDataImpl data, ResourceKey<Level> dimKey, Json5Object tag) {
		ClaimedChunkImpl chunk = new ClaimedChunkImpl(data, new ChunkDimPos(dimKey, tag.get("x").getAsInt(), tag.get("z").getAsInt()));
		chunk.time = Json5Util.getLong(tag, "time").orElse(System.currentTimeMillis());
		chunk.forceLoaded = Json5Util.getLong(tag, "force_loaded").orElse(0L);
		chunk.forceLoadExpiryTime = Json5Util.getLong(tag, "expiry_time").orElse(0L);
		return chunk;
	}
}
