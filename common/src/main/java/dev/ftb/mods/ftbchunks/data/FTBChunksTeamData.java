package dev.ftb.mods.ftbchunks.data;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.ChunkSendingUtils;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PrivacyMode;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamType;
import dev.ftb.mods.ftbteams.property.BooleanProperty;
import dev.ftb.mods.ftbteams.property.PrivacyProperty;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class FTBChunksTeamData {
	public static final BooleanProperty ALLOW_FAKE_PLAYERS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players"), true);
	public static final PrivacyProperty BLOCK_EDIT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty BLOCK_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_interact_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty ENTITY_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "entity_interact_mode"), PrivacyMode.ALLIES);
	//	public static final PrivacyProperty MINIMAP_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "minimap_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty LOCATION_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "location_mode"), PrivacyMode.ALLIES);
	public static final BooleanProperty ALLOW_EXPLOSIONS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_explosions"), false);
	public static final PrivacyProperty CLAIM_VISIBILITY = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "claim_visibility"), PrivacyMode.PUBLIC);

	public final ClaimedChunkManager manager;
	private final Team team;
	public final Path file;
	private boolean shouldSave;
	public int maxClaimChunks;
	public int maxForceLoadChunks;
	public int extraClaimChunks;
	public int extraForceLoadChunks;
	public final Set<UUID> forceLoadMembers;
	// claims owned by team members before they joined the team - to be returned when player leaves a party
	public final Map<UUID,Set<ChunkDimPos>> originalClaims;
	private Boolean canForceLoadChunks;

	public int prevChunkX = Integer.MAX_VALUE, prevChunkZ = Integer.MAX_VALUE;
	public String lastChunkID = "";
	private long lastLoginTime;

	public FTBChunksTeamData(ClaimedChunkManager m, Path f, Team t) {
		manager = m;
		team = t;
		file = f;
		shouldSave = false;
		maxClaimChunks = -1;
		maxForceLoadChunks = -1;
		extraClaimChunks = 0;
		extraForceLoadChunks = 0;
		forceLoadMembers = new HashSet<>();
		originalClaims = new HashMap<>();
		lastLoginTime = 0L;
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

		for (ClaimedChunk chunk : manager.getAllClaimedChunks()) {
			if (chunk.teamData == this) {
				list.add(chunk);
			}
		}

		return list;
	}

	public Collection<ClaimedChunk> getForceLoadedChunks() {
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunk chunk : manager.getAllClaimedChunks()) {
			if (chunk.teamData == this && chunk.isForceLoaded()) {
				list.add(chunk);
			}
		}

		return list;
	}

	public Collection<ClaimedChunk> getOriginalClaims(UUID playerID) {
		if (!originalClaims.containsKey(playerID)) return Collections.emptyList();

		List<ClaimedChunk> res = new ArrayList<>();
		for (ChunkDimPos cdp : originalClaims.get(playerID)) {
			ClaimedChunk cc = manager.getChunk(cdp);
			// original claim must still be claimed, and by the current team
			if (cc != null && cc.teamData == this) {
				res.add(manager.getChunk(cdp));
			}
		}

		return res;
	}

	public void noteOriginalClaims(FTBChunksTeamData originalTeam) {
		List<ChunkDimPos> claimedChunks = originalTeam.getClaimedChunks().stream().map(cc -> cc.pos).toList();
		originalClaims.computeIfAbsent(originalTeam.getTeamId(), k -> new HashSet<>()).addAll(claimedChunks);
	}

	public void updateLimits(ServerPlayer ownerPlayer) {
		if (maxClaimChunks != -1 && team.getType().isParty() && !ownerPlayer.getUUID().equals(team.getOwner())) {
			return;
		}

		int c = FTBChunksWorldConfig.getMaxClaimedChunks(this, ownerPlayer);
		int f = FTBChunksWorldConfig.getMaxForceLoadedChunks(this, ownerPlayer);

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
		ClaimedChunk chunk = manager.getChunk(pos);

		if (chunk != null) {
			return ClaimResults.ALREADY_CLAIMED;
		} else if (FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST_SET.contains(pos.dimension)) {
			return ClaimResults.DIMENSION_FORBIDDEN;
		} else if (!team.getType().isServer() && getClaimedChunks().size() >= FTBChunksWorldConfig.getMaxClaimedChunks(this, playerOrNull(source))) {
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

		chunk.setClaimedTime(System.currentTimeMillis());
		manager.registerClaim(pos, chunk);
		ClaimedChunkEvent.AFTER_CLAIM.invoker().after(source, chunk);
		save();
		return chunk;
	}

	public ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.getChunk(pos);

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
		ClaimedChunk chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.teamData != this && !source.hasPermission(2) && !source.getServer().isSingleplayer()) {
			return ClaimResults.NOT_OWNER;
		} else if (chunk.isForceLoaded()) {
			return ClaimResults.ALREADY_LOADED;
		} else if (!team.getType().isServer() && getForceLoadedChunks().size() >= FTBChunksWorldConfig.getMaxForceLoadedChunks(this, playerOrNull(source))) {
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
		ClaimedChunkEvent.AFTER_LOAD.invoker().after(source, chunk);
		chunk.teamData.save();
		chunk.sendUpdateToAll();
		return chunk;
	}

	public ClaimResult unload(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.getChunk(pos);

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
		if (FTBChunksWorldConfig.ALLY_MODE.get() == AllyMode.FORCED_ALL || team.isMember(p)) {
			return true;
		} else if (FTBChunksWorldConfig.ALLY_MODE.get() == AllyMode.FORCED_NONE) {
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

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.putInt("max_claim_chunks", maxClaimChunks);
		tag.putInt("max_force_load_chunks", maxForceLoadChunks);
		tag.putInt("extra_claim_chunks", extraClaimChunks);
		tag.putInt("extra_force_load_chunks", extraForceLoadChunks);
		tag.putLong("last_login_time", lastLoginTime);

		ListTag forceLoadMembersTag = new ListTag();

		for (UUID id : forceLoadMembers) {
			forceLoadMembersTag.add(StringTag.valueOf(id.toString()));
		}

		tag.put("force_load_members", forceLoadMembersTag);


		CompoundTag chunksTag = new CompoundTag();

		for (ClaimedChunk chunk : getClaimedChunks()) {
			String key = chunk.getPos().dimension.location().toString();
			ListTag chunksListTag = chunksTag.getList(key, Tag.TAG_COMPOUND);

			if (chunksListTag.isEmpty()) {
				chunksTag.put(key, chunksListTag);
			}

			SNBTCompoundTag o = new SNBTCompoundTag();
			o.singleLine();
			o.putInt("x", chunk.getPos().x);
			o.putInt("z", chunk.getPos().z);
			o.putLong("time", chunk.getTimeClaimed());
			if (chunk.isForceLoaded()) {
				o.putLong("force_loaded", chunk.getForceLoadedTime());
			}
			if (chunk.getExpiryTime() > 0L) {
				o.putLong("expiry_time", chunk.getExpiryTime());
			}

			chunksListTag.add(o);
		}
		tag.put("chunks", chunksTag);

		if (team.getType() == TeamType.PARTY && !originalClaims.isEmpty()) {
			CompoundTag originalClaimsTag = new CompoundTag();
			originalClaims.forEach((id, posList) -> {
				CompoundTag playerTag = new CompoundTag();
				Map<String,ListTag> perDimensionTags = new HashMap<>();
				posList.forEach(c -> {
					ListTag l = perDimensionTags.computeIfAbsent(c.dimension.location().toString(), k -> new ListTag());
					CompoundTag cdpTag = new CompoundTag();
					cdpTag.putInt("x", c.x);
					cdpTag.putInt("z", c.z);
					l.add(cdpTag);
				});
				perDimensionTags.forEach(playerTag::put);
				originalClaimsTag.put(id.toString(), playerTag);
			});
			tag.put("original_claims", originalClaimsTag);
		}

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		maxClaimChunks = tag.getInt("max_claim_chunks");
		maxForceLoadChunks = tag.getInt("max_force_load_chunks");
		extraClaimChunks = tag.getInt("extra_claim_chunks");
		extraForceLoadChunks = tag.getInt("extra_force_load_chunks");
		lastLoginTime = tag.getLong("last_login_time");
		forceLoadMembers.clear();

		ListTag forgeLoadMembersTag = tag.getList("force_load_members", Tag.TAG_STRING);

		if (tag.getBoolean("chunk_load_offline")) {
			forceLoadMembers.add(team.getOwner());
		} else {
			for (int i = 0; i < forgeLoadMembersTag.size(); i++) {
				forceLoadMembers.add(UUID.fromString(forgeLoadMembersTag.getString(i)));
			}
		}

		forceLoadMembers.remove(Util.NIL_UUID);
		canForceLoadChunks = null;

		CompoundTag chunksTag = tag.getCompound("chunks");

		for (String key : chunksTag.getAllKeys()) {
			ResourceKey<Level> dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(key));
			ListTag chunksListTag = chunksTag.getList(key, Tag.TAG_COMPOUND);

			for (int i = 0; i < chunksListTag.size(); i++) {
				CompoundTag o = chunksListTag.getCompound(i);
				ClaimedChunk chunk = new ClaimedChunk(this, new ChunkDimPos(dimKey, o.getInt("x"), o.getInt("z")));
				chunk.time = o.getLong("time");
				chunk.forceLoaded = o.getLong("force_loaded");
				chunk.expiryTime = o.getLong("expiry_time");
				manager.registerClaim(chunk.pos, chunk);
			}
		}

		CompoundTag originalClaimsTag = tag.getCompound("original_claims");
		originalClaims.clear();
		for (String idStr : originalClaimsTag.getAllKeys()) {
			try {
				UUID id = UUID.fromString(idStr);
				CompoundTag playerTag = originalClaimsTag.getCompound(idStr);
				for (String dimStr : playerTag.getAllKeys()) {
					ResourceKey<Level> dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimStr));
					Set<ChunkDimPos> cdpSet = new HashSet<>();
					playerTag.getList(dimStr, Tag.TAG_COMPOUND).forEach(el -> {
						if (el instanceof CompoundTag c) {
							cdpSet.add(new ChunkDimPos(dimKey, c.getInt("x"), c.getInt("z")));
						}
					});
					originalClaims.put(id, cdpSet);
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

	}

	public int getExtraClaimChunks() {
		return extraClaimChunks;
	}

	public int getExtraForceLoadChunks() {
		return extraForceLoadChunks;
	}

	public void setForceLoadMember(UUID id, boolean val) {
		FTBChunks.LOGGER.debug(String.format("chunks: set force load member %s = %s", id, val));
		boolean wasEmpty = forceLoadMembers.isEmpty();
		if (val ? forceLoadMembers.add(id) : forceLoadMembers.remove(id)) {
			save();
			canForceLoadChunks = null;
			manager.clearForceLoadedCache();
		}

		if (!wasEmpty && forceLoadMembers.isEmpty()) {
			// last player who could force-load chunks has gone offline
			updateChunkTickets(false);
		} else if (wasEmpty && !forceLoadMembers.isEmpty()) {
			// first player who can force-load chunks has come online
			updateChunkTickets(true);
		}
	}

	private void updateChunkTickets(boolean load) {
		getClaimedChunks().forEach(chunk -> {
			if (chunk.isForceLoaded()) {
				ServerLevel level = manager.getMinecraftServer().getLevel(chunk.pos.dimension);
				if (level != null) {
					FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, getTeamId(), chunk.pos.x, chunk.pos.z, load);
				}
			}
		});
	}

	public void saveNow() {
		if (shouldSave) {
			if (SNBT.write(file, serializeNBT())) {
				shouldSave = false;
			}
		}
	}

	public boolean canForceLoadChunks() {
		if (canForceLoadChunks == null) {
			canForceLoadChunks = switch (FTBChunksWorldConfig.FORCE_LOAD_MODE.get()) {
				case ALWAYS -> true;
				case NEVER -> false;
				default -> hasForceLoadMembers();
			};
		}
		return canForceLoadChunks;
	}

	public boolean hasForceLoadMembers() {
		for (UUID uuid : team.getMembers()) {
			if (forceLoadMembers.contains(uuid)) {
				return true;
			}
		}

		return false;
	}

	public static ServerPlayer playerOrNull(CommandSourceStack source) {
		Entity entity = source.getEntity();
		return entity instanceof ServerPlayer player ? player : null;
	}

	public boolean allowExplosions() {
		return team.getProperty(ALLOW_EXPLOSIONS);
	}

	public void setLastLoginTime(long when) {
		this.lastLoginTime = when;
		save();
	}

	public long getLastLoginTime() {
		if (lastLoginTime == 0L) {
			setLastLoginTime(System.currentTimeMillis());
		}
		return lastLoginTime;
	}

	public boolean shouldHideClaims() {
		return getTeam().getProperty(CLAIM_VISIBILITY) != PrivacyMode.PUBLIC;
	}

	public void syncChunksToPlayer(ServerPlayer recipient) {
		chunksByDimension().forEach((dimension, chunkPackets) -> {
			if (!chunkPackets.isEmpty()) {
				ChunkSendingUtils.sendManyChunksToPlayer(recipient, this, new SendManyChunksPacket(dimension, getTeamId(), chunkPackets));
			}
		});
	}

	public void syncChunksToAll(MinecraftServer server) {
		chunksByDimension().forEach((dimension, chunkPackets) -> {
			if (!chunkPackets.isEmpty()) {
				ChunkSendingUtils.sendManyChunksToAll(server, this, new SendManyChunksPacket(dimension, getTeamId(), chunkPackets));
			}
		});
	}

	private Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksByDimension() {
		long now = System.currentTimeMillis();
		return getClaimedChunks().stream()
				.collect(Collectors.groupingBy(
						c -> c.pos.dimension, Collectors.mapping(c -> new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z, c), Collectors.toList())
				));
	}
}
