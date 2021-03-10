package com.feed_the_beast.mods.ftbchunks.api;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkPlayerData {
	ClaimedChunkManager getManager();

	GameProfile getProfile();

	default UUID getUuid() {
		return getProfile().getId();
	}

	default String getName() {
		return getProfile().getName();
	}

	int getColor();

	Collection<ClaimedChunk> getClaimedChunks();

	Collection<ClaimedChunk> getForceLoadedChunks();

	ClaimedChunkGroup getGroup(String id);

	boolean hasGroup(String id);

	@Nullable
	ClaimedChunkGroup removeGroup(String id);

	Collection<ClaimedChunkGroup> getGroups();

	ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult load(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult unload(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

	void save();

	boolean isExplicitAlly(ClaimedChunkPlayerData player);

	boolean isInAllyList(UUID id);

	boolean isAlly(ClaimedChunkPlayerData player);

	default Component getDisplayName() {
		return new TextComponent(getName()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColor() & 0xFFFFFF)));
	}

	int getExtraClaimChunks();

	int getExtraForceLoadChunks();

	boolean chunkLoadOffline();
}