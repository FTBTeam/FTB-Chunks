package dev.ftb.mods.ftbchunks.data;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class ClaimedChunkGroup {
	private final ClaimedChunkPlayerData playerData;
	private final String id;
	public Component customName;

	public ClaimedChunkGroup(ClaimedChunkPlayerData p, String i) {
		playerData = p;
		id = i;
		customName = null;
	}

	public ClaimedChunkPlayerData getPlayerData() {
		return playerData;
	}

	public String getId() {
		return id;
	}

	@Nullable
	public Component getCustomName() {
		return customName;
	}

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