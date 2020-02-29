package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkGroup;
import com.google.gson.JsonObject;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class ClaimedChunkGroupImpl implements ClaimedChunkGroup
{
	private final ClaimedChunkPlayerDataImpl playerData;
	private final String id;
	public ITextComponent customName;

	public ClaimedChunkGroupImpl(ClaimedChunkPlayerDataImpl p, String i)
	{
		playerData = p;
		id = i;
		customName = null;
	}

	@Override
	public ClaimedChunkPlayerDataImpl getPlayerData()
	{
		return playerData;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Nullable
	@Override
	public ITextComponent getCustomName()
	{
		return customName;
	}

	@Override
	public int getColorOverride()
	{
		return 0;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		if (customName != null)
		{
			json.add("custom_name", ITextComponent.Serializer.toJsonTree(customName));
		}

		return json;
	}

	public void fromJson(JsonObject json)
	{
		customName = json.has("custom_name") ? ITextComponent.Serializer.fromJson(json.get("custom_name")) : null;
	}
}