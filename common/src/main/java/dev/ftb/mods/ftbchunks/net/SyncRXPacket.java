package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author LatvianModder
 */
public class SyncRXPacket extends SyncTXPacket {
	public SyncRXPacket(RegionSyncKey k, int o, int t, byte[] d) {
		super(k, o, t, d);
	}

	SyncRXPacket(FriendlyByteBuf buf) {
		super(buf);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.syncRegion(key, offset, total, data);
	}
}