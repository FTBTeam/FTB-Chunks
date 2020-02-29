package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkGroup;
import net.minecraft.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkPlayerData
{
	ClaimedChunkManager getManager();

	UUID getUuid();

	String getName();

	int getColor();

	Collection<ClaimedChunk> getClaimedChunks();

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
}