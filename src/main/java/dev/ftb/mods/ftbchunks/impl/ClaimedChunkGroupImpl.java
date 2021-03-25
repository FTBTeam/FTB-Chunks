package dev.ftb.mods.ftbchunks.impl;

import com.google.gson.JsonObject;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkGroup;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class ClaimedChunkGroupImpl implements ClaimedChunkGroup {
	private final ClaimedChunkPlayerDataImpl playerData;
	private final String id;
	public Component customName;

	public ClaimedChunkGroupImpl(ClaimedChunkPlayerDataImpl p, String i) {
		playerData = p;
		id = i;
		customName = null;
	}

	@Override
	public ClaimedChunkPlayerDataImpl getPlayerData() {
		return playerData;
	}

	@Override
	public String getId() {
		return id;
	}

	@Nullable
	@Override
	public Component getCustomName() {
		return customName;
	}

	@Override
	public int getColorOverride() {
		return 0;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (customName != null) {
			json.add("custom_name", Component.Serializer.toJsonTree(customName));
		}

		return json;
	}

	public void fromJson(JsonObject json) {
		customName = json.has("custom_name") ? Component.Serializer.fromJson(json.get("custom_name")) : null;
	}
}