package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendChunkPacket
{
	public String dimension;
	public int x, z;
	public int color;
	public ITextComponent owner;
	public long relativeTimeClaimed;
	public long relativeTimeForceLoaded;
	public boolean forceLoaded;

	public SendChunkPacket()
	{
	}

	SendChunkPacket(PacketBuffer buf)
	{
		dimension = buf.readString(100);
		x = buf.readVarInt();
		z = buf.readVarInt();
		color = buf.readInt();

		if (color != 0)
		{
			owner = buf.readTextComponent();
			relativeTimeClaimed = buf.readVarLong();
			forceLoaded = buf.readBoolean();

			if (forceLoaded)
			{
				relativeTimeForceLoaded = buf.readVarLong();
			}
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeString(dimension, 100);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeInt(color);

		if (color != 0)
		{
			buf.writeTextComponent(owner);
			buf.writeVarLong(relativeTimeClaimed);
			buf.writeBoolean(forceLoaded);

			if (forceLoaded)
			{
				buf.writeVarLong(relativeTimeForceLoaded);
			}
		}
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateChunk(this));
		context.get().setPacketHandled(true);
	}
}