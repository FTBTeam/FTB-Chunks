package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ClientMapRegion
{
	public final ClientMapDimension dimension;
	public final XZ pos;
	public final Map<XZ, ClientMapChunk> chunks;
	public boolean updateTexture;

	public ClientMapRegion(ClientMapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		chunks = new HashMap<>();
		updateTexture = false;
	}

	public ClientMapChunk getChunk(XZ pos)
	{
		if (pos.x != (pos.x & 31) || pos.z != (pos.z & 31))
		{
			pos = XZ.of(pos.x & 31, pos.z & 31);
		}

		return chunks.computeIfAbsent(pos, p -> new ClientMapChunk(this, p));
	}
}