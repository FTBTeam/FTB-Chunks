package dev.ftb.mods.ftbchunks.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.hooks.PlayerHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClaimedChunkPlayerData {
	public final ClaimedChunkManager manager;
	public final Path file;
	public boolean shouldSave;
	public GameProfile profile;
	public PrivacyMode blockEditMode;
	public PrivacyMode blockInteractMode;
	public PrivacyMode minimapMode;
	public PrivacyMode locationMode;
	public int extraClaimChunks;
	public int extraForceLoadChunks;
	public boolean chunkLoadOffline;
	public boolean bypassProtection;
	public boolean allowFakePlayers;

	public int prevChunkX = Integer.MAX_VALUE, prevChunkZ = Integer.MAX_VALUE;
	public String lastChunkID = "";

	public ClaimedChunkPlayerData(ClaimedChunkManager m, Path f, UUID id) {
		manager = m;
		file = f;
		shouldSave = false;
		profile = new GameProfile(id, "");
		blockEditMode = PrivacyMode.ALLIES;
		blockInteractMode = PrivacyMode.ALLIES;
		minimapMode = PrivacyMode.ALLIES;
		locationMode = PrivacyMode.ALLIES;
		extraClaimChunks = 0;
		extraForceLoadChunks = 0;
		chunkLoadOffline = true;
		bypassProtection = false;
		allowFakePlayers = true;
	}

	@Override
	public String toString() {
		return getName();
	}

	public ClaimedChunkManager getManager() {
		return manager;
	}

	public GameProfile getProfile() {
		return profile;
	}

	public UUID getUuid() {
		return getProfile().getId();
	}

	public String getName() {
		return getProfile().getName();
	}

	@Nullable
	public Team getTeam() {
		return FTBTeamsAPI.getManager().getPlayerTeam(getUuid());
	}

	public int getColor() {
		Team team = getTeam();
		return team == null ? 0xFFFFFF : team.getColor();
	}

	public Collection<ClaimedChunk> getClaimedChunks() {
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunk chunk : manager.claimedChunks.values()) {
			if (chunk.playerData == this) {
				list.add(chunk);
			}
		}

		return list;
	}

	public Collection<ClaimedChunk> getForceLoadedChunks() {
		List<ClaimedChunk> list = new ArrayList<>();

		for (ClaimedChunk chunk : manager.claimedChunks.values()) {
			if (chunk.playerData == this && chunk.isForceLoaded()) {
				list.add(chunk);
			}
		}

		return list;
	}

	public ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk != null) {
			return ClaimResults.ALREADY_CLAIMED;
		} else if (manager.config.claimDimensionBlacklistSet.contains(pos.dimension)) {
			return ClaimResults.DIMENSION_FORBIDDEN;
		} else if (source.getEntity() instanceof ServerPlayer && getClaimedChunks().size() >= manager.config.getMaxClaimedChunks(this, (ServerPlayer) source.getEntity())) {
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		chunk = new ClaimedChunk(this, pos);

		ClaimResult r = ClaimedChunkEvent.BEFORE_CLAIM.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		manager.claimedChunks.put(pos, chunk);
		ClaimedChunkEvent.AFTER_CLAIM.invoker().after(source, chunk);
		save();
		return chunk;
	}

	public ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.playerData != this && !source.hasPermission(2) && !source.getServer().isSingleplayer()) {
			return ClaimResults.NOT_OWNER;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_UNCLAIM.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		if (chunk.isForceLoaded()) {
			chunk.setForceLoadedTime(null);
			chunk.postSetForceLoaded(false);
			ClaimedChunkEvent.AFTER_UNLOAD.invoker().after(source, chunk);
		}

		manager.claimedChunks.remove(pos);
		ClaimedChunkEvent.AFTER_UNCLAIM.invoker().after(source, chunk);
		chunk.playerData.save();

		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.owner = getUuid();
		packet.chunk = new SendChunkPacket.SingleChunk(new Date(), pos.x, pos.z, null);
		packet.sendToAll();
		return chunk;
	}

	public ClaimResult load(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.playerData != this && !source.hasPermission(2) && !source.getServer().isSingleplayer()) {
			return ClaimResults.NOT_OWNER;
		} else if (chunk.isForceLoaded()) {
			return ClaimResults.ALREADY_LOADED;
		} else if (source.getEntity() instanceof ServerPlayer && getForceLoadedChunks().size() >= manager.config.getMaxForceLoadedChunks(this, (ServerPlayer) source.getEntity())) {
			return ClaimResults.NOT_ENOUGH_POWER;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_LOAD.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		chunk.setForceLoadedTime(Instant.now());
		chunk.postSetForceLoaded(true);
		ClaimedChunkEvent.AFTER_LOAD.invoker().after(source, chunk);
		chunk.playerData.save();
		return chunk;
	}

	public ClaimResult unload(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly) {
		ClaimedChunk chunk = manager.claimedChunks.get(pos);

		if (chunk == null) {
			return ClaimResults.NOT_CLAIMED;
		} else if (chunk.playerData != this
				&& !source.hasPermission(2)
				&& !source.getServer().isSingleplayer()
				&& !(source.getEntity() instanceof ServerPlayer && isTeamMember(source.getEntity().getUUID()))
		) {
			return ClaimResults.NOT_OWNER;
		} else if (!chunk.isForceLoaded()) {
			return ClaimResults.NOT_LOADED;
		}

		ClaimResult r = ClaimedChunkEvent.BEFORE_UNLOAD.invoker().before(source, chunk).object();

		if (r == null) {
			r = chunk;
		}

		if (checkOnly || !r.isSuccess()) {
			return r;
		}

		chunk.setForceLoadedTime(null);
		chunk.postSetForceLoaded(false);
		ClaimedChunkEvent.AFTER_UNLOAD.invoker().after(source, chunk);
		chunk.playerData.save();
		return chunk;
	}

	public void save() {
		shouldSave = true;
	}

	public boolean isTeamMember(UUID p) {
		if (p.equals(getUuid())) {
			return true;
		}

		Team team1 = getTeam();
		return team1 != null && team1.equals(FTBTeamsAPI.getManager().getPlayerTeam(p));
	}

	public boolean isExplicitAlly(UUID p) {
		if (getUuid().equals(p)) {
			return true;
		}

		Team team1 = getTeam();

		if (team1 != null) {
			if (team1.isMember(p)) {
				return true;
			}

			Team team2 = FTBTeamsAPI.getPlayerTeam(p);
			return team2 != null && team1.isAlly(p) && team2.isAlly(getUuid());
		}

		return false;
	}

	public boolean isAlly(UUID p) {
		if (manager.config.allyMode == AllyMode.FORCED_ALL || getUuid().equals(p)) {
			return true;
		} else if (manager.config.allyMode == AllyMode.FORCED_NONE) {
			return false;
		}

		return isExplicitAlly(p);
	}

	public boolean canUse(ServerPlayer p, PrivacyMode mode, boolean explicit) {
		if (mode == PrivacyMode.PUBLIC) {
			return true;
		} else if (mode == PrivacyMode.ALLIES) {
			if (PlayerHooks.isFake(p)) {
				return allowFakePlayers;
			}

			return explicit ? isExplicitAlly(p.getUUID()) : isAlly(p.getUUID());
		}

		Team team = getTeam();
		return team != null && team.isMember(p.getUUID());
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("uuid", UUIDTypeAdapter.fromUUID(getUuid()));
		json.addProperty("name", getName());

		json.addProperty("block_edit_mode", blockEditMode.name);
		json.addProperty("block_interact_mode", blockInteractMode.name);
		json.addProperty("minimap_mode", minimapMode.name);
		json.addProperty("location_mode", locationMode.name);
		json.addProperty("extra_claim_chunks", extraClaimChunks);
		json.addProperty("extra_force_load_chunks", extraForceLoadChunks);
		json.addProperty("chunk_load_offline", chunkLoadOffline);
		json.addProperty("allow_fake_players", allowFakePlayers);

		JsonObject chunksJson = new JsonObject();

		for (ClaimedChunk chunk : getClaimedChunks()) {
			String dim = chunk.getPos().dimension.location().toString();
			JsonElement e = chunksJson.get(dim);

			if (e == null || e.isJsonNull()) {
				e = new JsonArray();
				chunksJson.add(dim, e);
			}

			JsonObject chunkJson = new JsonObject();
			chunkJson.addProperty("x", chunk.getPos().x);
			chunkJson.addProperty("z", chunk.getPos().z);
			chunkJson.addProperty("time", chunk.getTimeClaimed().toString());

			if (chunk.isForceLoaded()) {
				chunkJson.addProperty("force_loaded", chunk.getForceLoadedTime().toString());
			}

			e.getAsJsonArray().add(chunkJson);
		}

		json.add("chunks", chunksJson);

		return json;
	}

	public void fromJson(JsonObject json) {
		profile = new GameProfile(getUuid(), json.get("name").getAsString());

		if (json.has("block_edit_mode")) {
			blockEditMode = PrivacyMode.get(json.get("block_edit_mode").getAsString());
		}

		if (json.has("block_interact_mode")) {
			blockInteractMode = PrivacyMode.get(json.get("block_interact_mode").getAsString());
		}

		if (json.has("minimap_mode")) {
			minimapMode = PrivacyMode.get(json.get("minimap_mode").getAsString());
		}

		if (json.has("location_mode")) {
			locationMode = PrivacyMode.get(json.get("location_mode").getAsString());
		}

		if (json.has("extra_claim_chunks")) {
			extraClaimChunks = json.get("extra_claim_chunks").getAsInt();
		}

		if (json.has("extra_force_load_chunks")) {
			extraForceLoadChunks = json.get("extra_force_load_chunks").getAsInt();
		}

		if (json.has("chunk_load_offline")) {
			chunkLoadOffline = json.get("chunk_load_offline").getAsBoolean();
		}

		if (json.has("allow_fake_players")) {
			allowFakePlayers = json.get("allow_fake_players").getAsBoolean();
		}

		if (json.has("chunks")) {
			for (Map.Entry<String, JsonElement> entry : json.get("chunks").getAsJsonObject().entrySet()) {
				for (JsonElement e : entry.getValue().getAsJsonArray()) {
					JsonObject o = e.getAsJsonObject();
					int x = o.get("x").getAsInt();
					int z = o.get("z").getAsInt();

					ClaimedChunk chunk = new ClaimedChunk(this, new ChunkDimPos(ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(entry.getKey())), x, z));

					if (o.has("time")) {
						chunk.time = Instant.parse(o.get("time").getAsString());
					}

					if (o.has("force_loaded")) {
						if (o.get("force_loaded").getAsJsonPrimitive().isBoolean()) {
							chunk.forceLoaded = chunk.time;
							save();
						} else {
							chunk.forceLoaded = Instant.parse(o.get("force_loaded").getAsString());
						}
					}

					manager.claimedChunks.put(chunk.pos, chunk);
				}
			}
		}
	}

	public Component getDisplayName() {
		Team team = getTeam();
		return team == null ? new TextComponent(getName()) : team.getName();
	}

	public int getExtraClaimChunks() {
		return extraClaimChunks;
	}

	public int getExtraForceLoadChunks() {
		return extraForceLoadChunks;
	}

	public boolean chunkLoadOffline() {
		return chunkLoadOffline;
	}

	public void setChunkLoadOffline(boolean val) {
		chunkLoadOffline = val;
		save();
	}

	public boolean getBypassProtection(ServerPlayer player) {
		return bypassProtection && !PlayerHooks.isFake(player);
	}
}
