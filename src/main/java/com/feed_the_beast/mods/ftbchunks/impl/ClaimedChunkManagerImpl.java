package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkManager;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkManagerImpl implements ClaimedChunkManager
{
	public static final FolderName DATA_DIR = new FolderName("data/ftbchunks");

	public final MinecraftServer server;
	public UUID serverId;
	public final Map<UUID, ClaimedChunkPlayerDataImpl> playerData;
	public final Map<ChunkDimPos, ClaimedChunkImpl> claimedChunks;
	public final Map<UUID, KnownFakePlayer> knownFakePlayers;
	public boolean saveFakePlayers;
	public Path dataDirectory;
	public Path localDirectory;
	private boolean inited;

	public ClaimedChunkManagerImpl(MinecraftServer s)
	{
		server = s;
		serverId = UUID.randomUUID();
		playerData = new HashMap<>();
		claimedChunks = new HashMap<>();
		knownFakePlayers = new HashMap<>();
		saveFakePlayers = false;
		inited = false;
	}

	public void init()
	{
		if (inited)
		{
			return;
		}

		inited = true;

		long nanos = System.nanoTime();
		dataDirectory = server.func_240776_a_(DATA_DIR);
		localDirectory = FMLPaths.GAMEDIR.get().resolve("local/ftbchunks");

		try
		{
			if (Files.notExists(dataDirectory))
			{
				Files.createDirectories(dataDirectory);
			}

			if (Files.notExists(localDirectory))
			{
				Files.createDirectories(localDirectory);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		Path infoFile = dataDirectory.resolve("info.json");

		if (Files.exists(infoFile))
		{
			try (Reader reader = Files.newBufferedReader(infoFile))
			{
				JsonObject json = new GsonBuilder().disableHtmlEscaping().create().fromJson(reader, JsonObject.class);
				serverId = UUID.fromString(json.get("id").getAsString());
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else
		{
			try (Writer writer = Files.newBufferedWriter(infoFile))
			{
				JsonObject json = new JsonObject();
				json.addProperty("id", serverId.toString());
				new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(json, writer);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		loadPlayerData();
		int forceLoaded = 0;

		for (ClaimedChunkImpl chunk : claimedChunks.values())
		{
			if (chunk.isForceLoaded() && chunk.getPlayerData().chunkLoadOffline())
			{
				forceLoaded++;
				chunk.postSetForceLoaded(true);
			}
		}

		FTBChunks.LOGGER.info("Server " + serverId + ": Loaded " + claimedChunks.size() + " chunks (" + forceLoaded + " force loaded) from " + playerData.size() + " players in " + ((System.nanoTime() - nanos) / 1000000D) + "ms");
		getServerData();
	}

	public void serverSaved()
	{
		for (ClaimedChunkPlayerDataImpl data : playerData.values())
		{
			if (data.shouldSave)
			{
				try (Writer writer = Files.newBufferedWriter(data.file))
				{
					FTBChunks.GSON.toJson(data.toJson(), writer);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}

				data.shouldSave = false;
			}
		}

		if (saveFakePlayers)
		{
			saveFakePlayers = false;

			JsonArray array = new JsonArray();

			for (KnownFakePlayer p : knownFakePlayers.values())
			{
				JsonObject json = new JsonObject();
				json.addProperty("uuid", UUIDTypeAdapter.fromUUID(p.uuid));
				json.addProperty("name", p.name);
				json.addProperty("banned", p.banned);
				array.add(json);
			}

			try (Writer writer = Files.newBufferedWriter(dataDirectory.resolve("known_fake_players.json")))
			{
				FTBChunks.GSON.toJson(array, writer);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private void loadPlayerData()
	{
		try
		{
			Files.list(dataDirectory).filter(path -> path.getFileName().toString().endsWith(".json") && !path.getFileName().toString().equals("info.json") && !path.getFileName().toString().equals("known_fake_players.json")).forEach(path -> {
				try
				{
					try (Reader reader = Files.newBufferedReader(path))
					{
						JsonObject json = FTBChunks.GSON.fromJson(reader, JsonObject.class);

						if (json == null || !json.has("name") || !json.has("uuid"))
						{
							return;
						}

						UUID id = UUIDTypeAdapter.fromString(json.get("uuid").getAsString());

						ClaimedChunkPlayerDataImpl data = new ClaimedChunkPlayerDataImpl(this, path, id);
						data.fromJson(json);
						playerData.put(id, data);
					}
				}
				catch (Exception ex)
				{
					FTBChunks.LOGGER.error("Failed to load " + path + ": " + ex + ". Deleting the file...");

					try
					{
						Files.delete(path);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		Path knownFakePlayersFile = dataDirectory.resolve("known_fake_players.json");

		if (Files.exists(knownFakePlayersFile))
		{
			try (Reader reader = Files.newBufferedReader(knownFakePlayersFile))
			{
				for (JsonElement e : FTBChunks.GSON.fromJson(reader, JsonArray.class))
				{
					JsonObject json = e.getAsJsonObject();
					UUID uuid = UUIDTypeAdapter.fromString(json.get("uuid").getAsString());
					String name = json.get("name").getAsString();
					boolean banned = json.get("banned").getAsBoolean();
					knownFakePlayers.put(uuid, new KnownFakePlayer(uuid, name, banned));
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	@Override
	public MinecraftServer getMinecraftServer()
	{
		return server;
	}

	@Override
	public UUID getServerId()
	{
		return serverId;
	}

	@Override
	public ClaimedChunkPlayerDataImpl getData(UUID id, String name)
	{
		ClaimedChunkPlayerDataImpl data = playerData.get(id);

		if (data == null)
		{
			data = new ClaimedChunkPlayerDataImpl(this, dataDirectory.resolve(UUIDTypeAdapter.fromUUID(id) + "-" + name + ".json"), id);
			data.profile = new GameProfile(id, name);
			playerData.put(id, data);
			data.save();
		}

		return data;
	}

	@Override
	public ClaimedChunkPlayerDataImpl getData(ServerPlayerEntity player)
	{
		return getData(player.getUniqueID(), player.getGameProfile().getName());
	}

	@Override
	public ClaimedChunkPlayerDataImpl getServerData()
	{
		return getData(SERVER_PLAYER_ID, "Server");
	}

	@Override
	@Nullable
	public ClaimedChunkImpl getChunk(ChunkDimPos pos)
	{
		return claimedChunks.get(pos);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<ClaimedChunk> getAllClaimedChunks()
	{
		return (Collection<ClaimedChunk>) (Collection) claimedChunks.values();
	}

	public static String prettyTimeString(long seconds)
	{
		if (seconds <= 0L)
		{
			return "0 seconds";
		}

		StringBuilder builder = new StringBuilder();
		prettyTimeString(builder, seconds, true);
		return builder.toString();
	}

	private static void prettyTimeString(StringBuilder builder, long seconds, boolean addAnother)
	{
		if (seconds <= 0L)
		{
			return;
		}
		else if (!addAnother)
		{
			builder.append(" and ");
		}

		if (seconds < 60L)
		{
			builder.append(seconds);
			builder.append(seconds == 1L ? " second" : " seconds");
		}
		else if (seconds < 3600L)
		{
			builder.append(seconds / 60L);
			builder.append(seconds / 60L == 1L ? " minute" : " minutes");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 60L, false);
			}
		}
		else if (seconds < 86400L)
		{
			builder.append(seconds / 3600L);
			builder.append(seconds / 3600L == 1L ? " hour" : " hours");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 3600L, false);
			}
		}
		else
		{
			builder.append(seconds / 86400L);
			builder.append(seconds / 86400L == 1L ? " day" : " days");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 86400L, false);
			}
		}
	}
}