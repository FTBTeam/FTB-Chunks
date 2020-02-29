package com.feed_the_beast.mods.ftbchunks.client;

import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author LatvianModder
 */
public class MinimapData
{
	public static MinimapData instance;

	public final UUID id;
	public final Map<DimensionType, MinimapDimension> dimensions;
	private ExecutorService renderExecutor;

	public MinimapData(UUID uuid)
	{
		id = uuid;
		dimensions = new HashMap<>();
		renderExecutor = Executors.newSingleThreadExecutor();
	}

	public MinimapDimension getDimension(DimensionType type)
	{
		return dimensions.computeIfAbsent(type, t -> new MinimapDimension(this, t));
	}

	public void releaseData()
	{
		if (renderExecutor != null)
		{
			renderExecutor.shutdown();
		}

		renderExecutor = null;
	}

	public void render(MinimapArea area)
	{
		renderExecutor.execute(() -> {
			System.out.println(area.dim.type.getRegistryName() + " @ " + area.pos);
		});
	}
}