package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
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
import com.feed_the_beast.mods.ftbchunks.api.PrivacyMode;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.api.WaypointType;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	public final Path file;
	public boolean shouldSave;
	public GameProfile profile;
	public int color;
	private final Map<String, ClaimedChunkGroupImpl> groups;
	public final Set<UUID> allies;
	public PrivacyMode blockEditMode;
	public PrivacyMode blockInteractMode;
	public final Map<UUID, Waypoint> waypoints;
	public PrivacyMode minimapMode;
	public PrivacyMode locationMode;

	public int prevChunkX = Integer.MAX_VALUE, prevChunkZ = Integer.MAX_VALUE;
	public String lastChunkID = "";
	public HashSet<PlayerLocation> cachedVisiblePlayers;

	public ClaimedChunkPlayerDataImpl(ClaimedChunkManagerImpl m, Path f, UUID id)
	{
		manager = m;
		file = f;
		shouldSave = false;
		profile = new GameProfile(id, "");
		color = 0;
		groups = new HashMap<>();
		allies = new HashSet<>();
		blockEditMode = PrivacyMode.ALLIES;
		blockInteractMode = PrivacyMode.ALLIES;
		waypoints = new LinkedHashMap<>();
		minimapMode = PrivacyMode.ALLIES;
		locationMode = PrivacyMode.ALLIES;
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
			color = MathHelper.hsvToRGB(MathUtils.RAND.nextFloat(), 0.65F, 1F);
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
		else if (FTBChunksConfig.claimDimensionBlacklist.contains(pos.dimension))
		{
			return ClaimResults.DIMENSION_FORBIDDEN;
		}
		else if (source.getEntity() instanceof ServerPlayerEntity && getClaimedChunks().size() >= FTBChunksConfig.getMaxClaimedChunks((ServerPlayerEntity) source.getEntity()))
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
		else if (chunk.playerData != this && !source.hasPermissionLevel(2) && !source.getServer().isSinglePlayer())
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

		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.owner = getUuid();
		packet.chunk = new SendChunkPacket.SingleChunk(new Date(), pos.x, pos.z, null);
		FTBChunksNet.MAIN.send(PacketDistributor.ALL.noArg(), packet);
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
		else if (source.getEntity() instanceof ServerPlayerEntity && getForceLoadedChunks().size() >= FTBChunksConfig.getMaxForceLoadedChunks((ServerPlayerEntity) source.getEntity()))
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
		return entity instanceof ServerPlayerEntity && FTBChunks.teamsMod && FTBTeamsIntegration.isTeamMember(getProfile(), ((ServerPlayerEntity) entity).getGameProfile());
	}

	@Override
	public boolean isExplicitAlly(ServerPlayerEntity p)
	{
		if (getUuid().equals(p.getUniqueID()))
		{
			return true;
		}
		else if (allies.contains(p.getUniqueID()))
		{
			return true;
		}
		else if (isTeamMember(p))
		{
			return true;
		}

		return getName().equals(p.getGameProfile().getName());
	}

	@Override
	public boolean isAlly(ServerPlayerEntity p)
	{
		if (FTBChunksConfig.allyMode == AllyMode.FORCED_ALL || getUuid().equals(p.getUniqueID()))
		{
			return true;
		}
		else if (FTBChunksConfig.allyMode == AllyMode.FORCED_NONE)
		{
			return false;
		}

		return isExplicitAlly(p);
	}

	public boolean canUse(ServerPlayerEntity p, PrivacyMode mode, boolean explicit)
	{
		if (mode == PrivacyMode.PUBLIC)
		{
			return true;
		}
		else if (p.getServer().isSinglePlayer())
		{
			return true;
		}
		else if (mode == PrivacyMode.ALLIES)
		{
			return explicit ? isExplicitAlly(p) : isAlly(p);
		}

		return false;
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

		json.addProperty("block_edit_mode", blockEditMode.name);
		json.addProperty("block_interact_mode", blockInteractMode.name);
		json.addProperty("minimap_mode", minimapMode.name);
		json.addProperty("location_mode", locationMode.name);

		JsonObject chunksJson = new JsonObject();

		for (ClaimedChunk chunk : getClaimedChunks())
		{
			String dim = chunk.getPos().dimension;
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

		JsonArray waypointArray = new JsonArray();

		for (Waypoint w : waypoints.values())
		{
			JsonObject o = new JsonObject();
			o.addProperty("id", UUIDTypeAdapter.fromUUID(w.id));
			o.addProperty("name", w.name);
			o.addProperty("dimension", w.dimension);
			o.addProperty("x", w.x);
			o.addProperty("y", w.y);
			o.addProperty("z", w.z);
			o.addProperty("color", String.format("#%06X", 0xFFFFFF & w.color));
			o.addProperty("privacy", w.privacy.name);
			o.addProperty("type", w.type.name().toLowerCase());
			waypointArray.add(o);
		}

		json.add("waypoints", waypointArray);

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

		if (json.has("block_edit_mode"))
		{
			blockEditMode = PrivacyMode.get(json.get("block_edit_mode").getAsString());
		}

		if (json.has("block_interact_mode"))
		{
			blockInteractMode = PrivacyMode.get(json.get("block_interact_mode").getAsString());
		}

		if (json.has("minimap_mode"))
		{
			minimapMode = PrivacyMode.get(json.get("minimap_mode").getAsString());
		}

		if (json.has("location_mode"))
		{
			locationMode = PrivacyMode.get(json.get("location_mode").getAsString());
		}

		if (json.has("chunks"))
		{
			for (Map.Entry<String, JsonElement> entry : json.get("chunks").getAsJsonObject().entrySet())
			{
				for (JsonElement e : entry.getValue().getAsJsonArray())
				{
					JsonObject o = e.getAsJsonObject();
					int x = o.get("x").getAsInt();
					int z = o.get("z").getAsInt();

					ClaimedChunkImpl chunk = new ClaimedChunkImpl(this, new ChunkDimPos(entry.getKey(), x, z));

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

		if (json.has("waypoints"))
		{
			for (JsonElement e : json.get("waypoints").getAsJsonArray())
			{
				JsonObject o = e.getAsJsonObject();

				Waypoint w = new Waypoint(this, o.has("id") ? UUIDTypeAdapter.fromString(o.get("id").getAsString()) : UUID.randomUUID());
				w.dimension = o.get("dimension").getAsString();
				w.name = o.get("name").getAsString();
				w.x = o.get("x").getAsInt();
				w.y = o.get("y").getAsInt();
				w.z = o.get("z").getAsInt();
				w.color = 0xFFFFFF;

				if (o.has("color"))
				{
					try
					{
						w.color = Integer.decode(o.get("color").getAsString());
					}
					catch (Exception ex)
					{
					}
				}

				if (o.has("privacy"))
				{
					w.privacy = PrivacyMode.get(o.get("privacy").getAsString());
				}

				if (o.has("type"))
				{
					w.type = WaypointType.valueOf(o.get("type").getAsString().toUpperCase());
				}

				waypoints.put(w.id, w);
			}
		}
		else
		{
			save();
		}
	}

	public IFormattableTextComponent getDisplayName()
	{
		if (FTBChunks.teamsMod)
		{
			IFormattableTextComponent component = FTBTeamsIntegration.getTeamName(this);

			if (component != null)
			{
				return component;
			}
		}

		return new StringTextComponent(getName()).mergeStyle(Style.EMPTY.setColor(Color.func_240743_a_(getColor())));
	}
}