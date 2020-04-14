package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendGeneralData
{
	public static void send(ServerPlayerEntity player)
	{
		SendGeneralData data = new SendGeneralData();

		data.maxClaimed = FTBChunksConfig.getMaxClaimedChunks(player);
		data.maxLoaded = FTBChunksConfig.getMaxForceLoadedChunks(player);

		for (ClaimedChunk chunk : FTBChunksAPI.INSTANCE.getManager().getData(player).getClaimedChunks())
		{
			data.claimed++;

			if (chunk.isForceLoaded())
			{
				data.loaded++;
			}
		}

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), data);
	}

	public int claimed;
	public int loaded;
	public int maxClaimed;
	public int maxLoaded;

	public SendGeneralData()
	{
	}

	SendGeneralData(PacketBuffer buf)
	{
		claimed = buf.readVarInt();
		loaded = buf.readVarInt();
		maxClaimed = buf.readVarInt();
		maxLoaded = buf.readVarInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(claimed);
		buf.writeVarInt(loaded);
		buf.writeVarInt(maxClaimed);
		buf.writeVarInt(maxLoaded);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateGeneralData(this));
		context.get().setPacketHandled(true);
	}
}