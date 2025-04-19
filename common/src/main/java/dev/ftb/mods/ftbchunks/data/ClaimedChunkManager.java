package dev.ftb.mods.ftbchunks.data;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class ClaimedChunkManager {
	public static final LevelResource DATA_DIR = new LevelResource("ftbchunks");

	private static final Long2ObjectMap<UUID> EMPTY_CHUNKS = Long2ObjectMaps.emptyMap();

	private final TeamManager teamManager;

	private final Map<UUID, FTBChunksTeamData> teamData;
	private final Map<ChunkDimPos, ClaimedChunk> claimedChunks;
	private final Path dataDirectory;
	private Map<ResourceKey<Level>, Long2ObjectMap<UUID>> forceLoadedChunkCache;

	public ClaimedChunkManager(TeamManager m) {
		teamManager = m;
		teamData = new HashMap<>();
		claimedChunks = new HashMap<>();

		dataDirectory = getMinecraftServer().getWorldPath(DATA_DIR);
		Path localDirectory = Platform.getGameFolder().resolve("local/ftbchunks");

		try {
			if (Files.notExists(dataDirectory)) {
				Files.createDirectories(dataDirectory);
			}

			if (Files.notExists(localDirectory)) {
				Files.createDirectories(localDirectory);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void initForceLoadedChunks(ServerLevel level) {
		var map = getForceLoadedChunks(level.dimension());

		if (map.isEmpty() || level.getChunkSource() == null) {
			return;
		}

		map.forEach((pos, id) -> {
			ChunkPos chunkPos = new ChunkPos(pos);
			FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, id, chunkPos.x, chunkPos.z, true);
		});

		level.getChunkSource().save(false);

		FTBChunks.LOGGER.info("Force-loaded %d chunks in %s".formatted(map.size(), level.dimension().location()));
	}

	private FTBChunksTeamData loadTeamData(Team team) {
		Path path = dataDirectory.resolve(team.getId() + ".snbt");
		FTBChunksTeamData data = new FTBChunksTeamData(this, path, team);
		CompoundTag dataFile = SNBT.read(path);

		if (dataFile != null) {
			data.deserializeNBT(dataFile);
			teamData.put(team.getId(), data);
			return data;
		}

		return data;
	}

	public MinecraftServer getMinecraftServer() {
		return teamManager.server;
	}

	public FTBChunksTeamData getData(@Nullable Team team) {
		if (team == null) {
			throw new IllegalArgumentException("Team not found!");
		}

		FTBChunksTeamData data = teamData.get(team.getId());

		if (data == null) {
			data = loadTeamData(team);
			teamData.put(team.getId(), data);
		}

		return data;
	}

	public FTBChunksTeamData getPersonalData(UUID id) {
		Team team = FTBTeamsAPI.getManager().getInternalPlayerTeam(id);
		return team == null ? null : getData(team);
	}

	public FTBChunksTeamData getPersonalData(ServerPlayer player) {
		return getPersonalData(player.getUUID());
	}

	public FTBChunksTeamData getData(ServerPlayer player) {
		return getData(FTBTeamsAPI.getPlayerTeam(player));
	}

	public boolean hasData(ServerPlayer player) {
		Team team = FTBTeamsAPI.getManager().getPlayerTeam(player.getUUID());
		return team != null && teamData.containsKey(team.getId());
	}


	public void deleteTeam(Team toDelete) {
		FTBChunksTeamData data = teamData.get(toDelete.getId());

		if (data != null && toDelete.getMembers().isEmpty()) {
			FTBChunks.LOGGER.debug("dropping references to empty team " + toDelete.getId());
			teamData.remove(toDelete.getId());
			try {
				Files.deleteIfExists(data.file);
			} catch (IOException e) {
				FTBChunks.LOGGER.error(String.format("can't delete file %s: %s", data.file, e.getMessage()));
			}
		}
	}

	@Nullable
	public ClaimedChunk getChunk(ChunkDimPos pos) {
		return claimedChunks.get(pos);
	}

	public Collection<ClaimedChunk> getAllClaimedChunks() {
		return claimedChunks.values();
	}

	public Map<UUID,List<ClaimedChunk>> getClaimedChunksByTeam(Predicate<ClaimedChunk> predicate) {
		return getAllClaimedChunks().stream()
				.filter(predicate)
				.collect(Collectors.groupingBy(cc -> cc.teamData.getTeamId()));
	}

	public boolean getBypassProtection(UUID player) {
		PlayerTeam team = teamManager.getInternalPlayerTeam(player);
		return team != null && team.getExtraData().getBoolean("BypassFTBChunksProtection");
	}

	public void setBypassProtection(UUID player, boolean bypass) {
		PlayerTeam team = teamManager.getInternalPlayerTeam(player);
		if (team != null) {
			team.getExtraData().putBoolean("BypassFTBChunksProtection", bypass);
			team.save();
		}
	}

	/**
	 * Check if the intended interaction should be prevented from occurring.
	 *
	 * @param entity the entity performing the interaction
	 * @param hand the actor's hand
	 * @param pos the block position at which the action will be performed
	 * @param protection the type of protection being checked for
	 * @param targetEntity the entity being acted upon, if any (e.g. a painting, armor stand etc.)
	 * @return true to prevent the interaction, false to permit it
	 */
	public boolean protect(@Nullable Entity entity, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity) {
		return protect(entity, hand, pos, protection, targetEntity, false, null);
	}

	/**
	 * Check if the intended interaction should be prevented from occurring.
	 *
	 * @param entity the entity performing the interaction
	 * @param hand the actor's hand
	 * @param pos the block position at which the action will be performed
	 * @param protection the type of protection being checked for
	 * @param targetEntity the entity being acted upon, if any (e.g. a painting, armor stand etc.)
	 * @param protectIfNullEntity if true, and entity = null, will return true
	 * @return true to prevent the interaction, false to permit it
	 */
	public boolean protect(@Nullable Entity entity, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity,
			boolean protectIfNullEntity, @Nullable Level level) {
		// this block may not be needed if create uses fake players instead
		// ********
		if (protectIfNullEntity && entity == null && level != null && FTBChunksWorldConfig.PROTECT_UNKNOWN_BLOCK_BREAKER.get()) {
			ClaimedChunk chunk = getChunk(new ChunkDimPos(level, pos));
			if (chunk == null) {
				return false;
			}
			if (FTBChunksWorldConfig.ALLOW_UNKNOWN_BREAKS_IN_FORCE_LOADS.get()) {
				return !chunk.isActuallyForceLoaded();
			}
			return true;
		}
		// ********
		if (!(entity instanceof ServerPlayer player) || FTBChunksWorldConfig.DISABLE_PROTECTION.get() || player.level == null) {
			return false;
		}

		boolean isFake = PlayerHooks.isFake(player);

		if (isFake && FTBChunksWorldConfig.FAKE_PLAYERS.get().isOverride()) {
			return FTBChunksWorldConfig.FAKE_PLAYERS.get().getProtect();
		}

		ClaimedChunk chunk = getChunk(new ChunkDimPos(player.level, pos));

		if (chunk != null) {
			ProtectionOverride override = protection.override(player, pos, hand, chunk, targetEntity);

			if (override.isOverride()) {
				return override.getProtect();
			}

			return !player.isSpectator() && (isFake || !getBypassProtection(player.getUUID()));
		} else if (FTBChunksWorldConfig.noWilderness(player)) {
			ProtectionOverride override = protection.override(player, pos, hand, null, targetEntity);

			if (override.isOverride()) {
				return override.getProtect();
			} else if (!isFake && (getBypassProtection(player.getUUID()) || player.isSpectator())) {
				return false;
			}

			player.displayClientMessage(Component.literal("You need to claim this chunk to interact with blocks here!"), true);
			return true;
		}

		return false;
	}

	public void clearForceLoadedCache() {
		forceLoadedChunkCache = null;
	}

	public Map<ResourceKey<Level>,Long2ObjectMap<UUID>> getForceLoadedChunks() {
		if (forceLoadedChunkCache == null) {
			forceLoadedChunkCache = new HashMap<>();

			for (ClaimedChunk chunk : getAllClaimedChunks()) {
				if (chunk.isActuallyForceLoaded()) {
					Long2ObjectMap<UUID> pos2idMap = forceLoadedChunkCache.computeIfAbsent(chunk.pos.dimension, k -> new Long2ObjectOpenHashMap<>());
					pos2idMap.put(ChunkPos.asLong(chunk.pos.x, chunk.pos.z), chunk.teamData.getTeamId());
				}
			}

			forceLoadedChunkCache = forceLoadedChunkCache.isEmpty() ? Collections.emptyMap() : forceLoadedChunkCache;
		}

		return forceLoadedChunkCache;
	}

	@NotNull
	public Long2ObjectMap<UUID> getForceLoadedChunks(ResourceKey<Level> dimension) {
		return getForceLoadedChunks().getOrDefault(dimension, EMPTY_CHUNKS);
	}

	public boolean isChunkForceLoaded(ResourceKey<Level> dimension, int x, int z) {
		return getForceLoadedChunks(dimension).containsKey(ChunkPos.asLong(x, z));
	}

	public void registerClaim(ChunkDimPos pos, ClaimedChunk chunk) {
		claimedChunks.put(pos, chunk);
	}

	public void unregisterClaim(ChunkDimPos pos) {
		claimedChunks.remove(pos);
	}

	public Collection<FTBChunksTeamData> getAllTeamData() {
		return teamData.values();
	}
}
