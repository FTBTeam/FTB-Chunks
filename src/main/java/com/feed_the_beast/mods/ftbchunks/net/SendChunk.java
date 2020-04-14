package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendChunk
{
	private final int chunkX, chunkZ;
	private final byte[] imageData;

	public SendChunk(int cx, int cz, byte[] img)
	{
		chunkX = cx;
		chunkZ = cz;
		imageData = img;
	}

	SendChunk(PacketBuffer buf)
	{
		chunkX = buf.readVarInt();
		chunkZ = buf.readVarInt();
		imageData = buf.readByteArray();
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(chunkX);
		buf.writeVarInt(chunkZ);
		buf.writeByteArray(imageData);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateChunk(chunkX, chunkZ, imageData));
		context.get().setPacketHandled(true);
	}
}