package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestMapDataPacket
{
	public final int fromX, fromZ, toX, toZ;

	public RequestMapDataPacket(int fx, int fz, int tx, int tz)
	{
		fromX = fx;
		fromZ = fz;
		toX = tx;
		toZ = tz;
	}

	RequestMapDataPacket(PacketBuffer buf)
	{
		fromX = buf.readVarInt();
		fromZ = buf.readVarInt();
		toX = buf.readVarInt();
		toZ = buf.readVarInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(fromX);
		buf.writeVarInt(fromZ);
		buf.writeVarInt(toX);
		buf.writeVarInt(toZ);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		//FIXME: context.get().enqueueWork(() -> SendMapDataPacket.send(Objects.requireNonNull(context.get().getSender())));
		context.get().setPacketHandled(true);
	}
}