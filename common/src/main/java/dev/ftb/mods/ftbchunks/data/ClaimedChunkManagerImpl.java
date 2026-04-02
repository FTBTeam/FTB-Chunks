package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.util.PlayerNotifier;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClaimedChunkManagerImpl implements ClaimedChunkManager {
	public static final LevelResource DATA_DIR = new LevelResource("ftbchunks");
	private static final Long2ObjectMap<UUID> EMPTY_CHUNKS = Long2ObjectMaps.emptyMap();

	@Nullable
	private static ClaimedChunkManagerImpl instance;

	private final TeamManager teamManager;
	private final Map<UUID, ChunkTeamDataImpl> teamData;
	private final Map<ChunkDimPos, ClaimedChunkImpl> claimedChunks;
	private final Path dataDirectory;
	@Nullable
	private Map<ResourceKey<Level>, Long2ObjectMap<UUID>> forceLoadedChunkCache;

	public ClaimedChunkManagerImpl(TeamManager teamManager) {
		this.teamManager = teamManager;

		teamData = new HashMap<>();
		claimedChunks = new HashMap<>();

		dataDirectory = getMinecraftServer().getWorldPath(DATA_DIR);
		Path localDirectory = Platform.get().paths().gamePath().resolve("local/ftbchunks");

		try {
			Files.createDirectories(dataDirectory);
			Files.createDirectories(localDirectory);
		} catch (Exception ex) {
			FTBChunks.LOGGER.error("failed to create {} or {}: {}", dataDirectory, localDirectory, ex.getMessage());
		}
	}

	public static boolean exists() {
		return instance != null;
	}

	public static ClaimedChunkManagerImpl getInstance() {
		return Objects.requireNonNull(instance);
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
		if (map.isEmpty()) {
			return;
		}

		map.forEach((pos, id) -> {
			ChunkPos chunkPos = ChunkPos.unpack(pos);
			FTBChunksAPIImpl.INSTANCE.getForceLoadHandler()
					.updateForceLoadingForChunk(level, id, chunkPos.x(), chunkPos.z(), true);
		});

		level.getChunkSource().save(false);

		FTBChunks.LOGGER.info("Force-loaded {} chunks in {}", map.size(), level.dimension().identifier());
	}

	public MinecraftServer getMinecraftServer() {
		return teamManager.getServer();
	}

	@Override
	public ChunkTeamDataImpl getOrCreateData(Team team) {
		ChunkTeamDataImpl data = teamData.get(team.getId());
		if (data == null) {
			data = ChunkTeamDataImpl.loadFromFile(this, dataDirectory, team);
			teamData.put(team.getId(), data);
		}

		return data;
	}

	@Override
	@Nullable
	public ChunkTeamDataImpl getPersonalData(UUID id) {
		return getTeamManager().getPlayerTeamForPlayerID(id)
				.map(this::getOrCreateData)
				.orElse(null);
	}

	@Override
	@Nullable
	public ChunkTeamDataImpl getOrCreateData(ServerPlayer player) {
		return getTeamManager().getTeamForPlayer(player)
				.map(this::getOrCreateData)
				.orElse(null);
	}

	public void deleteTeam(Team toDelete) {
		ChunkTeamDataImpl data = teamData.get(toDelete.getId());

		if (data != null && toDelete.getMembers().isEmpty()) {
			FTBChunks.LOGGER.debug("dropping references to empty team {}", toDelete.getId());
			teamData.remove(toDelete.getId());
			try {
				Files.deleteIfExists(data.getFile());
			} catch (IOException e) {
				FTBChunks.LOGGER.error("can't delete file {}: {}", data.getFile(), e.getMessage());
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
				.map(team -> team.getProperty(FTBChunksProperties.BYPASS_PROTECTION))
				.orElse(false);
	}

	@Override
	public void setBypassProtection(UUID player, boolean bypass) {
		teamManager.getPlayerTeamForPlayerID(player).ifPresent(team ->
				team.setProperty(FTBChunksProperties.BYPASS_PROTECTION, bypass));
	}

	@Override
	public boolean shouldPreventInteraction(@Nullable Entity actor, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity) {
		if (!(actor instanceof ServerPlayer player) || FTBChunksWorldConfig.DISABLE_PROTECTION.get() || getBypassProtection(player.getUUID())) {
			return false;
		}

		boolean isFake = Platform.get().misc().isFakePlayer(player);
		if (isFake && FTBChunksWorldConfig.ALLOW_FAKE_PLAYERS.get().isOverride()) {
			return FTBChunksWorldConfig.ALLOW_FAKE_PLAYERS.get().shouldPreventInteraction();
		}

		ClaimedChunkImpl chunk = getChunk(new ChunkDimPos(player.level(), pos));
		if (chunk != null) {
			ProtectionPolicy policy = protection.getProtectionPolicy(player, pos, hand, chunk, targetEntity);
			boolean prevented = policy.isOverride() ?
					policy.shouldPreventInteraction() :
					!player.isSpectator() && (isFake || !getBypassProtection(player.getUUID()));
			if (prevented) {
				PlayerNotifier.notifyWithCooldown(player, Component.translatable("ftbchunks.action_prevented").withStyle(ChatFormatting.GOLD), 2000);
				if (isFake) {
					chunk.getTeamData().logPreventedAccess(player, System.currentTimeMillis());
				}
			}
			return prevented;
		} else if (FTBChunksWorldConfig.noWilderness(player)) {
			ProtectionPolicy override = protection.getProtectionPolicy(player, pos, hand, null, targetEntity);
			if (override.isOverride()) {
				return override.shouldPreventInteraction();
			} else if (!isFake && (getBypassProtection(player.getUUID()) || player.isSpectator())) {
				return false;
			}
			PlayerNotifier.notifyWithCooldown(player, Component.translatable("ftbchunks.need_to_claim_chunk").withStyle(ChatFormatting.GOLD), 2000);
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
					Long2ObjectMap<UUID> pos2idMap = forceLoadedChunkCache.computeIfAbsent(chunk.getPos().dimension(), ignored -> new Long2ObjectOpenHashMap<>());
					pos2idMap.put(ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z()), chunk.getTeamData().getTeamId());
				}
			}

			forceLoadedChunkCache = forceLoadedChunkCache.isEmpty() ? Collections.emptyMap() : forceLoadedChunkCache;
		}

		return Collections.unmodifiableMap(forceLoadedChunkCache);
	}

	@Override
	public Long2ObjectMap<UUID> getForceLoadedChunks(ResourceKey<Level> dimension) {
		return getForceLoadedChunks().getOrDefault(dimension, EMPTY_CHUNKS);
	}

	@Override
	public boolean isChunkForceLoaded(ChunkDimPos chunkDimPos) {
		return getForceLoadedChunks(chunkDimPos.dimension()).containsKey(chunkDimPos.chunkPos().pack());
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
