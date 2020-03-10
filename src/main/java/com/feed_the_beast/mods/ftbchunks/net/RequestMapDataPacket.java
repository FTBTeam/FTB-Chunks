package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestMapDataPacket
{
	public RequestMapDataPacket()
	{
	}

	RequestMapDataPacket(PacketBuffer buf)
	{
	}

	void write(PacketBuffer buf)
	{
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> SendMapDataPacket.send(Objects.requireNonNull(context.get().getSender())));
		context.get().setPacketHandled(true);
	}
}