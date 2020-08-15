package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SyncRXPacket
{
	public final byte[] data;

	public SyncRXPacket(byte[] d)
	{
		data = d;
	}

	SyncRXPacket(PacketBuffer buf)
	{
		data = buf.readByteArray(Integer.MAX_VALUE);
	}

	void write(PacketBuffer buf)
	{
		buf.writeByteArray(data);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.syncRegion(data));
		context.get().setPacketHandled(true);
	}
}