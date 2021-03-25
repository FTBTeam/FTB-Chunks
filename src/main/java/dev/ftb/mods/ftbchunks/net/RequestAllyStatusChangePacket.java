package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.data.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPIImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestAllyStatusChangePacket {
	private final UUID uuid;
	private final boolean add;

	public RequestAllyStatusChangePacket(UUID id, boolean a) {
		uuid = id;
		add = a;
	}

	RequestAllyStatusChangePacket(FriendlyByteBuf buf) {
		uuid = new UUID(buf.readLong(), buf.readLong());
		add = buf.readBoolean();
	}

	void write(FriendlyByteBuf buf) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
		buf.writeBoolean(add);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			if (add) {
				ClaimedChunkPlayerData data = FTBChunksAPIImpl.manager.getData(context.get().getSender());

				if (data.allies.contains(uuid)) {
					data.allies.remove(uuid);
					data.save();
				} else if (data.allies.add(uuid)) {
					data.save();
				}
			} else {
				ClaimedChunkPlayerData data = FTBChunksAPIImpl.manager.playerData.get(uuid);

				if (data != null && data.allies.remove(context.get().getSender().getUUID())) {
					data.save();
				}
			}
		});

		context.get().setPacketHandled(true);
	}
}