package dev.ftb.mods.ftbchunks.data;

import com.google.gson.JsonObject;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.io.FilenameUtils;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.resources.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
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
	public Path levelDataDirectory;

	public ClaimedChunkManager(TeamManager m) {
		server = m.server;
		teamManager = m;
		config = FTBChunksWorldConfig.init(server);
		teamData = new HashMap<>();
		claimedChunks = new HashMap<>();

		dataDirectory = server.getWorldPath(DATA_DIR);
		localDirectory = Platform.getGameFolder().resolve("local/ftbchunks");
        levelDataDirectory = server.getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent();
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
	
	public Collection<ClaimedChunk> getAllClaimedChunksForLevel(ResourceKey<Level> level) {
		return claimedChunks.entrySet().stream().filter(map->map.getKey().dimension.equals(level))
			.map(map-> map.getValue()).collect(Collectors.toList());
	}
	public Collection<String> getNamesOfRegionFilesWithClaimedChunks(ResourceKey<Level> level){
		java.util.HashSet<String> namesOfRegionFilesWithClaimedChunks = new  java.util.HashSet<String>();
		
		for (ClaimedChunk claimedChunk : this.getAllClaimedChunksForLevel(level)) {	
			XZ region = XZ.regionFromChunk(claimedChunk.getPos().getChunkPos());
			namesOfRegionFilesWithClaimedChunks.add("r."+ region.x +"."+ region.z+ ".mca");
		}
		return new ArrayList<String>(namesOfRegionFilesWithClaimedChunks);
	}

	public boolean pruneRegionFiles(String fromPath,@Nullable String toPath, Collection<String> filterFileNames, boolean doBackup){
			
		try(java.util.stream.Stream<Path> stream =  Files.list(Paths.get(fromPath))){
			
			FTBChunks.LOGGER.info("Pruning from: " + fromPath);
			
			Set<String> regionFileNames = stream.filter(path -> !Files.isDirectory(path))
			.map(Path::getFileName)
			.map(Path::toString)
			.filter(name-> "mca".equals(FilenameUtils.getExtension(name)))
			.collect(Collectors.toSet());
			
			regionFileNames.removeAll(filterFileNames);

			if(doBackup){
				Files.createDirectories(Paths.get(toPath));
				for(String filename : regionFileNames){
					Files.move(Paths.get(fromPath + filename), Paths.get(toPath + filename), StandardCopyOption.REPLACE_EXISTING);
					FTBChunks.LOGGER.info("Moved file: "+filename+ " to " + toPath + filename);
				}	
			}
			else{
				for(String filename : regionFileNames){
					Files.delete(Paths.get(fromPath + filename));
					FTBChunks.LOGGER.info("Removed file: "+filename);
				}	
			}
		}
		catch(Throwable ex){
			FTBChunks.LOGGER.error("Faild to prune files "+ex);
			return false;
		}
		return true;       
	}
}