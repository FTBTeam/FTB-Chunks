package com.feed_the_beast.mods.ftbchunks.api;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkPlayerData
{
	ClaimedChunkManager getManager();

	GameProfile getProfile();

	default UUID getUuid()
	{
		return getProfile().getId();
	}

	default String getName()
	{
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

	ClaimResult claim(CommandSource source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult unclaim(CommandSource source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult load(CommandSource source, ChunkDimPos pos, boolean checkOnly);

	ClaimResult unload(CommandSource source, ChunkDimPos pos, boolean checkOnly);

	void save();

	boolean isAlly(ServerPlayerEntity player);

	default ITextComponent getDisplayName()
	{
		return new StringTextComponent(getName());
	}
}