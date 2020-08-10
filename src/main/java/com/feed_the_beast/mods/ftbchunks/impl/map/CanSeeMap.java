package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBRanksIntegration;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class CanSeeMap implements Predicate<ServerPlayerEntity>
{
	public final ServerPlayerEntity player;
	private final ClaimedChunkPlayerDataImpl data;

	public CanSeeMap(ServerPlayerEntity p)
	{
		player = p;
		data = FTBChunksAPIImpl.manager.getData(player);
	}

	@Override
	public boolean test(ServerPlayerEntity p)
	{
		return player == p || data.canUse(p, data.minimapMode, true) || (FTBChunks.ranksMod ? FTBRanksIntegration.canSeeMap(p) : p.hasPermissionLevel(2));
	}
}