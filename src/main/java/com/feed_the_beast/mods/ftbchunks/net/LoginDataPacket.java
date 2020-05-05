package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class LoginDataPacket
{
	public final UUID serverId;

	public LoginDataPacket(UUID id)
	{
		serverId = id;
	}

	LoginDataPacket(PacketBuffer buf)
	{
		serverId = new UUID(buf.readLong(), buf.readLong());
	}

	void write(PacketBuffer buf)
	{
		buf.writeLong(serverId.getMostSignificantBits());
		buf.writeLong(serverId.getLeastSignificantBits());
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.login(this));
		context.get().setPacketHandled(true);
	}
}