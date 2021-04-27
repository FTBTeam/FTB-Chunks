package dev.ftb.mods.ftbchunks.data;

import com.google.gson.JsonObject;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
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

	public final MinecraftServer server;
	public final TeamManager teamManager;
	public final FTBChunksWorldConfig config;

	public final Map<UUID, ClaimedChunkTeamData> teamData;
	public final Map<ChunkDimPos, ClaimedChunk> claimedChunks;
	public Path dataDirectory;
	public Path localDirectory;

	public ClaimedChunkManager(TeamManager m) {
		server = m.server;
		teamManager = m;
		config = FTBChunksWorldConfig.init(server);
		teamData = new HashMap<>();
		claimedChunks = new HashMap<>();

		dataDirectory = server.getWorldPath(DATA_DIR);
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
			if (chunk.isForceLoaded() && chunk.getPlayerData().chunkLoadOffline()) {
				forceLoaded++;
				chunk.postSetForceLoaded(true);
			}
		}

		FTBChunks.LOGGER.info("Server " + teamManager.getId() + ": Loaded " + claimedChunks.size() + " chunks (" + forceLoaded + " force loaded) from " + teamData.size() + " teams in " + ((System.nanoTime() - nanos) / 1000000D) + "ms");
	}

	public ClaimedChunkTeamData loadTeamData(Team team) {
		Path path = dataDirectory.resolve(UUIDTypeAdapter.fromUUID(team.getId()) + ".json");
		ClaimedChunkTeamData data = new ClaimedChunkTeamData(this, path, team);

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				JsonObject json = FTBChunks.GSON.fromJson(reader, JsonObject.class);
				data.fromJson(json);
				teamData.put(team.getId(), data);

				for (ClaimedChunk chunk : data.getClaimedChunks()) {
					if (chunk.isForceLoaded() && chunk.getPlayerData().chunkLoadOffline()) {
						chunk.postSetForceLoaded(true);
					}
				}

				return data;
			} catch (Exception ex) {
				FTBChunks.LOGGER.error("Failed to load " + path + ": " + ex + ". Deleting the file...");

				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return data;
	}

	public MinecraftServer getMinecraftServer() {
		return server;
	}

	public ClaimedChunkTeamData getData(@Nullable Team team) {
		if (team == null) {
			throw new IllegalArgumentException("Team not found!");
		}

		ClaimedChunkTeamData data = teamData.get(team.getId());

		if (data == null) {
			data = loadTeamData(team);
		}

		return data;
	}

	public ClaimedChunkTeamData getData(ServerPlayer player) {
		return getData(FTBTeamsAPI.getPlayerTeam(player));
	}

	@Nullable
	public ClaimedChunk getChunk(ChunkDimPos pos) {
		return claimedChunks.get(pos);
	}

	public Collection<ClaimedChunk> getAllClaimedChunks() {
		return claimedChunks.values();
	}
}