package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendMapDataPacket
{
	private NetClaimedChunkData data;

	public SendMapDataPacket(NetClaimedChunkData d)
	{
		data = d;
	}

	SendMapDataPacket(PacketBuffer buf)
	{
		data = new NetClaimedChunkData();
		data.read(buf);
	}

	void write(PacketBuffer buf)
	{
		data.write(buf);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.PROXY.setMapData(data));
		context.get().setPacketHandled(true);
	}
}