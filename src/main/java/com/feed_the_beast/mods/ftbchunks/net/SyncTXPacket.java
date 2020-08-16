package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
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
	public final RegionSyncKey key;
	public final int offset;
	public final int total;
	public final byte[] data;

	public SyncTXPacket(RegionSyncKey k, int o, int t, byte[] d)
	{
		key = k;
		offset = o;
		total = t;
		data = d;
	}

	SyncTXPacket(PacketBuffer buf)
	{
		key = new RegionSyncKey(buf);
		offset = buf.readVarInt();
		total = buf.readVarInt();
		data = buf.readByteArray(Integer.MAX_VALUE);
	}

	void write(PacketBuffer buf)
	{
		key.write(buf);
		buf.writeVarInt(offset);
		buf.writeVarInt(total);
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
						FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> p1), new SyncRXPacket(key, offset, total, data));
					}
				}
			}
		});

		context.get().setPacketHandled(true);
	}
}