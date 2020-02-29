package com.feed_the_beast.mods.ftbchunks.api;

import com.feed_the_beast.mods.ftbchunks.ClaimedChunkPlayerData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author LatvianModder
 */
public interface ClaimedChunk extends ClaimResult
{
	ClaimedChunkPlayerData getPlayerData();

	ChunkDimPos getPos();

	boolean isForceLoaded();

	@Nullable
	ClaimedChunkGroup getGroup();

	default String getGroupID()
	{
		ClaimedChunkGroup g = getGroup();
		return g == null ? "" : g.getId();
	}

	Instant getTime();

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

	boolean canEdit(ServerPlayerEntity player, BlockState blockState);

	boolean canInteract(ServerPlayerEntity player, BlockState blockState);

	boolean canEntitySpawn(Entity entity);

	boolean allowExplosions();

	default ITextComponent getDisplayName()
	{
		ClaimedChunkGroup group = getGroup();

		if (group != null && group.getCustomName() != null)
		{
			return group.getCustomName();
		}

		return new StringTextComponent(getPlayerData().getName());
	}

}