package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SyncTXPacket
{
	public final byte[] data;

	public SyncTXPacket(byte[] d)
	{
		data = d;
	}

	SyncTXPacket(PacketBuffer buf)
	{
		data = buf.readByteArray(Integer.MAX_VALUE);
	}

	void write(PacketBuffer buf)
	{
		buf.writeByteArray(data);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			ClaimedChunkPlayerDataImpl pd = FTBChunksAPIImpl.manager.getData(p);

			for (ServerPlayerEntity p1 : p.getServer().getPlayerList().getPlayers())
			{
				if (p1 != p)
				{
					ClaimedChunkPlayerDataImpl pd1 = FTBChunksAPIImpl.manager.getData(p);

					if (pd.isAlly(p1) && pd1.isAlly(p))
					{
						FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> p1), new SyncRXPacket(data));
					}
				}
			}
		});
		context.get().setPacketHandled(true);
	}
}