package dev.ftb.mods.ftbchunks.data;

import com.mojang.authlib.GameProfile;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.ChunkSendingUtils;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.PrivacyMode;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.property.BooleanProperty;
import dev.ftb.mods.ftbteams.property.PrivacyProperty;
import dev.ftb.mods.ftbteams.property.StringListProperty;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class FTBChunksTeamData {
	public static final BooleanProperty ALLOW_ALL_FAKE_PLAYERS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players"), false);
	public static final StringListProperty ALLOW_NAMED_FAKE_PLAYERS = new StringListProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_named_fake_players"), new ArrayList<>());
	public static final BooleanProperty ALLOW_FAKE_PLAYERS_BY_ID = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players_by_id"), true);
	// forge
	public static final PrivacyProperty BLOCK_EDIT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty BLOCK_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_interact_mode"), PrivacyMode.ALLIES);
	// fabric
	public static final PrivacyProperty BLOCK_EDIT_AND_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_and_interact_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty ENTITY_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "entity_interact_mode"), PrivacyMode.ALLIES);
	public static final PrivacyProperty NONLIVING_ENTITY_ATTACK_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "nonliving_entity_attack_mode"), PrivacyMode.ALLIES);
	public static final BooleanProperty ALLOW_EXPLOSIONS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_explosions"), false);
	public static final BooleanProperty ALLOW_MOB_GRIEFING = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_mob_griefing"), false);
	public static final PrivacyProperty CLAIM_VISIBILITY = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "claim_visibility"), PrivacyMode.PUBLIC);

	public static final PrivacyProperty ALLOW_ATTACK_BLACKLISTED_ENTITIES = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_attack_blacklisted_entities"), PrivacyMode.ALLIES);
	public static final BooleanProperty ALLOW_ANY_FAKE_PLAYER_BREAK_IF_FORCE_LOADED = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_any_fake_player_break_if_force_loaded"), true);

	//	public static final PrivacyProperty MINIMAP_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "minimap_mode"), PrivacyMode.ALLIES);

	public static final PrivacyProperty LOCATION_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "location_mode"), PrivacyMode.ALLIES);
	public final ClaimedChunkManager manager;
	private final Team team;
	public final Path file;
	private boolean shouldSave;
	private int maxClaimChunks;
	private int maxForceLoadChunks;
	public int extraClaimChunks;
	public int extraForceLoadChunks;
	private Boolean canForceLoadChunks;
	private final Map<UUID,TeamMemberData> memberData;

	public int prevChunkX = Integer.MAX_VALUE, prevChunkZ = Integer.MAX_VALUE;
	public String lastChunkID = "";
	private long lastLoginTime;
	private long lastLogoffTime;
	private Set<String> fakePlayerNameCache;
	private final BrokenBlocksCounter brokenBlocksCounter;

	public FTBChunksTeamData(ClaimedChunkManager m, Path f, Team t) {
		manager = m;
		team = t;
		file = f;
		shouldSave = false;
		maxClaimChunks = -1;
		maxForceLoadChunks = -1;
		extraClaimChunks = 0;
		extraForceLoadChunks = 0;
		lastLoginTime = 0L;
		lastLogoffTime = 0L;
		memberData = new HashMap<>();
		brokenBlocksCounter = new BrokenBlocksCounter();
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
		if (!memberData.containsKey(playerID)) return Collections.emptyList();

		List<ClaimedChunk> res = new ArrayList<>();
		for (ChunkDimPos cdp : memberData.get(playerID).getOriginalClaims()) {
			ClaimedChunk cc = manager.getChunk(cdp);
			// original claim must still be claimed, and by the current team
			if (cc != null && cc.teamData == this) {
				res.add(manager.getChunk(cdp));
			}
		}

		return res;
	}

	public ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.getChunk(pos);

		if (chunk != null) {
			return ClaimResults.ALREADY_CLAIMED;
		} else if (!DimensionFilter.isDimensionOK(pos.dimension)) {
			return ClaimResults.DIMENSION_FORBIDDEN;
		} else if (!team.getType().isServer() && getClaimedChunks().size() >= getMaxClaimChunks()) {
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
		} else if (!team.getType().isServer() && getForceLoadedChunks().size() >= getMaxForceLoadChunks()) {
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

	protected boolean baseUseCheck(ServerPlayer p, PrivacyProperty property, boolean offlineCheck, boolean forceLoadedChunk) {
		PrivacyMode mode = team.getProperty(property);

		if (mode == PrivacyMode.PUBLIC) {
			return true;
		}

		if (PlayerHooks.isFake(p)) {
			return canFakePlayerUse(p, mode, forceLoadedChunk);
		} else if (mode == PrivacyMode.ALLIES) {
			return isAlly(p.getUUID());
		} else {
			if (team.isMember(p.getUUID())) return true;
			if (offlineCheck && FTBChunksWorldConfig.OFFLINE_PROTECTION_ONLY.get()) {
				return canUseOffline();
			}
			return false;
		}
	}

	public boolean canUseOffline() {
		if (!team.getOnlineMembers().isEmpty()) {
			return true;
		}
		long buffer = FTBChunksWorldConfig.OFFLINE_PROTECTION_BUFFER.get();
		long now = System.currentTimeMillis();
		long timeDiff = now - getLastLogoffTime();
		return timeDiff < buffer * 1000;
	}

	public boolean canUse(ServerPlayer p, PrivacyProperty property) {
		return baseUseCheck(p, property, true, false);
	}

	public boolean canAttackBlackListedEntity(ServerPlayer p, PrivacyProperty property) {
		if (baseUseCheck(p, property, true, false)) return true;
        return FTBChunksWorldConfig.PROTECT_ENTITIES_OFFLINE_ONLY.get() && canUseOffline();
    }

	public boolean canBreak(ServerPlayer p, PrivacyProperty property, boolean leftClick, BlockState state, boolean forceLoadedChunk) {
		if (baseUseCheck(p, property, false, forceLoadedChunk)) return true;
		if (FTBChunksWorldConfig.OFFLINE_PROTECTION_ONLY.get() && !canUseOffline()) return false;
		if (state.is(FTBChunksAPI.EDIT_BLACKLIST_TAG)) return false;
		if (brokenBlocksCounter.canBreakBlock(p, leftClick)) {
			if (!leftClick) save();
			return true;
		}
		return false;
	}

	private boolean canFakePlayerUse(Player player, PrivacyMode mode, boolean forceLoadedChunk) {
		if (team.getProperty(FTBChunksTeamData.ALLOW_ALL_FAKE_PLAYERS)) {
			return mode == PrivacyMode.ALLIES;
		}
		if (forceLoadedChunk && team.getProperty(FTBChunksTeamData.ALLOW_ANY_FAKE_PLAYER_BREAK_IF_FORCE_LOADED)) {
			return true;
		}

		boolean checkById = team.getProperty(FTBChunksTeamData.ALLOW_FAKE_PLAYERS_BY_ID) && player.getUUID() != null;
		if (mode == PrivacyMode.ALLIES) {
			return checkById && isAlly(player.getUUID()) || fakePlayerMatches(player.getGameProfile());
		} else if (mode == PrivacyMode.PRIVATE) {
			return checkById && team.isMember(player.getUUID());
		}

		return false;
	}

	private boolean fakePlayerMatches(GameProfile profile) {
		return profile.getName() != null && getCachedFakePlayerNames().contains(profile.getName().toLowerCase(Locale.ROOT))
				|| profile.getId() != null && getCachedFakePlayerNames().contains(profile.getId().toString().toLowerCase(Locale.ROOT));
	}

	private Set<String> getCachedFakePlayerNames() {
		if (fakePlayerNameCache == null) {
			fakePlayerNameCache = team.getProperty(FTBChunksTeamData.ALLOW_NAMED_FAKE_PLAYERS).stream()
					.map(s -> s.toLowerCase(Locale.ROOT))
					.collect(Collectors.toSet());
		}
		return fakePlayerNameCache;
	}

	public void clearFakePlayerNameCache() {
		fakePlayerNameCache = null;
	}

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.putInt("max_claim_chunks", getMaxClaimChunks());
		tag.putInt("max_force_load_chunks", getMaxForceLoadChunks());
		if (extraClaimChunks > 0 && !(team instanceof PartyTeam)) tag.putInt("extra_claim_chunks", extraClaimChunks);
		if (extraForceLoadChunks > 0 && !(team instanceof PartyTeam)) tag.putInt("extra_force_load_chunks", extraForceLoadChunks);
		tag.putLong("last_login_time", lastLoginTime);
		tag.putLong("last_logoff_time", lastLogoffTime);

		CompoundTag chunksTag = new CompoundTag();
		for (ClaimedChunk chunk : getClaimedChunks()) {
			String key = chunk.getPos().dimension.location().toString();
			ListTag chunksListTag = chunksTag.getList(key, Tag.TAG_COMPOUND);
			if (chunksListTag.isEmpty()) {
				chunksTag.put(key, chunksListTag);
			}
			chunksListTag.add(chunk.serializeNBT());
		}
		tag.put("chunks", chunksTag);

		CompoundTag memberTag = new CompoundTag();
		memberData.forEach((id, data) -> memberTag.put(id.toString(), data.serializeNBT()));
		if (!memberTag.isEmpty()) {
			tag.put("member_data", memberTag);
		}

		tag.put("broken_blocks_counter", brokenBlocksCounter.serializeNBT());

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		maxClaimChunks = tag.getInt("max_claim_chunks");
		maxForceLoadChunks = tag.getInt("max_force_load_chunks");
		extraClaimChunks = tag.getInt("extra_claim_chunks");
		extraForceLoadChunks = tag.getInt("extra_force_load_chunks");
		lastLoginTime = tag.getLong("last_login_time");
		lastLogoffTime = tag.getLong("last_logoff_time");
		canForceLoadChunks = null;

		CompoundTag chunksTag = tag.getCompound("chunks");

		for (String key : chunksTag.getAllKeys()) {
			ResourceKey<Level> dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(key));
			ListTag chunksListTag = chunksTag.getList(key, Tag.TAG_COMPOUND);

			for (int i = 0; i < chunksListTag.size(); i++) {
				ClaimedChunk chunk = ClaimedChunk.deserializeNBT(this, dimKey, chunksListTag.getCompound(i));
				manager.registerClaim(chunk.pos, chunk);
			}
		}

		memberData.clear();
		CompoundTag memberTag = tag.getCompound("member_data");
		for (String key : memberTag.getAllKeys()) {
			try {
				UUID id = UUID.fromString(key);
				if (id != Util.NIL_UUID) {
					memberData.put(id, TeamMemberData.deserializeNBT(memberTag.getCompound(key)));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		brokenBlocksCounter.deserializeNBT(tag.getCompound("broken_blocks_counter"));
	}

	public int getExtraClaimChunks() {
		if (extraClaimChunks > 0 && team instanceof PartyTeam) {
			FTBChunks.LOGGER.info("found non-zero extra_claim_chunks={} in party team {}: transferring to owner {}", extraClaimChunks, getTeamId(), team.getOwner());
			FTBChunksTeamData personalTeam = FTBChunksAPI.getManager().getPersonalData(team.getOwner());
			personalTeam.extraClaimChunks = extraClaimChunks;
			extraClaimChunks = 0;
			save();
			personalTeam.save();
		}
		return extraClaimChunks;
	}

	public int getExtraForceLoadChunks() {
		if (extraForceLoadChunks > 0 && team instanceof PartyTeam) {
			FTBChunks.LOGGER.info("found non-zero extra_force_load_chunks={} in party team {}: transferring to owner {}", extraForceLoadChunks, getTeamId(), team.getOwner());
			FTBChunksTeamData personalTeam = FTBChunksAPI.getManager().getPersonalData(team.getOwner());
			personalTeam.extraForceLoadChunks = extraForceLoadChunks;
			extraForceLoadChunks = 0;
			save();
			personalTeam.save();
		}
		return extraForceLoadChunks;
	}

	public void setForceLoadMember(UUID id, boolean val) {
		long oldForceCount = memberData.values().stream().filter(TeamMemberData::isOfflineForceLoader).count();
		getTeamMemberData(id).setOfflineForceLoader(val);
		long newForceCount = memberData.values().stream().filter(TeamMemberData::isOfflineForceLoader).count();

		if (oldForceCount != newForceCount) {
			FTBChunks.LOGGER.debug("team {}: set force load member {} = {}", team.getId(), id, val);
			save();
			canForceLoadChunks = null;
			manager.clearForceLoadedCache();
		}
	}

	@NotNull
	private TeamMemberData getTeamMemberData(UUID id) {
		if (id.equals(Util.NIL_UUID)) {
			FTBChunks.LOGGER.warn("attempt to get member data for nil UUID");
			new RuntimeException().printStackTrace();
			return TeamMemberData.defaultData();
		}
		return memberData.computeIfAbsent(id, k -> TeamMemberData.defaultData());
	}

	public void updateChunkTickets(boolean load) {
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
				default -> FTBChunksWorldConfig.CHUNK_LOAD_OFFLINE.get() || hasForceLoadMembers();
			};
		}
		return canForceLoadChunks;
	}

	public boolean hasForceLoadMembers() {
		return memberData.values().stream().anyMatch(TeamMemberData::isOfflineForceLoader);
	}

	public boolean allowExplosions() {
		return team.getProperty(ALLOW_EXPLOSIONS);
	}

	public boolean allowMobGriefing() {
		return team.getProperty(ALLOW_MOB_GRIEFING);
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

	public long getLastLogoffTime() {
		return lastLogoffTime;
	}

	public void setLastLogoffTime(long when) {
		this.lastLogoffTime = when;
		save();
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

	public int getMaxClaimChunks() {
		return maxClaimChunks;
	}

	public int getMaxForceLoadChunks() {
		return maxForceLoadChunks;
	}

	public void updateLimits() {
		updateMemberLimitData(!memberData.isEmpty());

		if (!(team instanceof PartyTeam)) {
			TeamMemberData m = getTeamMemberData(getTeam().getId());
			maxClaimChunks = m.getMaxClaims();
			maxForceLoadChunks = m.getMaxForceLoads();
		} else {
			switch (FTBChunksWorldConfig.PARTY_LIMIT_MODE.get()) {
				case OWNER -> {
					TeamMemberData m = getTeamMemberData(getTeam().getOwner());
					maxClaimChunks = m.getMaxClaims();
					maxForceLoadChunks = m.getMaxForceLoads();
				}
				case SUM -> {
					maxClaimChunks = maxForceLoadChunks = 0;
					memberData.values().forEach(m -> {
						maxClaimChunks += m.getMaxClaims();
						maxForceLoadChunks += m.getMaxForceLoads();
					});
				}
				case LARGEST -> {
					maxClaimChunks = maxForceLoadChunks = 0;
					for (TeamMemberData m : memberData.values()) {
						maxClaimChunks = Math.max(m.getMaxClaims(), maxClaimChunks);
						maxForceLoadChunks = Math.max(m.getMaxForceLoads(), maxForceLoadChunks);
					}
				}
				case AVERAGE -> {
					maxClaimChunks = maxForceLoadChunks = 0;
					memberData.values().forEach(m -> {
						maxClaimChunks += m.getMaxClaims();
						maxForceLoadChunks += m.getMaxForceLoads();
					});
					if (memberData.size() > 0) {
						maxClaimChunks /= memberData.size();
						maxForceLoadChunks /= memberData.size();
					}
				}
			}
		}

		if (FTBChunksWorldConfig.HARD_TEAM_CLAIM_LIMIT.get() > 0) {
			maxClaimChunks = Math.min(maxClaimChunks, FTBChunksWorldConfig.HARD_TEAM_CLAIM_LIMIT.get());
		}
		if (FTBChunksWorldConfig.HARD_TEAM_FORCE_LIMIT.get() > 0) {
			maxForceLoadChunks = Math.min(maxForceLoadChunks, FTBChunksWorldConfig.HARD_TEAM_FORCE_LIMIT.get());
		}

		SendGeneralDataPacket.send(this, getTeam().getOnlineMembers());

		save();
	}

	private void updateMemberLimitData(boolean onlinePlayersOnly) {
		Set<UUID> members = new HashSet<>(team.getMembers());

		for (ServerPlayer p : team.getOnlineMembers()) {
			Team playerTeam = FTBTeamsAPI.getManager().getInternalPlayerTeam(p.getUUID());

			TeamMemberData m = getTeamMemberData(p.getUUID());
			if (playerTeam != null) {
				// pull limits in from the player's *personal* team data, if possible
				FTBChunksTeamData personalData = FTBChunksAPI.getManager().getData(playerTeam);
				m.setMaxClaims(FTBChunksWorldConfig.getMaxClaimedChunks(personalData, p));
				m.setMaxForceLoads(FTBChunksWorldConfig.getMaxForceLoadedChunks(personalData, p));
			} else {
				// missing player's personal team data? shouldn't happen, but just in case...
				m.setMaxClaims(FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get());
				m.setMaxForceLoads(FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get());
			}
			members.remove(p.getUUID());
		}

		// remaining members are currently offline - we don't know their personal claim limits yet,
		// so fill in with server defaults; they will get updated when the player next logs in
		if (!onlinePlayersOnly) {
			for (UUID id : members) {
				TeamMemberData m = getTeamMemberData(id);

				Team playerTeam = FTBTeamsAPI.getManager().getInternalPlayerTeam(id);
				int maxC, maxF;
				if (playerTeam != null) {
					FTBChunksTeamData personalData = FTBChunksAPI.getManager().getData(playerTeam);
					maxC = FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get() + personalData.getExtraClaimChunks();
					maxF = FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS.get() + personalData.getExtraForceLoadChunks();
				} else {
					maxC = FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get();
					maxF = FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS.get();
				}

				m.setMaxClaims(maxC);
				m.setMaxForceLoads(maxF);
			}
		}

		save();
	}

	public void addMemberData(ServerPlayer player, FTBChunksTeamData otherTeam) {
		if (otherTeam.team instanceof PlayerTeam) {
			memberData.put(otherTeam.getTeamId(), TeamMemberData.fromPlayerData(player, otherTeam));
			save();
		}
	}

	public void deleteMemberData(UUID playerId) {
		if (memberData.remove(playerId) != null) {
			save();
		}
	}

	public void resetBrokenBlocksCounter() {
		brokenBlocksCounter.reset();
		save();
	}

	public static final Style WARNING_STYLE = Style.EMPTY.withColor(0xFFFF55);
	public static final int HOUR_TICKS = 60 * 60 * 20;

	public static class BrokenBlocksCounter {
		private final List<BrokenBlocksGroup> groups = new ArrayList<>();
		public BrokenBlocksCounter() {}
		public boolean canBreakBlock(ServerPlayer p, boolean leftClick) {
			long time = p.getLevel().getGameTime();
			int blocks_per_hour = FTBChunksWorldConfig.MAX_DESTROY_BLOCKS_PER_HOUR.get();
			if (blocks_per_hour == -1)
				return true;
			int total = getTotalBrokenBlocks(time);
			if (total >= blocks_per_hour)
				return false;
			if (!leftClick) {
				int group_period_tick = FTBChunksWorldConfig.DESTROY_BLOCKS_COUNT_PERIOD.get() * 20;
				BrokenBlocksGroup group = getCurrentGroup(time, group_period_tick);
				group.addBrokenBlock();
				if (total+1 >= blocks_per_hour) {
					p.sendSystemMessage(Component.translatable("ftbchunks.block_break_limit_reached")
							.setStyle(WARNING_STYLE));
				}
			}
			return true;
		}
		public int getTotalBrokenBlocks(long time) {
			removeOldGroups(time);
			int total = 0;
			for (BrokenBlocksGroup group : groups)
				total += group.getBrokenBlocks();
			return total;
		}
		private BrokenBlocksGroup getCurrentGroup(long time, int length) {
			if (groups.isEmpty()) return addGroup(time, length);
			BrokenBlocksGroup group = groups.get(0);
			if (!group.isCurrentGroup(time)) return addGroup(time, length);
			return group;
		}
		private BrokenBlocksGroup addGroup(long time, int length) {
			BrokenBlocksGroup group = new BrokenBlocksGroup(time, length);
			groups.add(0, group);
			return group;
		}
		private void removeOldGroups(long time) {
			for (int i = 0; i < groups.size(); ++i)
				if (groups.get(i).isOutdated(time))
					groups.remove(i--);
		}
		public void reset() {
			groups.clear();
		}
		public SNBTCompoundTag serializeNBT() {
			SNBTCompoundTag tag = new SNBTCompoundTag();
			ListTag list = new ListTag();
            for (BrokenBlocksGroup group : groups) list.add(group.serializeNBT());
			tag.put("groups", list);
			return tag;
		}
		public void deserializeNBT(CompoundTag tag) {
			ListTag list = tag.getList("groups", 10);
			for (int i = 0; i < list.size(); ++i){
				CompoundTag groupNBT = list.getCompound(i);
				BrokenBlocksGroup group = new BrokenBlocksGroup();
				group.deserializeNBT(groupNBT);
				groups.add(group);
			}
		}
	}

	public static class BrokenBlocksGroup {
		private long startTime;
		private int brokenBlocks, length;
		private BrokenBlocksGroup() {}
		public BrokenBlocksGroup(long startTime, int length) {
            this.startTime = startTime;
			this.length = length;
        }
		public long getStartTime() {
			return startTime;
		}
		public int getBrokenBlocks() {
			return brokenBlocks;
		}
		public void addBrokenBlock() {
			++brokenBlocks;
		}
		public int getLength() {
			return length;
		}
		public boolean isOutdated(long time) {
			return time - (getStartTime() + getLength()) > HOUR_TICKS;
		}
		public boolean isCurrentGroup(long time) {
			return getStartTime() + getLength() > time;
		}
        public SNBTCompoundTag serializeNBT() {
			SNBTCompoundTag tag = new SNBTCompoundTag();
			tag.putLong("start_time", startTime);
			tag.putInt("broken_blocks", brokenBlocks);
			tag.putInt("length", length);
			return tag;
		}
		public void deserializeNBT(CompoundTag tag) {
			startTime = tag.getLong("start_time");
			brokenBlocks = tag.getInt("broken_blocks");
			length = tag.getInt("length");
		}
	}
}
