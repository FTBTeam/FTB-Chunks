package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author LatvianModder
 */
public interface ClaimedChunk extends ClaimResult
{
	ClaimedChunkPlayerData getPlayerData();

	ChunkDimPos getPos();

	@Nullable
	Instant getForceLoadedTime();

	default boolean isForceLoaded()
	{
		return getForceLoadedTime() != null;
	}

	@Nullable
	ClaimedChunkGroup getGroup();

	default String getGroupID()
	{
		ClaimedChunkGroup g = getGroup();
		return g == null ? "" : g.getId();
	}

	Instant getTimeClaimed();

	@Override
	default boolean isSuccess()
	{
		return true;
	}

	default int getColor()
	{
		int c = getGroup() == null ? 0 : getGroup().getColorOverride();
		return c == 0 ? getPlayerData().getColor() : c;
	}

	boolean canEdit(ServerPlayerEntity player, BlockState state);

	boolean canInteract(ServerPlayerEntity player, BlockState state);

	boolean canEntitySpawn(Entity entity);

	boolean allowExplosions();

	default ITextComponent getDisplayName()
	{
		ClaimedChunkGroup group = getGroup();

		if (group != null && group.getCustomName() != null)
		{
			return group.getCustomName();
		}

		return getPlayerData().getDisplayName().mergeStyle(Style.EMPTY.setColor(Color.fromInt(getColor() & 0xFFFFFF)));
	}
}