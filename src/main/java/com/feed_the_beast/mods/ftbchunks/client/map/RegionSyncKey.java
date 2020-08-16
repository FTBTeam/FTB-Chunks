package com.feed_the_beast.mods.ftbchunks.client.map;

import net.minecraft.network.PacketBuffer;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class RegionSyncKey
{
	public String dim;
	public int x, z;
	public int random;

	public RegionSyncKey()
	{
	}

	public RegionSyncKey(PacketBuffer buf)
	{
		dim = buf.readString(Short.MAX_VALUE);
		x = buf.readVarInt();
		z = buf.readVarInt();
		random = buf.readInt();
	}

	public void write(PacketBuffer buf)
	{
		buf.writeString(dim, Short.MAX_VALUE);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeInt(random);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		RegionSyncKey key = (RegionSyncKey) o;
		return x == key.x &&
				z == key.z &&
				random == key.random &&
				dim.equals(key.dim);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dim, x, z, random);
	}
}