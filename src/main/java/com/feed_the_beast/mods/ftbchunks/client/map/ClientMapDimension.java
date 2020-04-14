package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class ClientMapDimension
{
	public static ClientMapDimension current;

	public static ClientMapDimension get()
	{
		ClientPlayerEntity player = Minecraft.getInstance().player;

		if (current == null || current.clientPlayerEntity != player || current.dimension != player.dimension)
		{
			if (current != null)
			{
				current.release();
			}

			current = new ClientMapDimension(player);
		}

		return current;
	}

	private final ClientPlayerEntity clientPlayerEntity;
	public final DimensionType dimension;
	public final HashMap<XZ, ClientMapRegion> regions;

	public ClientMapDimension(ClientPlayerEntity d)
	{
		clientPlayerEntity = d;
		dimension = clientPlayerEntity.dimension;
		regions = new HashMap<>();
	}

	public ClientMapRegion getRegion(XZ pos)
	{
		return regions.computeIfAbsent(pos, p -> new ClientMapRegion(this, p));
	}

	public void release()
	{
	}

	public int getHeight(ClientMapChunk chunk, int x, int z)
	{
		int hcx = x >> 4;
		int hcz = z >> 4;

		if (hcx == (chunk.pos.x + chunk.region.pos.x * 16) && hcz == (chunk.pos.z + chunk.region.pos.z * 16))
		{
			return chunk.getHeight(x, z);
		}

		return getRegion(XZ.regionFromBlock(x, z)).getChunk(XZ.chunkFromBlock(x, z)).getHeight(x, z);
	}
}