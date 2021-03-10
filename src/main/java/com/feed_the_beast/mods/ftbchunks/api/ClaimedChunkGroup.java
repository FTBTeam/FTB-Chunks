package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkGroup {
	ClaimedChunkPlayerData getPlayerData();

	String getId();

	@Nullable
	Component getCustomName();

	int getColorOverride();
}