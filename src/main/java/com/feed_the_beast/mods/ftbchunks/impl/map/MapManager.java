package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class MapManager
{
	public final ClaimedChunkManagerImpl manager;
	public final Map<DimensionType, MapDimension> dimensions;
	public final ArrayDeque<Runnable> taskQueue;
	public long ticks;

	public MapManager(ClaimedChunkManagerImpl m)
	{
		manager = m;
		dimensions = new HashMap<>();
		taskQueue = new ArrayDeque<>();
		ticks = 0L;
	}

	public MapDimension getDimension(DimensionType dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new MapDimension(this, d));
	}

	public MapChunk getChunk(ChunkDimPos pos)
	{
		return getDimension(pos.dimension).getRegion(XZ.regionFromChunk(pos.x, pos.z)).access().getChunk(XZ.of(pos.x, pos.z));
	}

	public void queueUpdate(World world, XZ pos, ReloadChunkTask.Callback callback)
	{
		taskQueue.addLast(new ReloadChunkTask(world, pos, callback));
	}

	public void queueUpdate(World world, XZ pos, ServerPlayerEntity player)
	{
		queueUpdate(world, pos, (task, changed) -> task.send(new CanSeeMap(player)));
	}

	public void queueSend(World world, XZ pos, Predicate<ServerPlayerEntity> sendTo)
	{
		taskQueue.addLast(new SendChunkTask(world, pos, sendTo));
	}
}