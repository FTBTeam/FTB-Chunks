package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

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

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.syncRegion(key, offset, total, data));
		context.get().setPacketHandled(true);
	}
}