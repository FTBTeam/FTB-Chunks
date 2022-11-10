package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import me.shedaniel.architectury.hooks.PlayerHooks;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkManager {
	public static final LevelResource DATA_DIR = LevelResourceHooks.create("ftbchunks");

	public final TeamManager teamManager;

	public final Map<UUID, FTBChunksTeamData> teamData;
	public final Map<ChunkDimPos, ClaimedChunk> claimedChunks;
	public Path dataDirectory;
	public Path localDirectory;

	public ClaimedChunkManager(TeamManager m) {
		teamManager = m;
		teamData = new HashMap<>();
		claimedChunks = new HashMap<>();

		dataDirectory = getMinecraftServer().getWorldPath(DATA_DIR);
		localDirectory = Platform.getGameFolder().resolve("local/ftbchunks");

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

	public void init() {
		long nanos = System.nanoTime();

		int forceLoaded = 0;

		for (ClaimedChunk chunk : claimedChunks.values()) {
			if (chunk.isForceLoaded() && chunk.getTeamData().getChunkLoadOffline()) {
				forceLoaded++;
				chunk.postSetForceLoaded(true);
			}
		}

		FTBChunks.LOGGER.info("Server " + teamManager.getId() + ": Loaded " + claimedChunks.size() + " chunks (" + forceLoaded + " force loaded) from " + teamData.size() + " teams in " + ((System.nanoTime() - nanos) / 1000000D) + "ms");
	}

	private FTBChunksTeamData loadTeamData(Team team) {
		Path path = dataDirectory.resolve(team.getId() + ".snbt");
		FTBChunksTeamData data = new FTBChunksTeamData(this, path, team);
		CompoundTag dataFile = SNBT.read(path);

		if (dataFile != null) {
			data.deserializeNBT(dataFile);
			teamData.put(team.getId(), data);

			for (ClaimedChunk chunk : data.getClaimedChunks()) {
				if (chunk.isForceLoaded() && chunk.getTeamData().getChunkLoadOffline()) {
					chunk.postSetForceLoaded(true);
				}
			}

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

	public boolean protect(@Nullable Entity entity, InteractionHand hand, BlockPos pos, Protection protection) {
		if (!(entity instanceof ServerPlayer) || FTBChunksWorldConfig.DISABLE_PROTECTION.get()) {
			return false;
		}

		ServerPlayer player = (ServerPlayer) entity;
		boolean isFake = PlayerHooks.isFake(player);

		if (isFake && FTBChunksWorldConfig.FAKE_PLAYERS.get().isOverride()) {
			return FTBChunksWorldConfig.FAKE_PLAYERS.get().getProtect();
		}

		ClaimedChunk chunk = getChunk(new ChunkDimPos(player.level, pos));

		if (chunk != null) {
			ProtectionOverride override = protection.override(player, pos, hand, chunk);

			if (override.isOverride()) {
				return override.getProtect();
			}

			return isFake || !getBypassProtection(player.getUUID());
		} else if (FTBChunksWorldConfig.noWilderness(player)) {
			ProtectionOverride override = protection.override(player, pos, hand, null);

			if (override.isOverride()) {
				return override.getProtect();
			} else if (!isFake && getBypassProtection(player.getUUID())) {
				return false;
			}

			player.displayClientMessage(new TextComponent("You need to claim this chunk to interact with blocks here!"), true);
			return true;
		}

		return false;
	}
}