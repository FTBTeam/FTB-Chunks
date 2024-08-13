package dev.ftb.mods.ftbchunks.data;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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
	private Boolean canForceLoadChunks;
	private final Map<UUID,TeamMemberData> memberData;

	private int prevChunkX = Integer.MAX_VALUE;
	private int prevChunkZ = Integer.MAX_VALUE;
	private String lastChunkID = "";
	private long lastLoginTime;
	private Set<String> fakePlayerNameCache;

	private Collection<ClaimedChunkImpl> claimedChunkCache;
	private Collection<ClaimedChunkImpl> forcedChunkCache;

	private final Map<UUID,PreventedAccess> preventedAccess = new HashMap<>();

	public ChunkTeamDataImpl(ClaimedChunkManagerImpl manager, Path file, Team team) {
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
				res.add(manager.getChunk(cdp));
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
		ClaimResult result = ClaimedChunkEvent.BEFORE_CLAIM.invoker().before(source, chunk).object();
		if (result == null) {
			result = chunk;
		}

		if (checkOnly || !result.isSuccess()) {
			return result;
		}

		chunk.setClaimedTime(System.currentTimeMillis());
		manager.registerClaim(pos, chunk);
		ClaimedChunkEvent.AFTER_CLAIM.invoker().after(source, chunk);
		markDirty();
		return chunk;
	}

	@Override
	public ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this && !(adminOverride && source.hasPermission(Commands.LEVEL_GAMEMASTERS)) && !source.getServer().isSingleplayer()) {
            return ClaimResult.StandardProblem.NOT_OWNER;
        }

		ClaimResult result = ClaimedChunkEvent.BEFORE_UNCLAIM.invoker().before(source, chunk).object();
		if (result == null) {
			result = chunk;
		}

		if (checkOnly || !result.isSuccess()) {
			return result;
		}

		chunk.unclaim(source, true);
		markDirty();
		return chunk;
	}

	@Override
	public ClaimResult forceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this && !(adminOverride && source.hasPermission(Commands.LEVEL_GAMEMASTERS)) && !source.getServer().isSingleplayer()) {
			return ClaimResult.StandardProblem.NOT_OWNER;
		} else if (chunk.isForceLoaded()) {
			return ClaimResult.StandardProblem.ALREADY_LOADED;
		} else if (!team.isServerTeam() && getForceLoadedChunks().size() >= getMaxForceLoadChunks()) {
			return ClaimResult.StandardProblem.NOT_ENOUGH_POWER;
		}

		ClaimResult result = ClaimedChunkEvent.BEFORE_LOAD.invoker().before(source, chunk).object();
		if (result == null) {
			result = chunk;
		}
		if (checkOnly || !result.isSuccess()) {
			return result;
		}

		chunk.setForceLoadedTime(System.currentTimeMillis());
		ClaimedChunkEvent.AFTER_LOAD.invoker().after(source, chunk);
		chunk.getTeamData().markDirty();
		chunk.sendUpdateToAll();
		return chunk;
	}

	@Override
	public ClaimResult unForceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly, boolean adminOverride) {
		ClaimedChunkImpl chunk = manager.getChunk(pos);

		if (chunk == null) {
			return ClaimResult.StandardProblem.NOT_CLAIMED;
		} else if (chunk.getTeamData() != this
				&& !(adminOverride && source.hasPermission(Commands.LEVEL_GAMEMASTERS))
				&& !source.getServer().isSingleplayer()
				&& !(source.getEntity() instanceof ServerPlayer && isTeamMember(source.getEntity().getUUID()))
		) {
			return ClaimResult.StandardProblem.NOT_OWNER;
		} else if (!chunk.isForceLoaded()) {
			return ClaimResult.StandardProblem.NOT_LOADED;
		}

		ClaimResult result = ClaimedChunkEvent.BEFORE_UNLOAD.invoker().before(source, chunk).object();
		if (result == null) {
			result = chunk;
		}
		if (checkOnly || !result.isSuccess()) {
			return result;
		}

		chunk.unload(source);
		return chunk;
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

		if (PlayerHooks.isFake(player)) {
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

		boolean checkById = team.getProperty(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID) && player.getUUID() != null;
		if (mode == PrivacyMode.ALLIES) {
			return checkById && isAlly(player.getUUID()) || fakePlayerMatches(player.getGameProfile());
		} else if (mode == PrivacyMode.PRIVATE) {
			return checkById && team.getRankForPlayer(player.getUUID()).isMemberOrBetter();
		}

		return false;
	}

	private boolean fakePlayerMatches(GameProfile profile) {
		return profile.getName() != null && getCachedFakePlayerNames().contains(profile.getName().toLowerCase(Locale.ROOT))
				|| profile.getId() != null && getCachedFakePlayerNames().contains(profile.getId().toString().toLowerCase(Locale.ROOT));
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

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.putInt("max_claim_chunks", getMaxClaimChunks());
		tag.putInt("max_force_load_chunks", getMaxForceLoadChunks());
		if (extraClaimChunks > 0 && !team.isPartyTeam()) tag.putInt("extra_claim_chunks", extraClaimChunks);
		if (extraForceLoadChunks > 0 && !team.isPartyTeam()) tag.putInt("extra_force_load_chunks", extraForceLoadChunks);
		tag.putLong("last_login_time", lastLoginTime);

		CompoundTag chunksTag = new CompoundTag();
		for (ClaimedChunkImpl chunk : getClaimedChunks()) {
			String key = chunk.getPos().dimension().location().toString();
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

		if (!preventedAccess.isEmpty()) {
			SNBTCompoundTag p = new SNBTCompoundTag();
			preventedAccess.forEach((id, element) -> p.put(id.toString(), PreventedAccess.CODEC.encodeStart(NbtOps.INSTANCE, element).result().orElseThrow()));
			tag.put("prevented_access", p);
		}

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		maxClaimChunks = tag.getInt("max_claim_chunks");
		maxForceLoadChunks = tag.getInt("max_force_load_chunks");
		extraClaimChunks = tag.getInt("extra_claim_chunks");
		extraForceLoadChunks = tag.getInt("extra_force_load_chunks");
		lastLoginTime = tag.getLong("last_login_time");
		canForceLoadChunks = null;
		claimedChunkCache = null;
		forcedChunkCache = null;

		CompoundTag chunksTag = tag.getCompound("chunks");

		for (String key : chunksTag.getAllKeys()) {
			ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(key));
			ListTag chunksListTag = chunksTag.getList(key, Tag.TAG_COMPOUND);

			for (int i = 0; i < chunksListTag.size(); i++) {
				ClaimedChunkImpl chunk = ClaimedChunkImpl.deserializeNBT(this, dimKey, chunksListTag.getCompound(i));
				manager.registerClaim(chunk.getPos(), chunk);
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

		preventedAccess.clear();
		if (tag.contains("prevented_access")) {
			CompoundTag p = tag.getCompound("prevented_access");
			for (String key : p.getAllKeys()) {
				preventedAccess.put(UUID.fromString(key), PreventedAccess.CODEC.parse(NbtOps.INSTANCE, p.getCompound(key)).result().orElseThrow());
			}
			prunePreventedLog();
		}
	}

	@Override
	public int getExtraClaimChunks() {
		if (extraClaimChunks > 0 && team.isPartyTeam()) {
			FTBChunks.LOGGER.info("found non-zero extra_claim_chunks={} in party team {}: transferring to owner {}", extraClaimChunks, getTeamId(), team.getOwner());
			ChunkTeamDataImpl personalTeam = ClaimedChunkManagerImpl.getInstance().getPersonalData(team.getOwner());
			personalTeam.extraClaimChunks = extraClaimChunks;
			extraClaimChunks = 0;
			markDirty();
			personalTeam.markDirty();
		}
		return extraClaimChunks;
	}

	@Override
	public int getExtraForceLoadChunks() {
		if (extraForceLoadChunks > 0 && team.isPartyTeam()) {
			FTBChunks.LOGGER.info("found non-zero extra_force_load_chunks={} in party team {}: transferring to owner {}", extraForceLoadChunks, getTeamId(), team.getOwner());
			ChunkTeamDataImpl personalTeam = ClaimedChunkManagerImpl.getInstance().getPersonalData(team.getOwner());
			personalTeam.extraForceLoadChunks = extraForceLoadChunks;
			extraForceLoadChunks = 0;
			markDirty();
			personalTeam.markDirty();
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
				ServerLevel level = manager.getMinecraftServer().getLevel(chunk.getPos().dimension());
				if (level != null) {
					FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, getTeamId(), chunk.getPos().x(), chunk.getPos().z(), load);
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

	private boolean hasChunkChanged(ClaimedChunkImpl chunk) {
		String s = chunk == null ? "-" : chunk.getTeamData().getTeamId().toString();
		if (!lastChunkID.equals(s)) {
			lastChunkID = s;
			return true;
		}

		return false;
	}

	public void checkForChunkChange(Player player, int chunkX, int chunkZ) {
		if (prevChunkX != chunkX || prevChunkZ != chunkZ) {
			ClaimedChunkImpl chunk = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(player));
			if (hasChunkChanged(chunk)) {
				if (chunk != null) {
					player.displayClientMessage(chunk.getTeamData().getTeam().getColoredName(), true);
				} else {
					player.displayClientMessage(Component.translatable("wilderness").withStyle(ChatFormatting.DARK_GREEN), true);
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
		preventedAccess.put(player.getUUID(), new PreventedAccess(player.getGameProfile().getName(), when));
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
