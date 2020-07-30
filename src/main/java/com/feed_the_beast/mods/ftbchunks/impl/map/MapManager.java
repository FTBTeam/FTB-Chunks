package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class MapManager
{
	public final ClaimedChunkManagerImpl manager;
	public final Map<String, MapDimension> dimensions;
	public final ArrayDeque<MapTask> taskQueue;
	public long taskQueueTicks;

	public MapManager(ClaimedChunkManagerImpl m)
	{
		manager = m;
		dimensions = new HashMap<>();
		taskQueue = new ArrayDeque<>();
		taskQueueTicks = 0L;
	}

	public MapDimension getDimension(String dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new MapDimension(this, d));
	}

	public MapDimension getDimension(IWorld world)
	{
		return getDimension(Objects.requireNonNull(ChunkDimPos.getID(world)));
	}

	public MapChunk getChunk(String dim, XZ chunkPos)
	{
		return getDimension(dim).getRegion(XZ.regionFromChunk(chunkPos.x, chunkPos.z)).access().getChunk(chunkPos);
	}

	public MapChunk getChunk(ChunkDimPos pos)
	{
		return getDimension(pos.dimension).getRegion(XZ.regionFromChunk(pos.x, pos.z)).access().getChunk(XZ.of(pos.x, pos.z));
	}

	public void queue(MapTask task)
	{
		taskQueue.addLast(task);
	}

	public void queueUpdate(World world, XZ pos, ReloadChunkTask.Callback callback)
	{
		queue(new ReloadChunkTask(world, pos, callback));
	}

	public void queueUpdate(World world, XZ pos, ServerPlayerEntity player)
	{
		queueUpdate(world, pos, (task, changed) -> task.send(new CanSeeMap(player)));
	}

	public void queueSend(World world, XZ pos, Predicate<ServerPlayerEntity> sendTo)
	{
		queue(new SendChunkTask(world, pos, sendTo));
	}
}