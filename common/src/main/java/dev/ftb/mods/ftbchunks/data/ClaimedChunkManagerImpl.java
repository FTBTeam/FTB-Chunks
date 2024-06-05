package dev.ftb.mods.ftbchunks.data;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.api.ProtectionPolicy;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
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

public class ClaimedChunkManagerImpl implements ClaimedChunkManager {
	public static final LevelResource DATA_DIR = new LevelResource("ftbchunks");
	private static final Long2ObjectMap<UUID> EMPTY_CHUNKS = Long2ObjectMaps.emptyMap();
	protected static final String BYPASS_FTB_CHUNKS_PROTECTION = "BypassFTBChunksProtection";

	private static ClaimedChunkManagerImpl instance;

	private final TeamManager teamManager;
	private final Map<UUID, ChunkTeamDataImpl> teamData;
	private final Map<ChunkDimPos, ClaimedChunkImpl> claimedChunks;
	private final Path dataDirectory;
	private Map<ResourceKey<Level>, Long2ObjectMap<UUID>> forceLoadedChunkCache;

	public ClaimedChunkManagerImpl(TeamManager teamManager) {
		this.teamManager = teamManager;

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

	public static ClaimedChunkManagerImpl getInstance() {
		return instance;
	}

	public static void init(TeamManager teamManager) {
		instance = new ClaimedChunkManagerImpl(teamManager);
	}

	public static void shutdown() {
		instance = null;
	}

	public TeamManager getTeamManager() {
		return teamManager;
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

	private ChunkTeamDataImpl loadTeamData(Team team) {
		Path path = dataDirectory.resolve(team.getId() + ".snbt");
		ChunkTeamDataImpl data = new ChunkTeamDataImpl(this, path, team);
		CompoundTag dataFile = SNBT.read(path);

		if (dataFile != null) {
			data.deserializeNBT(dataFile);
			teamData.put(team.getId(), data);
			return data;
		}

		return data;
	}

	public MinecraftServer getMinecraftServer() {
		return teamManager.getServer();
	}

	@Override
	public ChunkTeamDataImpl getOrCreateData(@NotNull Team team) {
		ChunkTeamDataImpl data = teamData.get(team.getId());
		if (data == null) {
			data = loadTeamData(team);
			teamData.put(team.getId(), data);
		}

		return data;
	}

	@Override
	public ChunkTeamDataImpl getPersonalData(UUID id) {
		return getTeamManager().getPlayerTeamForPlayerID(id)
				.map(this::getOrCreateData)
				.orElse(null);
	}

	@Override
	public ChunkTeamDataImpl getOrCreateData(ServerPlayer player) {
		return getTeamManager().getTeamForPlayer(player)
				.map(this::getOrCreateData)
				.orElse(null);
	}

	public void deleteTeam(Team toDelete) {
		ChunkTeamDataImpl data = teamData.get(toDelete.getId());

		if (data != null && toDelete.getMembers().isEmpty()) {
			FTBChunks.LOGGER.debug("dropping references to empty team " + toDelete.getId());
			teamData.remove(toDelete.getId());
			try {
				Files.deleteIfExists(data.getFile());
			} catch (IOException e) {
				FTBChunks.LOGGER.error(String.format("can't delete file %s: %s", data.getFile(), e.getMessage()));
			}
		}
	}

	@Nullable
	@Override
	public ClaimedChunkImpl getChunk(ChunkDimPos pos) {
		return claimedChunks.get(pos);
	}

	@Override
	public Collection<ClaimedChunkImpl> getAllClaimedChunks() {
		return Collections.unmodifiableCollection(claimedChunks.values());
	}

	@Override
	public Map<UUID, Collection<ClaimedChunk>> getClaimedChunksByTeam(Predicate<ClaimedChunk> predicate) {
		return Collections.unmodifiableMap(getAllClaimedChunks().stream()
				.filter(predicate)
				.collect(Collectors.groupingBy(cc -> cc.getTeamData().getTeam().getId()))
		);
	}

	@Override
	public boolean getBypassProtection(UUID player) {
		return teamManager.getPlayerTeamForPlayerID(player)
				.map(team -> team.getExtraData().getBoolean(BYPASS_FTB_CHUNKS_PROTECTION))
				.orElse(false);
		}

	@Override
	public void setBypassProtection(UUID player, boolean bypass) {
		teamManager.getPlayerTeamForPlayerID(player).ifPresent(team -> {
			team.getExtraData().putBoolean(BYPASS_FTB_CHUNKS_PROTECTION, bypass);
			team.markDirty();
		});
	}

	@Override
	public boolean shouldPreventInteraction(@Nullable Entity actor, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity) {
		if (!(actor instanceof ServerPlayer player) || FTBChunksWorldConfig.DISABLE_PROTECTION.get() || player.level() == null) {
			return false;
		}

		boolean isFake = PlayerHooks.isFake(player);
		if (isFake && FTBChunksWorldConfig.ALLOW_FAKE_PLAYERS.get().isOverride()) {
			return FTBChunksWorldConfig.ALLOW_FAKE_PLAYERS.get().shouldPreventInteraction();
		}

		ClaimedChunkImpl chunk = getChunk(new ChunkDimPos(player.level(), pos));
		if (chunk != null) {
			ProtectionPolicy policy = protection.getProtectionPolicy(player, pos, hand, chunk, targetEntity);
			boolean prevented = policy.isOverride() ?
					policy.shouldPreventInteraction() :
					!player.isSpectator() && (isFake || !getBypassProtection(player.getUUID()));
			if (prevented && isFake) {
				chunk.getTeamData().logPreventedAccess(player, System.currentTimeMillis());
			}
			return prevented;
		} else if (FTBChunksWorldConfig.noWilderness(player)) {
			ProtectionPolicy override = protection.getProtectionPolicy(player, pos, hand, null, targetEntity);
			if (override.isOverride()) {
				return override.shouldPreventInteraction();
			} else if (!isFake && (getBypassProtection(player.getUUID()) || player.isSpectator())) {
				return false;
			}
			player.displayClientMessage(Component.translatable("ftbchunks.need_to_claim_chunk"), true);
			return true;
		}

		return false;
	}

	public void clearForceLoadedCache() {
		forceLoadedChunkCache = null;
	}

	@Override
	public Map<ResourceKey<Level>,Long2ObjectMap<UUID>> getForceLoadedChunks() {
		if (forceLoadedChunkCache == null) {
			forceLoadedChunkCache = new HashMap<>();

			for (ClaimedChunkImpl chunk : getAllClaimedChunks()) {
				if (chunk.isActuallyForceLoaded()) {
					Long2ObjectMap<UUID> pos2idMap = forceLoadedChunkCache.computeIfAbsent(chunk.getPos().dimension(), k -> new Long2ObjectOpenHashMap<>());
					pos2idMap.put(ChunkPos.asLong(chunk.getPos().x(), chunk.getPos().z()), chunk.getTeamData().getTeamId());
				}
			}

			forceLoadedChunkCache = forceLoadedChunkCache.isEmpty() ? Collections.emptyMap() : forceLoadedChunkCache;
		}

		return Collections.unmodifiableMap(forceLoadedChunkCache);
	}

	@NotNull
	@Override
	public Long2ObjectMap<UUID> getForceLoadedChunks(ResourceKey<Level> dimension) {
		return getForceLoadedChunks().getOrDefault(dimension, EMPTY_CHUNKS);
	}

	@Override
	public boolean isChunkForceLoaded(ChunkDimPos chunkDimPos) {
		return getForceLoadedChunks(chunkDimPos.dimension()).containsKey(chunkDimPos.chunkPos().toLong());
	}

	public void registerClaim(ChunkDimPos pos, ClaimedChunk chunk) {
		if (chunk instanceof ClaimedChunkImpl impl) {
			claimedChunks.put(pos, impl);
		}
	}

	public void unregisterClaim(ChunkDimPos pos) {
		claimedChunks.remove(pos);
	}
}
