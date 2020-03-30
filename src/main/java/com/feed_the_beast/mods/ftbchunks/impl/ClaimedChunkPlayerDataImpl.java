package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.FTBTeamsIntegration;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResults;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkEvent;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkGroup;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkManager;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkPlayerDataImpl implements ClaimedChunkPlayerData
{
	public final ClaimedChunkManagerImpl manager;
	public final File file;
	public boolean shouldSave;
	public GameProfile profile;
	public int color;
	private final Map<String, ClaimedChunkGroupImpl> groups;
	public final Set<UUID> allies;
	public boolean alliesWhitelist;

	public ClaimedChunkPlayerDataImpl(ClaimedChunkManagerImpl m, File f, UUID id)
	{
		manager = m;
		file = f;
		shouldSave = false;
		profile = new GameProfile(id, "");
		color = 0;
		groups = new HashMap<>();
		allies = new HashSet<>();
		alliesWhitelist = true;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public ClaimedChunkManager getManager()
	{
		return manager;
	}

	@Override
	public GameProfile getProfile()
	{
		return profile;
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
	public Collection<ClaimedChunk> getClaimedChunks()
	{
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunkImpl chunk : manager.claimedChunks.values())
		{
			if (chunk.playerData == this)
			{
				list.add(chunk);
			}
		}

		return list;
	}

	@Override
	public Collection<ClaimedChunk> getForceLoadedChunks()
	{
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunkImpl chunk : manager.claimedChunks.values())
		{
			if (chunk.playerData == this && chunk.isForceLoaded())
			{
				list.add(chunk);
			}
		}

		return list;
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
		else if (getClaimedChunks().size() >= FTBChunksConfig.maxClaimedChunks)
		{
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		chunk = new ClaimedChunkImpl(this, pos);

		ClaimResult r = new ClaimedChunkEvent.Claim.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

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
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer() && !isTeamMember(source.getEntity()))
		{
			return ClaimResults.NOT_OWNER;
		}

		ClaimResult r = new ClaimedChunkEvent.Unclaim.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		if (chunk.isForceLoaded())
		{
			chunk.setForceLoadedTime(null);
			chunk.postSetForceLoaded(false);
			new ClaimedChunkEvent.Unload.Done(source, chunk).postAndGetResult();
		}

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
		else if (chunk.isForceLoaded())
		{
			return ClaimResults.ALREADY_LOADED;
		}
		else if (getForceLoadedChunks().size() >= FTBChunksConfig.maxForceLoadedChunks)
		{
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		ClaimResult r = new ClaimedChunkEvent.Load.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		chunk.setForceLoadedTime(Instant.now());
		chunk.postSetForceLoaded(true);
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
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer() && !isTeamMember(source.getEntity()))
		{
			return ClaimResults.NOT_OWNER;
		}
		else if (!chunk.isForceLoaded())
		{
			return ClaimResults.NOT_LOADED;
		}

		ClaimResult r = new ClaimedChunkEvent.Unload.Check(source, chunk).postAndGetResult();

		if (checkOnly || !r.isSuccess())
		{
			return r;
		}

		chunk.setForceLoadedTime(null);
		chunk.postSetForceLoaded(false);
		new ClaimedChunkEvent.Unload.Done(source, chunk).postAndGetResult();
		chunk.playerData.save();
		return chunk;
	}

	@Override
	public void save()
	{
		shouldSave = true;
	}

	public boolean isTeamMember(@Nullable Entity entity)
	{
		return entity instanceof ServerPlayerEntity && ModList.get().isLoaded("ftbteams") && FTBTeamsIntegration.isTeamMember(this, (ServerPlayerEntity) entity);
	}

	@Override
	public boolean isAlly(ServerPlayerEntity player)
	{
		if (FTBChunksConfig.allyMode == AllyMode.FORCED_ALL)
		{
			return true;
		}
		else if (FTBChunksConfig.allyMode == AllyMode.FORCED_NONE)
		{
			return false;
		}

		return getUuid().equals(player.getUniqueID()) || getName().equals(player.getGameProfile().getName()) || alliesWhitelist == allies.contains(player.getUniqueID()) || isTeamMember(player);
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty("uuid", UUIDTypeAdapter.fromUUID(getUuid()));
		json.addProperty("name", getName());
		json.addProperty("color", String.format("#%06X", 0xFFFFFF & color));

		JsonObject groupJson = new JsonObject();

		for (ClaimedChunkGroupImpl group : groups.values())
		{
			groupJson.add(group.getId(), group.toJson());
		}

		json.add("groups", groupJson);

		JsonArray alliesJson = new JsonArray();

		for (UUID ally : allies)
		{
			alliesJson.add(UUIDTypeAdapter.fromUUID(ally));
		}

		json.add("allies", alliesJson);
		json.addProperty("allies_whitelist", alliesWhitelist);

		JsonObject chunksJson = new JsonObject();

		for (ClaimedChunk chunk : getClaimedChunks())
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
			chunkJson.addProperty("time", chunk.getTimeClaimed().toString());

			if (chunk.isForceLoaded())
			{
				chunkJson.addProperty("force_loaded", chunk.getForceLoadedTime().toString());
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

	public void fromJson(JsonObject json)
	{
		profile = new GameProfile(getUuid(), json.get("name").getAsString());
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

		if (json.has("allies"))
		{
			for (JsonElement e : json.get("allies").getAsJsonArray())
			{
				allies.add(UUIDTypeAdapter.fromString(e.getAsString()));
			}
		}

		if (json.has("allies_whitelist"))
		{
			alliesWhitelist = json.get("allies_whitelist").getAsBoolean();
		}
		else
		{
			alliesWhitelist = true;
			save();
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

					if (o.has("force_loaded"))
					{
						if (o.get("force_loaded").getAsJsonPrimitive().isBoolean())
						{
							chunk.forceLoaded = chunk.time;
							save();
						}
						else
						{
							chunk.forceLoaded = Instant.parse(o.get("force_loaded").getAsString());
						}
					}

					if (o.has("group"))
					{
						chunk.group = getGroup(o.get("group").getAsString());
					}

					manager.claimedChunks.put(chunk.pos, chunk);
				}
			}
		}
	}

	public ITextComponent getDisplayName()
	{
		if (ModList.get().isLoaded("ftbteams"))
		{
			ITextComponent component = FTBTeamsIntegration.getTeamName(this);

			if (component != null)
			{
				return component;
			}
		}

		return new StringTextComponent(getName());
	}
}