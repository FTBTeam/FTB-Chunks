package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.ClaimedChunkManager;
import com.feed_the_beast.mods.ftbchunks.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResults;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkEvent;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkGroup;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.command.CommandSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkPlayerDataImpl implements ClaimedChunkPlayerData
{
	public final ClaimedChunkManagerImpl manager;
	public final File file;
	public final UUID uuid;
	public String name;
	public int color;
	public Map<ChunkDimPos, ClaimedChunkImpl> claimedChunks;
	private final Map<String, ClaimedChunkGroupImpl> groups;
	public boolean shouldSave;

	public ClaimedChunkPlayerDataImpl(ClaimedChunkManagerImpl m, File f, UUID id)
	{
		manager = m;
		file = f;
		uuid = id;
		name = "";
		color = 0;
		claimedChunks = new HashMap<>();
		groups = new HashMap<>();
		shouldSave = false;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public ClaimedChunkManager getManager()
	{
		return manager;
	}

	@Override
	public UUID getUuid()
	{
		return uuid;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int getColor()
	{
		if (color == 0)
		{
			color = java.awt.Color.HSBtoRGB(MathUtils.RAND.nextFloat(), 0.65F, 1F);
			save();
		}

		return 0xFF000000 | color;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<ClaimedChunk> getClaimedChunks()
	{
		return (Collection<ClaimedChunk>) (Collection) claimedChunks.values();
	}

	@Override
	public ClaimedChunkGroupImpl getGroup(String id)
	{
		if (id.isEmpty())
		{
			throw new IllegalArgumentException("Invalid group ID!");
		}

		ClaimedChunkGroupImpl group = groups.get(id);

		if (group == null)
		{
			group = new ClaimedChunkGroupImpl(this, id);
			groups.put(id, group);
			save();
		}

		return group;
	}

	@Override
	public boolean hasGroup(String id)
	{
		return groups.containsKey(id);
	}

	@Override
	@Nullable
	public ClaimedChunkGroupImpl removeGroup(String id)
	{
		ClaimedChunkGroupImpl g = groups.remove(id);

		if (g != null)
		{
			save();
		}

		return g;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<ClaimedChunkGroup> getGroups()
	{
		return (Collection<ClaimedChunkGroup>) (Collection) groups.values();
	}

	@Override
	public ClaimResult claim(CommandSource source, ChunkDimPos pos, boolean checkOnly)
	{
		ClaimedChunkImpl chunk = manager.claimedChunks.get(pos);

		if (chunk != null)
		{
			return ClaimResults.ALREADY_CLAIMED;
		}

		chunk = new ClaimedChunkImpl(this, pos);

		ClaimResult r = new ClaimedChunkEvent.Claim.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		claimedChunks.put(pos, chunk);
		manager.claimedChunks.put(pos, chunk);
		new ClaimedChunkEvent.Claim.Done(source, chunk).postAndGetResult();
		save();
		return chunk;
	}

	@Override
	public ClaimResult unclaim(CommandSource source, ChunkDimPos pos, boolean checkOnly)
	{
		ClaimedChunkImpl chunk = manager.claimedChunks.get(pos);

		if (chunk == null)
		{
			return ClaimResults.NOT_CLAIMED;
		}
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer())
		{
			return ClaimResults.NOT_OWNER;
		}

		ClaimResult r = new ClaimedChunkEvent.Unclaim.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		claimedChunks.remove(pos);
		manager.claimedChunks.remove(pos);
		new ClaimedChunkEvent.Unclaim.Done(source, chunk).postAndGetResult();
		chunk.playerData.save();
		return chunk;
	}

	@Override
	public ClaimResult load(CommandSource source, ChunkDimPos pos, boolean checkOnly)
	{
		ClaimedChunkImpl chunk = manager.claimedChunks.get(pos);

		if (chunk == null)
		{
			return ClaimResults.NOT_CLAIMED;
		}
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer())
		{
			return ClaimResults.NOT_OWNER;
		}
		else if (chunk.forceLoaded)
		{
			return ClaimResults.ALREADY_LOADED;
		}

		ClaimResult r = new ClaimedChunkEvent.Load.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		chunk.forceLoaded = true;
		ServerChunkProvider chunkProvider = source.getServer().getWorld(pos.dimension).getChunkProvider();
		chunkProvider.registerTicket(ClaimedChunkManagerImpl.TICKET_TYPE, pos.getChunkPos(), 2, chunk);
		new ClaimedChunkEvent.Load.Done(source, chunk).postAndGetResult();
		chunk.playerData.save();
		return chunk;
	}

	@Override
	public ClaimResult unload(CommandSource source, ChunkDimPos pos, boolean checkOnly)
	{
		ClaimedChunkImpl chunk = manager.claimedChunks.get(pos);

		if (chunk == null)
		{
			return ClaimResults.NOT_CLAIMED;
		}
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer())
		{
			return ClaimResults.NOT_OWNER;
		}
		else if (!chunk.forceLoaded)
		{
			return ClaimResults.NOT_LOADED;
		}

		ClaimResult r = new ClaimedChunkEvent.Unload.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		chunk.forceLoaded = false;
		ServerChunkProvider chunkProvider = source.getServer().getWorld(pos.dimension).getChunkProvider();
		chunkProvider.releaseTicket(ClaimedChunkManagerImpl.TICKET_TYPE, pos.getChunkPos(), 2, chunk);
		new ClaimedChunkEvent.Unload.Done(source, chunk).postAndGetResult();
		chunk.playerData.save();
		return chunk;
	}

	@Override
	public void save()
	{
		shouldSave = true;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty("uuid", UUIDTypeAdapter.fromUUID(uuid));
		json.addProperty("name", name);
		json.addProperty("color", String.format("#%06X", 0xFFFFFF & color));

		JsonObject groupJson = new JsonObject();

		for (ClaimedChunkGroupImpl group : groups.values())
		{
			groupJson.add(group.getId(), group.toJson());
		}

		json.add("groups", groupJson);

		JsonObject chunksJson = new JsonObject();

		for (ClaimedChunk chunk : claimedChunks.values())
		{
			String dim = DimensionType.getKey(chunk.getPos().dimension).toString();
			JsonElement e = chunksJson.get(dim);

			if (e == null || e.isJsonNull())
			{
				e = new JsonArray();
				chunksJson.add(dim, e);
			}

			JsonObject chunkJson = new JsonObject();
			chunkJson.addProperty("x", chunk.getPos().x);
			chunkJson.addProperty("z", chunk.getPos().z);
			chunkJson.addProperty("time", chunk.getTime().toString());

			if (chunk.isForceLoaded())
			{
				chunkJson.addProperty("force_loaded", true);
			}

			if (chunk.getGroup() != null)
			{
				chunkJson.addProperty("group", chunk.getGroup().getId());
			}

			e.getAsJsonArray().add(chunkJson);
		}

		json.add("chunks", chunksJson);
		return json;
	}

	public long fromJson(JsonObject json)
	{
		long totalChunks = 0L;
		name = json.get("name").getAsString();
		color = 0;

		try
		{
			color = Integer.decode(json.get("color").getAsString());
		}
		catch (Exception ex)
		{
		}

		if (json.has("groups"))
		{
			for (Map.Entry<String, JsonElement> entry : json.get("groups").getAsJsonObject().entrySet())
			{
				getGroup(entry.getKey()).fromJson(entry.getValue().getAsJsonObject());
			}
		}

		if (json.has("chunks"))
		{
			for (Map.Entry<String, JsonElement> entry : json.get("chunks").getAsJsonObject().entrySet())
			{
				DimensionType dimension = DimensionType.byName(new ResourceLocation(entry.getKey()));

				if (dimension == null)
				{
					continue;
				}

				for (JsonElement e : entry.getValue().getAsJsonArray())
				{
					JsonObject o = e.getAsJsonObject();
					int x = o.get("x").getAsInt();
					int z = o.get("z").getAsInt();

					ClaimedChunkImpl chunk = new ClaimedChunkImpl(this, new ChunkDimPos(dimension, x, z));

					if (o.has("time"))
					{
						chunk.time = Instant.parse(o.get("time").getAsString());
					}

					chunk.forceLoaded = o.has("force_loaded") && o.get("force_loaded").getAsBoolean();

					if (o.has("group"))
					{
						chunk.group = getGroup(o.get("group").getAsString());
					}

					claimedChunks.put(chunk.pos, chunk);
					manager.claimedChunks.put(chunk.pos, chunk);
					totalChunks++;
				}
			}
		}

		return totalChunks;
	}
}