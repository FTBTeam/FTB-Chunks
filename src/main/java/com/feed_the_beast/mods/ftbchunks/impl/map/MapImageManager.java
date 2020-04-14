package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class MapImageManager
{
	public final ClaimedChunkManagerImpl manager;
	public File directory;
	public final Map<DimensionType, MapDimension> dimensions;
	public final ArrayDeque<Runnable> taskQueue;
	public long lastUpdate;

	public MapImageManager(ClaimedChunkManagerImpl m)
	{
		manager = m;
		dimensions = new HashMap<>();
		taskQueue = new ArrayDeque<>();
		lastUpdate = 0L;
	}

	public MapDimension getDimension(DimensionType dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new MapDimension(this, d));
	}

	public MapChunk getChunk(ChunkDimPos pos)
	{
		return getDimension(pos.dimension).getRegion(XZ.regionFromChunk(pos.x, pos.z)).access().getChunk(XZ.of(pos.x, pos.z));
	}

	public void queueUpdate(World world, XZ pos, Predicate<ServerPlayerEntity> playerFilter, boolean sendIsOptional)
	{
		taskQueue.addLast(new ReloadChunkTask(world, pos, playerFilter, sendIsOptional));
	}
}