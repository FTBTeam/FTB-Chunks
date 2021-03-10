package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SyncTXPacket {
	public final RegionSyncKey key;
	public final int offset;
	public final int total;
	public final byte[] data;

	public SyncTXPacket(RegionSyncKey k, int o, int t, byte[] d) {
		key = k;
		offset = o;
		total = t;
		data = d;
	}

	SyncTXPacket(FriendlyByteBuf buf) {
		key = new RegionSyncKey(buf);
		offset = buf.readVarInt();
		total = buf.readVarInt();
		data = buf.readByteArray(Integer.MAX_VALUE);
	}

	void write(FriendlyByteBuf buf) {
		key.write(buf);
		buf.writeVarInt(offset);
		buf.writeVarInt(total);
		buf.writeByteArray(data);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer p = context.get().getSender();
			ClaimedChunkPlayerDataImpl pd = FTBChunksAPIImpl.manager.getData(p);

			for (ServerPlayer p1 : p.getServer().getPlayerList().getPlayers()) {
				if (p1 != p) {
					if (pd.isAlly(FTBChunksAPIImpl.manager.getData(p))) {
						FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> p1), new SyncRXPacket(key, offset, total, data));
					}
				}
			}
		});

		context.get().setPacketHandled(true);
	}
}