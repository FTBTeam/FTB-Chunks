package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author LatvianModder
 */
public interface ClaimedChunk extends ClaimResult {
	ClaimedChunkPlayerData getPlayerData();

	ChunkDimPos getPos();

	@Nullable
	Instant getForceLoadedTime();

	default boolean isForceLoaded() {
		return getForceLoadedTime() != null;
	}

	@Nullable
	ClaimedChunkGroup getGroup();

	default String getGroupID() {
		ClaimedChunkGroup g = getGroup();
		return g == null ? "" : g.getId();
	}

	Instant getTimeClaimed();

	@Override
	default boolean isSuccess() {
		return true;
	}

	default int getColor() {
		int c = getGroup() == null ? 0 : getGroup().getColorOverride();
		return c == 0 ? getPlayerData().getColor() : c;
	}

	boolean canEdit(ServerPlayer player, BlockState state);

	boolean canInteract(ServerPlayer player, BlockState state);

	boolean canEntitySpawn(Entity entity);

	boolean allowExplosions();

	default Component getDisplayName() {
		ClaimedChunkGroup group = getGroup();

		if (group != null && group.getCustomName() != null) {
			return group.getCustomName();
		}

		return new TextComponent("").append(getPlayerData().getDisplayName()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColor() & 0xFFFFFF)));
	}
}