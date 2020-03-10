package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class NetClaimedChunkData
{
	public int claimed;
	public int loaded;
	public int maxClaimed;
	public int maxLoaded;
	public List<NetClaimedChunkGroup> groups;
	public List<NetClaimedChunk> chunks;

	void read(PacketBuffer buf)
	{
		long currentTime = new Date().getTime();

		claimed = buf.readVarInt();
		loaded = buf.readVarInt();
		maxClaimed = buf.readVarInt();
		maxLoaded = buf.readVarInt();

		int gs = buf.readVarInt();
		groups = new ArrayList<>(gs);

		for (int i = 0; i < gs; i++)
		{
			NetClaimedChunkGroup g = new NetClaimedChunkGroup();
			g.id = i;
			g.color = buf.readInt();
			g.forceLoaded = buf.readBoolean();
			g.owner = buf.readTextComponent();
			groups.add(g);
		}

		int cs = buf.readVarInt();
		chunks = new ArrayList<>(cs);

		for (int i = 0; i < cs; i++)
		{
			NetClaimedChunk c = new NetClaimedChunk();
			c.x = buf.readVarInt();
			c.z = buf.readVarInt();
			c.borders = buf.readUnsignedByte();
			c.group = groups.get(buf.readVarInt());
			c.relativeTimeClaimed = buf.readVarLong();
			c.relativeTimeForceLoaded = buf.readVarLong();

			c.claimedDate = new Date(currentTime - c.relativeTimeClaimed);

			if (c.relativeTimeForceLoaded > 0)
			{
				c.forceLoadedDate = new Date(currentTime - c.relativeTimeForceLoaded);
			}

			chunks.add(c);
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(claimed);
		buf.writeVarInt(loaded);
		buf.writeVarInt(maxClaimed);
		buf.writeVarInt(maxLoaded);

		buf.writeVarInt(groups.size());

		for (NetClaimedChunkGroup g : groups)
		{
			buf.writeInt(g.color);
			buf.writeBoolean(g.forceLoaded);
			buf.writeTextComponent(g.owner);
		}

		buf.writeVarInt(chunks.size());

		for (NetClaimedChunk c : chunks)
		{
			buf.writeVarInt(c.x);
			buf.writeVarInt(c.z);
			buf.writeByte(c.borders);
			buf.writeVarInt(c.group.id);
			buf.writeVarLong(c.relativeTimeClaimed);
			buf.writeVarLong(c.relativeTimeForceLoaded);
		}
	}
}