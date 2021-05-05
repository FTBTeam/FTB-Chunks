package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.OrderedCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PrivacyMode;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.property.BooleanProperty;
import dev.ftb.mods.ftbteams.property.PrivacyProperty;
import me.shedaniel.architectury.hooks.PlayerHooks;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksTeamData {
	public static final BooleanProperty ALLOW_FAKE_PLAYERS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players"), true);
	public static final PrivacyProperty BLOCK_EDIT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty BLOCK_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_interact_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty MINIMAP_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "minimap_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty LOCATION_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "location_mode"), PrivacyMode.ALLIES);

	public final ClaimedChunkManager manager;
	private final Team team;
	public final Path file;
	private boolean shouldSave;
	public int maxClaimChunks;
	public int maxForceLoadChunks;
	public int extraClaimChunks;
	public int extraForceLoadChunks;
	public boolean chunkLoadOffline;
	public boolean bypassProtection;

	public int prevChunkX = Integer.MAX_VALUE, prevChunkZ = Integer.MAX_VALUE;
	public String lastChunkID = "";

	public FTBChunksTeamData(ClaimedChunkManager m, Path f, Team t) {
		manager = m;
		team = t;
		file = f;
		shouldSave = false;
		maxClaimChunks = -1;
		maxForceLoadChunks = -1;
		extraClaimChunks = 0;
		extraForceLoadChunks = 0;
		chunkLoadOffline = true;
		bypassProtection = false;
	}

	@Override
	public String toString() {
		return team.getStringID();
	}

	public ClaimedChunkManager getManager() {
		return manager;
	}

	public Team getTeam() {
		return team;
	}

	public UUID getTeamId() {
		return team.getId();
	}

	public Collection<ClaimedChunk> getClaimedChunks() {
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunk chunk : manager.claimedChunks.values()) {
			if (chunk.teamData == this) {
				list.add(chunk);
			}
		}

		return list;
	}

	public Collection<ClaimedChunk> getForceLoadedChunks() {
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunk chunk : manager.claimedChunks.values()) {
			if (chunk.teamData == this && chunk.isForceLoaded()) {
				list.add(chunk);
			}
		}

		return list;
	}

	public void updateLimits(ServerPlayer ownerPlayer) {
		if (maxClaimChunks != -1 && team.getType().isParty() && !ownerPlayer.getUUID().equals(team.getOwner())) {
			return;
		}

		int c = manager.config.getMaxClaimedChunks(this, ownerPlayer);
		int f = manager.config.getMaxForceLoadedChunks(this, ownerPlayer);

		if (maxClaimChunks != c || maxForceLoadChunks != f) {
			maxClaimChunks = c;
			maxForceLoadChunks = f;

			for (ServerPlayer p : team.getOnlineMembers()) {
				SendGeneralDataPacket.send(this, p);
			}

			save();
		}
	}

	public ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk != null) {
			return ClaimResults.ALREADY_CLAIMED;
		} else if (manager.config.claimDimensionBlacklistSet.contains(pos.dimension)) {
			return ClaimResults.DIMENSION_FORBIDDEN;
		} else if (!team.getType().isServer() && getClaimedChunks().size() >= maxClaimChunks) {
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		chunk = new ClaimedChunk(this, pos);

		ClaimResult r = ClaimedChunkEvent.BEFORE_CLAIM.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		manager.claimedChunks.put(pos, chunk);
		ClaimedChunkEvent.AFTER_CLAIM.invoker().after(source, chunk);
		save();
		return chunk;
	}

	public ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.teamData != this && !source.hasPermission(2) && !source.getServer().isSingleplayer()) {
			return ClaimResults.NOT_OWNER;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_UNCLAIM.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		chunk.unclaim(source, true);
		return chunk;
	}

	public ClaimResult load(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.teamData != this && !source.hasPermission(2) && !source.getServer().isSingleplayer()) {
			return ClaimResults.NOT_OWNER;
		} else if (chunk.isForceLoaded()) {
			return ClaimResults.ALREADY_LOADED;
		} else if (!team.getType().isServer() && getForceLoadedChunks().size() >= maxForceLoadChunks) {
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_LOAD.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		chunk.setForceLoadedTime(System.currentTimeMillis());
		chunk.postSetForceLoaded(true);
		ClaimedChunkEvent.AFTER_LOAD.invoker().after(source, chunk);
		chunk.teamData.save();
		return chunk;
	}

	public ClaimResult unload(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.teamData != this
				&& !source.hasPermission(2)
				&& !source.getServer().isSingleplayer()
				&& !(source.getEntity() instanceof ServerPlayer && isTeamMember(source.getEntity().getUUID()))
		) {
			return ClaimResults.NOT_OWNER;
		} else if (!chunk.isForceLoaded()) {
			return ClaimResults.NOT_LOADED;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_UNLOAD.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		chunk.unload(source);
		return chunk;
	}

	public void save() {
		shouldSave = true;
		team.save();
	}

	public boolean isTeamMember(UUID p) {
		if (p.equals(getTeamId())) {
			return true;
		}

		return team.equals(FTBTeamsAPI.getManager().getPlayerTeam(p));
	}

	public boolean isAlly(UUID p) {
		if (manager.config.allyMode == AllyMode.FORCED_ALL || team.isMember(p)) {
			return true;
		} else if (manager.config.allyMode == AllyMode.FORCED_NONE) {
			return false;
		}

		return team.isAlly(p);
	}

	public boolean canUse(ServerPlayer p, PrivacyProperty property) {
		PrivacyMode mode = team.getProperty(property);

		if (mode == PrivacyMode.PUBLIC) {
			return true;
		} else if (mode == PrivacyMode.ALLIES) {
			if (PlayerHooks.isFake(p)) {
				return team.getProperty(ALLOW_FAKE_PLAYERS);
			}

			return isAlly(p.getUUID());
		}

		return team.isMember(p.getUUID());
	}

	public OrderedCompoundTag serializeNBT() {
		OrderedCompoundTag tag = new OrderedCompoundTag();
		tag.putInt("max_claim_chunks", maxClaimChunks);
		tag.putInt("max_force_load_chunks", maxForceLoadChunks);
		tag.putInt("extra_claim_chunks", extraClaimChunks);
		tag.putInt("extra_force_load_chunks", extraForceLoadChunks);
		tag.putBoolean("chunk_load_offline", chunkLoadOffline);

		CompoundTag chunksTag = new CompoundTag();

		for (ClaimedChunk chunk : getClaimedChunks()) {
			String key = chunk.getPos().dimension.location().toString();
			ListTag chunksListTag = chunksTag.getList(key, NbtType.COMPOUND);

			if (chunksListTag.isEmpty()) {
				chunksTag.put(key, chunksListTag);
			}

			OrderedCompoundTag o = new OrderedCompoundTag();
			o.singleLine = true;
			o.putInt("x", chunk.getPos().x);
			o.putInt("z", chunk.getPos().z);
			o.putLong("time", chunk.getTimeClaimed());

			if (chunk.isForceLoaded()) {
				o.putLong("force_loaded", chunk.getForceLoadedTime());
			}

			chunksListTag.add(o);
		}

		tag.put("chunks", chunksTag);

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		maxClaimChunks = tag.getInt("max_claim_chunks");
		maxForceLoadChunks = tag.getInt("max_force_load_chunks");
		extraClaimChunks = tag.getInt("extra_claim_chunks");
		extraForceLoadChunks = tag.getInt("extra_force_load_chunks");
		chunkLoadOffline = tag.getBoolean("chunk_load_offline");

		CompoundTag chunksTag = tag.getCompound("chunks");

		for (String key : chunksTag.getAllKeys()) {
			ResourceKey<Level> dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(key));
			ListTag chunksListTag = chunksTag.getList(key, NbtType.COMPOUND);

			for (int i = 0; i < chunksListTag.size(); i++) {
				CompoundTag o = chunksListTag.getCompound(i);
				ClaimedChunk chunk = new ClaimedChunk(this, new ChunkDimPos(dimKey, o.getInt("x"), o.getInt("z")));
				chunk.time = o.getLong("time");
				chunk.forceLoaded = o.getLong("force_loaded");
				manager.claimedChunks.put(chunk.pos, chunk);
			}
		}
	}

	public int getExtraClaimChunks() {
		return extraClaimChunks;
	}

	public int getExtraForceLoadChunks() {
		return extraForceLoadChunks;
	}

	public boolean getChunkLoadOffline() {
		return chunkLoadOffline;
	}

	public void setChunkLoadOffline(boolean val) {
		chunkLoadOffline = val;
		save();
	}

	public boolean getBypassProtection(ServerPlayer player) {
		return bypassProtection && !PlayerHooks.isFake(player);
	}

	public void saveNow() {
		if (shouldSave) {
			if (SNBT.write(file, serializeNBT())) {
				shouldSave = false;
			}
		}
	}
}
