package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author LatvianModder
 */
public class SyncRXPacket extends BaseS2CMessage {
	public final RegionSyncKey key;
	public final int offset;
	public final int total;
	public final byte[] data;

	public SyncRXPacket(RegionSyncKey k, int o, int t, byte[] d) {
		key = k;
		offset = o;
		total = t;
		data = d;
	}

	SyncRXPacket(FriendlyByteBuf buf) {
		key = new RegionSyncKey(buf);
		offset = buf.readVarInt();
		total = buf.readVarInt();
		data = buf.readByteArray(Integer.MAX_VALUE);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SYNC_RX;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		key.write(buf);
		buf.writeVarInt(offset);
		buf.writeVarInt(total);
		buf.writeByteArray(data);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.syncRegion(key, offset, total, data);
	}
}