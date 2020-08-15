package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.loading.FMLPaths;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkManagerImpl implements ClaimedChunkManager
{
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();
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

	public void init(ServerWorld world)
	{
		if (inited)
		{
			return;
		}

		inited = true;

		long nanos = System.nanoTime();
		dataDirectory = world.getServer().func_240776_a_(DATA_DIR);
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
			if (chunk.isForceLoaded())
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
					ClaimedChunkManagerImpl.GSON.toJson(data.toJson(), writer);
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
				ClaimedChunkManagerImpl.GSON.toJson(array, writer);
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
						JsonObject json = GSON.fromJson(reader, JsonObject.class);

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
				for (JsonElement e : GSON.fromJson(reader, JsonArray.class))
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

	public void exportJson()
	{
		JsonObject json = new JsonObject();

		JsonObject playersJson = new JsonObject();

		for (ClaimedChunkPlayerDataImpl data : playerData.values())
		{
			playersJson.add(data.getName(), data.toJson());
		}

		json.add("players", playersJson);

		try (Writer writer = Files.newBufferedWriter(localDirectory.resolve("all.json")))
		{
			ClaimedChunkManagerImpl.GSON.toJson(json, writer);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void exportSvg()
	{
		HashMap<String, ArrayList<ClaimedChunk>> chunkMap = new HashMap<>();

		for (ClaimedChunk chunk : FTBChunksAPIImpl.manager.getAllClaimedChunks())
		{
			chunkMap.computeIfAbsent(chunk.getPos().dimension, type -> new ArrayList<>()).add(chunk);
		}

		long sec = Instant.now().getEpochSecond();

		for (Map.Entry<String, ArrayList<ClaimedChunk>> entry : chunkMap.entrySet())
		{
			try (Writer writer = Files.newBufferedWriter(localDirectory.resolve(entry.getKey().replace(':', '_') + ".svg"), StandardOpenOption.CREATE))
			{
				DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
				Document document = documentBuilder.newDocument();

				Element svg = document.createElement("svg");
				document.appendChild(svg);
				svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
				svg.setAttribute("version", "1.1");

				Element style = document.createElement("style");
				StringBuilder sb = new StringBuilder();
				sb.append("text{font-size:4px;dominant-baseline:hanging;}");
				sb.append("rect{width:32px;height:32px;stroke-width:0.75px;stroke:#000000;}");
				sb.append(".fl{stroke:#FF0000;}");

				style.appendChild(document.createTextNode(sb.toString()));
				svg.appendChild(style);

				int minX = Integer.MAX_VALUE;
				int minZ = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				int maxZ = Integer.MIN_VALUE;

				for (ClaimedChunk chunk : entry.getValue())
				{
					minX = Math.min(minX, chunk.getPos().x);
					minZ = Math.min(minZ, chunk.getPos().z);
					maxX = Math.max(maxX, chunk.getPos().x);
					maxZ = Math.max(maxZ, chunk.getPos().z);
				}

				svg.setAttribute("width", Integer.toString((maxX - minX + 3) * 33) + 2);
				svg.setAttribute("height", Integer.toString((maxZ - minZ + 3) * 33) + 2);

				for (ClaimedChunk chunk : entry.getValue())
				{
					int x = (chunk.getPos().x - minX + 1) * 33 + 1;
					int z = (chunk.getPos().z - minZ + 1) * 33 + 1;

					Element c = document.createElement("rect");
					c.setAttribute("x", Integer.toString(x));
					c.setAttribute("y", Integer.toString(z));
					c.setAttribute("fill", String.format("#%06X", 0xFFFFFF & chunk.getColor()));

					StringBuilder title = new StringBuilder();
					title.append("Claimed: ").append(prettyTimeString(sec - chunk.getTimeClaimed().getEpochSecond())).append(" ago");

					String group = chunk.getGroupID();

					if (!group.isEmpty())
					{
						title.append("\r\nGroup: ").append(group);
					}

					if (chunk.isForceLoaded())
					{
						c.setAttribute("class", "fl");
						title.append("\r\nForce Loaded");
					}

					svg.appendChild(c);

					Element t = document.createElement("title");
					t.appendChild(document.createTextNode(title.toString()));
					c.appendChild(t);

					Element n = document.createElement("text");
					n.setAttribute("x", Integer.toString(x + 1));
					n.setAttribute("y", Integer.toString(z + 1));
					n.appendChild(document.createTextNode(chunk.getPlayerData().getName()));
					svg.appendChild(n);

					Element cx = document.createElement("text");
					cx.setAttribute("x", Integer.toString(x + 1));
					cx.setAttribute("y", Integer.toString(z + 6));
					cx.appendChild(document.createTextNode("X: " + ((chunk.getPos().x << 4) + 8)));
					svg.appendChild(cx);

					Element cz = document.createElement("text");
					cz.setAttribute("x", Integer.toString(x + 1));
					cz.setAttribute("y", Integer.toString(z + 11));
					cz.appendChild(document.createTextNode("Z: " + ((chunk.getPos().z << 4) + 8)));
					svg.appendChild(cz);
				}

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource domSource = new DOMSource(document);
				StreamResult streamResult = new StreamResult(writer);
				transformer.transform(domSource, streamResult);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}