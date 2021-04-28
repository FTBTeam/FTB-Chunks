package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkTeamData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class SyncTXPacket extends MessageBase {
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

	@Override
	public void write(FriendlyByteBuf buf) {
		key.write(buf);
		buf.writeVarInt(offset);
		buf.writeVarInt(total);
		buf.writeByteArray(data);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer p = (ServerPlayer) context.getPlayer();
		ClaimedChunkTeamData pd = FTBChunksAPI.getManager().getData(p);

		for (ServerPlayer p1 : p.getServer().getPlayerList().getPlayers()) {
			if (p1 != p) {
				if (pd.isAlly(p.getUUID())) {
					new SyncRXPacket(key, offset, total, data).sendTo(p1);
				}
			}
		}
	}
}