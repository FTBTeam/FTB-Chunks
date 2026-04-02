package dev.ftb.mods.ftbchunks.data;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.json5.Json5Ops;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ChunkTeamDataImpl implements ChunkTeamData {
	private final ClaimedChunkManagerImpl manager;
	private final Team team;
	private final Path file;

	private boolean shouldSave;
	private int maxClaimChunks;
	private int maxForceLoadChunks;
	private int extraClaimChunks;
	private int extraForceLoadChunks;
	@Nullable
	private Boolean canForceLoadChunks;
	private final Map<UUID,TeamMemberData> memberData;

	private int prevChunkX = Integer.MAX_VALUE;
	private int prevChunkZ = Integer.MAX_VALUE;
	private String lastChunkID = "";
	private long lastLoginTime;

	@Nullable
	private Set<String> fakePlayerNameCache;
	@Nullable
	private Collection<ClaimedChunkImpl> claimedChunkCache;
	@Nullable
	private Collection<ClaimedChunkImpl> forcedChunkCache;

	private final Map<UUID,PreventedAccess> preventedAccess = new HashMap<>();

	private ChunkTeamDataImpl(ClaimedChunkManagerImpl manager, Path file, Team team) {
		this.manager = manager;
		this.file = file;
		this.team = team;

		shouldSave = false;
		maxClaimChunks = -1;
		maxForceLoadChunks = -1;
		extraClaimChunks = 0;
		extraForceLoadChunks = 0;
		lastLoginTime = 0L;
		memberData = new HashMap<>();
	}

	public static ChunkTeamDataImpl loadFromFile(ClaimedChunkManagerImpl manager, Path dataDirectory, Team team) {
		Path path = dataDirectory.resolve(team.getId() + Json5Util.FILE_EXT);
		ChunkTeamDataImpl data = new ChunkTeamDataImpl(manager, path, team);

		try {
			if (Files.exists(path)) {
				Json5Object dataFile = Json5Util.tryRead(path);
				data.deserializeJson(dataFile);
			} else {
				data.markDirty();
				data.saveNow();
				FTBChunks.LOGGER.info("Created initial chunk team data file {}", path);
			}
		} catch (Exception ex) {
			FTBChunks.LOGGER.error("Failed to load data for team {}: {}", team.getId(), ex.getMessage());
		}

		return data;
	}

	@Override
	public String toString() {
		return team.getId().toString();
	}

	@Override
	public ClaimedChunkManagerImpl getManager() {
		return manager;
	}

	public Path getFile() {
		return file;
	}

	@Override
	public TeamManager getTeamManager() {
		return manager.getTeamManager();
	}

	@Override
	public Team getTeam() {
		return team;
	}

	public UUID getTeamId() {
		return team.getId();
	}

	@Override
	public void setExtraClaimChunks(int extraClaimChunks) {
		this.extraClaimChunks = extraClaimChunks;
	}

	@Override
	public void setExtraForceLoadChunks(int extraForceLoadChunks) {
		this.extraForceLoadChunks = extraForceLoadChunks;
	}

	@Override
	public Collection<ClaimedChunkImpl> getClaimedChunks() {
		if (claimedChunkCache == null) {
			claimedChunkCache = manager.getAllClaimedChunks().stream()
					.filter(chunk -> chunk.getTeamData() == this)
					.collect(Collectors.toList());
		}
		return claimedChunkCache;
	}

	@Override
	public Collection<ClaimedChunkImpl> getForceLoadedChunks() {
		if (forcedChunkCache == null) {
			forcedChunkCache = manager.getAllClaimedChunks().stream()
					.filter(chunk -> chunk.getTeamData() == this && chunk.isForceLoaded())
					.collect(Collectors.toList());
		}
		return forcedChunkCache;
	}

	public Collection<ClaimedChunkImpl> getOriginalClaims(UUID playerID) {
		if (!memberData.containsKey(playerID)) return Collections.emptyList();

		List<ClaimedChunkImpl> res = new ArrayList<>();
		for (ChunkDimPos cdp : memberData.get(playerID).getOriginalClaims()) {
			ClaimedChunkImpl cc = manager.getChunk(cdp);
			// original claim must still be claimed, and by the current team
			if (cc != null && cc.getTeamData() == this) {
				res.add(cc);
			}
		}

		return res;
	}

	@Override
	public ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk != null) {
			return ClaimResult.StandardProblem.ALREADY_CLAIMED;
		} else if (!DimensionFilter.isDimensionOK(pos.dimension())) {
			return ClaimResult.StandardProblem.DIMENSION_FORBIDDEN;
		} else if (!team.isServerTeam() && getClaimedChunks().size() >= getMaxClaimChunks()) {
			return ClaimResult.StandardProblem.NOT_ENOUGH_POWER;
		}

		chunk = new ClaimedChunkImpl(this, pos);
		DataOutcome<ClaimResult> result = ChunkChangeEvent.Pre.TYPE.post(new ChunkChangeEvent.Pre.Data(source, chunk, ChunkChangeEvent.Operation.CLAIM));

		if (result.isFail()) {
			return result.data().orElseThrow();
		} else if (!checkOnly) {
			chunk.setClaimedTime(System.currentTimeMillis());
			manager.registerClaim(pos, chunk);

			NativeEventPosting.get().postEvent(new ChunkChangeEvent.Post.Data(source, chunk, ChunkChangeEvent.Operation.CLAIM));
			markDirty();
		}
		return ClaimResult.success();
	}

	@Override
	public ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this && !(adminOverride && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) && !source.getServer().isSingleplayer()) {
			return ClaimResult.StandardProblem.NOT_OWNER;
		}

		DataOutcome<ClaimResult> result = ChunkChangeEvent.Pre.TYPE.post(new ChunkChangeEvent.Pre.Data(source, chunk, ChunkChangeEvent.Operation.UNCLAIM));
		if (result.isFail()) {
			return result.data().orElseThrow();
		} else if (!checkOnly) {
			chunk.unclaim(source, true);
			markDirty();
		}

		return ClaimResult.success();
	}

	@Override
	public ClaimResult forceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this && !(adminOverride && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) && !source.getServer().isSingleplayer()) {
			return ClaimResult.StandardProblem.NOT_OWNER;
		} else if (chunk.isForceLoaded()) {
			return ClaimResult.StandardProblem.ALREADY_LOADED;
		} else if (!team.isServerTeam() && getForceLoadedChunks().size() >= getMaxForceLoadChunks()) {
			return ClaimResult.StandardProblem.NOT_ENOUGH_POWER;
		}

		DataOutcome<ClaimResult> result = ChunkChangeEvent.Pre.TYPE.post(new ChunkChangeEvent.Pre.Data(source, chunk, ChunkChangeEvent.Operation.LOAD));
		if (result.isFail()) {
			return result.data().orElseThrow();
		} else if (!checkOnly) {
			chunk.setForceLoadedTime(System.currentTimeMillis());
			NativeEventPosting.get().postEvent(new ChunkChangeEvent.Post.Data(source, chunk, ChunkChangeEvent.Operation.LOAD));
			chunk.getTeamData().markDirty();
			chunk.sendUpdateToAll();
		}

		return ClaimResult.success();
	}

	@Override
	public ClaimResult unForceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this
				&& !(adminOverride && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				&& !source.getServer().isSingleplayer()
				&& !(source.getEntity() instanceof ServerPlayer && isTeamMember(source.getEntity().getUUID()))
		) {
			return ClaimResult.StandardProblem.NOT_OWNER;
		} else if (!chunk.isForceLoaded()) {
			return ClaimResult.StandardProblem.NOT_LOADED;
		}

		DataOutcome<ClaimResult> result = ChunkChangeEvent.Pre.TYPE.post(new ChunkChangeEvent.Pre.Data(source, chunk, ChunkChangeEvent.Operation.UNLOAD));
		if (result.isFail()) {
			return result.data().orElseThrow();
		} else if (!checkOnly) {
			chunk.unload(source);
		}

		return ClaimResult.success();
	}

	public void markDirty() {
		shouldSave = true;
		team.markDirty();
	}

	@Override
	public boolean isTeamMember(UUID playerId) {
		return playerId.equals(getTeamId()) || team.getMembers().contains(playerId);
	}

	@Override
	public boolean isAlly(UUID playerId) {
		if (FTBChunksWorldConfig.ALLY_MODE.get() == AllyMode.FORCED_ALL || team.getRankForPlayer(playerId).isMemberOrBetter()) {
			return true;
		} else if (FTBChunksWorldConfig.ALLY_MODE.get() == AllyMode.FORCED_NONE) {
			return false;
		}

		return team.getRankForPlayer(playerId) == TeamRank.ALLY;
	}

	@Override
	public boolean canPlayerUse(ServerPlayer player, PrivacyProperty property) {
		PrivacyMode mode = team.getProperty(property);

		if (mode == PrivacyMode.PUBLIC) {
			return true;
		}

		if (Platform.get().misc().isFakePlayer(player)) {
			return canFakePlayerUse(player, mode);
		} else if (mode == PrivacyMode.ALLIES) {
			return isAlly(player.getUUID());
		} else {
			return team.getRankForPlayer(player.getUUID()).isMemberOrBetter();
		}
	}

	private boolean canFakePlayerUse(Player player, PrivacyMode mode) {
		if (team.getProperty(FTBChunksProperties.ALLOW_ALL_FAKE_PLAYERS)) {
			return mode == PrivacyMode.ALLIES;
		}

		boolean checkById = team.getProperty(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID);
		if (mode == PrivacyMode.ALLIES) {
			return checkById && isAlly(player.getUUID()) || fakePlayerMatches(player.getGameProfile());
		} else if (mode == PrivacyMode.PRIVATE) {
			return checkById && team.getRankForPlayer(player.getUUID()).isMemberOrBetter();
		}

		return false;
	}

	private boolean fakePlayerMatches(GameProfile profile) {
		return profile.name() != null && getCachedFakePlayerNames().contains(profile.name().toLowerCase(Locale.ROOT))
				|| profile.id() != null && getCachedFakePlayerNames().contains(profile.id().toString().toLowerCase(Locale.ROOT));
	}

	private Set<String> getCachedFakePlayerNames() {
		if (fakePlayerNameCache == null) {
			fakePlayerNameCache = team.getProperty(FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS).stream()
					.map(s -> s.toLowerCase(Locale.ROOT))
					.collect(Collectors.toSet());
		}
		return fakePlayerNameCache;
	}

	public void clearFakePlayerNameCache() {
		fakePlayerNameCache = null;
	}

	public Json5Object toJson() {
		Json5Object tag = new Json5Object();
		tag.addProperty("max_claim_chunks", getMaxClaimChunks());
		tag.addProperty("max_force_load_chunks", getMaxForceLoadChunks());
		if (extraClaimChunks > 0 && !team.isPartyTeam()) tag.addProperty("extra_claim_chunks", extraClaimChunks);
		if (extraForceLoadChunks > 0 && !team.isPartyTeam()) tag.addProperty("extra_force_load_chunks", extraForceLoadChunks);
		tag.addProperty("last_login_time", lastLoginTime);

		Json5Object chunksJson = new Json5Object();
		for (ClaimedChunkImpl chunk : getClaimedChunks()) {
			String key = chunk.getPos().dimension().identifier().toString();
			Json5Array chunksList = Json5Util.getJson5Array(chunksJson, key).orElse(new Json5Array());
			if (chunksList.isEmpty()) {
				chunksJson.add(key, chunksList);
			}
			chunksList.add(chunk.toJson());
		}
		tag.add("chunks", chunksJson);

		Json5Object members = new Json5Object();
		memberData.forEach((id, data) -> members.add(id.toString(), data.toJson()));
		if (!members.isEmpty()) {
			tag.add("member_data", members);
		}

		if (!preventedAccess.isEmpty()) {
			Json5Object p = new Json5Object();
			preventedAccess.forEach((id, element) -> p.add(id.toString(), PreventedAccess.CODEC.encodeStart(Json5Ops.INSTANCE, element).result().orElseThrow()));
			tag.add("prevented_access", p);
		}

		return tag;
	}

	public void deserializeJson(Json5Object json) {
		maxClaimChunks = Json5Util.getInt(json, "max_claim_chunks").orElse(-1);
		maxForceLoadChunks = Json5Util.getInt(json, "max_force_load_chunks").orElse(-1);
		extraClaimChunks = Json5Util.getInt(json, "extra_claim_chunks").orElse(0);
		extraForceLoadChunks = Json5Util.getInt(json, "extra_force_load_chunks").orElse(0);
		lastLoginTime = Json5Util.getLong(json, "last_login_time").orElse(0L);
		canForceLoadChunks = null;
		claimedChunkCache = null;
		forcedChunkCache = null;

		Json5Util.getJson5Object(json, "chunks").ifPresent(chunksJson ->
				chunksJson.asMap().forEach((key, el) -> {
					try {
						ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(key));
						Json5Array chunksListTag = el.getAsJson5Array();
						for (int i = 0; i < chunksListTag.size(); i++) {
							ClaimedChunkImpl chunk = ClaimedChunkImpl.fromJson(this, dimKey, chunksListTag.get(i).getAsJson5Object());
							manager.registerClaim(chunk.getPos(), chunk);
						}
					} catch (IdentifierException ex) {
						FTBChunks.LOGGER.error("ignoring bad dimension key {}", key);
					}
				}));

		memberData.clear();
		Json5Util.getJson5Object(json, "member_data").ifPresent(memberJson ->
				memberJson.asMap().forEach((key, el) -> {
					try {
						UUID id = UUID.fromString(key);
						if (id != Util.NIL_UUID) {
							memberData.put(id, TeamMemberData.fromJson(el.getAsJson5Object()));
						}
					} catch (IllegalArgumentException e) {
						FTBChunks.LOGGER.error("ignoring bad UUID {}", key);
					}
				}));

		preventedAccess.clear();
		Json5Util.getJson5Object(json, "prevented_access").ifPresent(accessJson -> {
			accessJson.asMap().forEach((key, el) ->
					preventedAccess.put(UUID.fromString(key), PreventedAccess.CODEC.parse(Json5Ops.INSTANCE, el).result().orElseThrow()));
			prunePreventedLog();
		});
	}

	@Override
	public int getExtraClaimChunks() {
		if (extraClaimChunks > 0 && team.isPartyTeam()) {
			FTBChunks.LOGGER.info("found non-zero extra_claim_chunks={} in party team {}: transferring to owner {}", extraClaimChunks, getTeamId(), team.getOwner());
			ChunkTeamDataImpl personalTeam = ClaimedChunkManagerImpl.getInstance().getPersonalData(team.getOwner());
			if (personalTeam != null) {
				personalTeam.extraClaimChunks = extraClaimChunks;
				extraClaimChunks = 0;
				markDirty();
				personalTeam.markDirty();
			}
		}
		return extraClaimChunks;
	}

	@Override
	public int getExtraForceLoadChunks() {
		if (extraForceLoadChunks > 0 && team.isPartyTeam()) {
			FTBChunks.LOGGER.info("found non-zero extra_force_load_chunks={} in party team {}: transferring to owner {}", extraForceLoadChunks, getTeamId(), team.getOwner());
			ChunkTeamDataImpl personalTeam = ClaimedChunkManagerImpl.getInstance().getPersonalData(team.getOwner());
			if (personalTeam != null) {
				personalTeam.extraForceLoadChunks = extraForceLoadChunks;
				extraForceLoadChunks = 0;
				markDirty();
				personalTeam.markDirty();
			}
		}
		return extraForceLoadChunks;
	}

	public boolean setForceLoadMember(UUID id, boolean val) {
		if (val == getTeamMemberData(id).isOfflineForceLoader()) {
			return false;
		}

		getTeamMemberData(id).setOfflineForceLoader(val);
		FTBChunks.LOGGER.debug("team {}: set force load member {} = {}", team.getId(), id, val);
		markDirty();
		canForceLoadChunks = null;
		manager.clearForceLoadedCache();
		return true;
	}

	private TeamMemberData getTeamMemberData(UUID memberId) {
		if (memberId.equals(Util.NIL_UUID)) {
			FTBChunks.LOGGER.warn("attempt to get member data for nil UUID in team {}", getTeamId());
			return TeamMemberData.defaultData();
		}
		return memberData.computeIfAbsent(memberId, ignored -> TeamMemberData.defaultData());
	}

	public void updateChunkTickets(boolean load) {
		getClaimedChunks().forEach(chunk -> {
			if (chunk.isForceLoaded()) {
				ServerLevel level = manager.getMinecraftServer().getLevel(chunk.getPos().dimension());
				if (level != null) {
					FTBChunksAPIImpl.INSTANCE.getForceLoadHandler()
							.updateForceLoadingForChunk(level, getTeamId(), chunk.getPos().x(), chunk.getPos().z(), load);
				}
			}
		});
	}

	public void saveNow() {
		if (shouldSave) {
			try {
				Json5Util.tryWrite(file, toJson());
				shouldSave = false;
			} catch (IOException e) {
				FTBChunks.LOGGER.error("Failed to save chunk team data for team {}", team.getId(), e);
			}
		}
	}

	@Override
	public boolean canDoOfflineForceLoading() {
		if (canForceLoadChunks == null) {
			canForceLoadChunks = switch (FTBChunksWorldConfig.FORCE_LOAD_MODE.get()) {
				case ALWAYS -> true;
				case NEVER -> false;
				default -> hasForceLoadMembers();
			};
		}
		return canForceLoadChunks;
	}

	private boolean hasForceLoadMembers() {
		return memberData.values().stream().anyMatch(TeamMemberData::isOfflineForceLoader);
	}

	@Override
	public boolean canExplosionsDamageTerrain() {
		return team.getProperty(FTBChunksProperties.ALLOW_EXPLOSIONS);
	}

	@Override
	public boolean allowMobGriefing() {
		return team.getProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING);
	}

	@Override
	public boolean allowPVP() {
		return team.getProperty(FTBChunksProperties.ALLOW_PVP);
	}

	public void setLastLoginTime(long when) {
		this.lastLoginTime = when;
		markDirty();
	}

	@Override
	public long getLastLoginTime() {
		if (lastLoginTime == 0L) {
			setLastLoginTime(System.currentTimeMillis());
		}
		return lastLoginTime;
	}

	@Override
	public boolean shouldHideClaims() {
		return getTeam().getProperty(FTBChunksProperties.CLAIM_VISIBILITY) != PrivacyMode.PUBLIC;
	}

	public void syncChunksToPlayer(ServerPlayer recipient) {
		chunksByDimension().forEach((dimension, chunkPackets) -> {
			if (!chunkPackets.isEmpty()) {
				new SendManyChunksPacket(dimension, getTeamId(), chunkPackets).sendToPlayer(recipient, this);
			}
		});
	}

	public void syncChunksToAll(MinecraftServer server) {
		chunksByDimension().forEach((dimension, chunkPackets) -> {
			if (!chunkPackets.isEmpty()) {
				new SendManyChunksPacket(dimension, getTeamId(), chunkPackets).sendToAll(server, this);
			}
		});
	}

	private Map<ResourceKey<Level>, List<ChunkSyncInfo>> chunksByDimension() {
		long now = System.currentTimeMillis();
		return getClaimedChunks().stream()
				.collect(Collectors.groupingBy(
						c -> c.getPos().dimension(), Collectors.mapping(c -> ChunkSyncInfo.create(now, c.getPos().x(), c.getPos().z(), c), Collectors.toList())
				));
	}

	@Override
	public int getMaxClaimChunks() {
		return maxClaimChunks;
	}

	@Override
	public int getMaxForceLoadChunks() {
		return maxForceLoadChunks;
	}

	public void updateLimits() {
		updateMemberLimitData(!memberData.isEmpty());

		int prevMaxClaimed = maxClaimChunks;
		int prevMaxForced = maxForceLoadChunks;

		if (!team.isPartyTeam()) {
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
					if (!memberData.isEmpty()) {
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

		if (maxClaimChunks != prevMaxClaimed || maxForceLoadChunks != prevMaxForced) {
			SendGeneralDataPacket.send(this, getTeam().getOnlineMembers());
		}

		markDirty();
	}

	private void updateMemberLimitData(boolean onlinePlayersOnly) {
		Set<UUID> members = new HashSet<>(team.getMembers());

		for (ServerPlayer p : team.getOnlineMembers()) {
			Team playerTeam = getTeamManager().getPlayerTeamForPlayerID(p.getUUID()).orElse(null);

			TeamMemberData m = getTeamMemberData(p.getUUID());
			if (playerTeam != null) {
				// pull limits in from the player's *personal* team data, if possible
				ChunkTeamDataImpl personalData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(playerTeam);
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

				Team playerTeam = getTeamManager().getPlayerTeamForPlayerID(id).orElse(null);
				int maxC, maxF;
				if (playerTeam != null) {
					ChunkTeamDataImpl personalData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(playerTeam);
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

		markDirty();
	}

	public void addMemberData(ServerPlayer player, ChunkTeamDataImpl otherTeam) {
		if (otherTeam.team.isPlayerTeam()) {
			memberData.put(otherTeam.getTeamId(), TeamMemberData.fromPlayerData(player, otherTeam));
			markDirty();
		}
	}

	public void deleteMemberData(UUID playerId) {
		if (memberData.remove(playerId) != null) {
			markDirty();
		}
	}

	private boolean hasChunkChanged(@Nullable ClaimedChunk chunk) {
		String s = chunk == null ? "-" : chunk.getTeamData().getTeam().getTeamId().toString();
		if (!lastChunkID.equals(s)) {
			lastChunkID = s;
			return true;
		}

		return false;
	}

	public void checkForChunkChange(Player player, int chunkX, int chunkZ) {
		if (prevChunkX != chunkX || prevChunkZ != chunkZ) {
			ClaimedChunk chunk = getManager().getChunk(new ChunkDimPos(player));
			if (hasChunkChanged(chunk)) {
				if (chunk != null) {
					player.sendOverlayMessage(chunk.getTeamData().getTeam().getColoredName());
				} else {
					player.sendOverlayMessage(Component.translatable("wilderness").withStyle(ChatFormatting.DARK_GREEN));
				}
			}
			prevChunkX = chunkX;
			prevChunkZ = chunkZ;
		}
	}

	public void clearClaimCaches() {
		claimedChunkCache = null;
		forcedChunkCache = null;
	}

	@Override
	public void checkMemberForceLoading(UUID playerId) {
		if (isTeamMember(playerId)) {
			ServerPlayer player = manager.getMinecraftServer().getPlayerList().getPlayer(playerId);
			if (player != null && setForceLoadMember(playerId, FTBChunksWorldConfig.canPlayerOfflineForceload(player))) {
				updateLimits();
			}
		}
	}

	public void logPreventedAccess(ServerPlayer player, long when) {
		preventedAccess.put(player.getUUID(), new PreventedAccess(player.getGameProfile().name(), when));
		markDirty();
	}

	private void prunePreventedLog() {
		Set<UUID> toRemove = new HashSet<>();
		long now = System.currentTimeMillis();
		long max = FTBChunksWorldConfig.MAX_PREVENTED_LOG_AGE.get() * 86400L * 1000L;
		preventedAccess.forEach((id, el) -> {
			if (now - el.when() > max) {
				toRemove.add(id);
			}
		});
		if (!toRemove.isEmpty()) {
			toRemove.forEach(preventedAccess::remove);
			markDirty();
		}
	}

	private record PreventedAccess(String name, long when) {
		public static final Codec<PreventedAccess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
						Codec.STRING.fieldOf("name").forGetter(PreventedAccess::name),
						Codec.LONG.fieldOf("when").forGetter(PreventedAccess::when)
				).apply(instance, PreventedAccess::new)
		);
	}
}
