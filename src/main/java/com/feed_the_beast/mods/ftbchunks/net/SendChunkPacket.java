package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendChunkPacket
{
	public DimensionType dimension;
	public int x, z;
	public byte[] imageData;
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
		dimension = DimensionType.getById(buf.readVarInt());
		x = buf.readVarInt();
		z = buf.readVarInt();
		imageData = buf.readByteArray();
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
		buf.writeVarInt(dimension.getId());
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeByteArray(imageData);
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