package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class PlayerDeathPacket
{
	public final String dimension;
	public final int x, z, number;

	public PlayerDeathPacket(String dim, int _x, int _z, int num)
	{
		dimension = dim;
		x = _x;
		z = _z;
		number = num;
	}

	PlayerDeathPacket(PacketBuffer buf)
	{
		dimension = buf.readString(Short.MAX_VALUE);
		x = buf.readVarInt();
		z = buf.readVarInt();
		number = buf.readVarInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeString(dimension, Short.MAX_VALUE);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeVarInt(number);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.playerDeath(this));
		context.get().setPacketHandled(true);
	}
}