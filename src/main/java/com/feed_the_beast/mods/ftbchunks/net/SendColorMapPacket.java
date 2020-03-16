package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendColorMapPacket
{
	private Map<ResourceLocation, Integer> colorMap;

	public SendColorMapPacket(Map<ResourceLocation, Integer> c)
	{
		colorMap = c;
	}

	SendColorMapPacket(PacketBuffer buf)
	{
		int s = buf.readVarInt();
		colorMap = new HashMap<>(s);

		for (int i = 0; i < s; i++)
		{
			ResourceLocation rl = buf.readResourceLocation();
			int c = buf.readInt();
			colorMap.put(rl, c);
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(colorMap.size());

		for (Map.Entry<ResourceLocation, Integer> entry : colorMap.entrySet())
		{
			buf.writeResourceLocation(entry.getKey());
			buf.writeInt(entry.getValue());
		}
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.setColorMap(colorMap));
		context.get().setPacketHandled(true);
	}
}