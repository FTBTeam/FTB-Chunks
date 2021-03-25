package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.XZ;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestChunkChangePacket {
	private final int action;
	private final Set<XZ> chunks;

	public RequestChunkChangePacket(int a, Set<XZ> c) {
		action = a;
		chunks = c;
	}

	RequestChunkChangePacket(FriendlyByteBuf buf) {
		action = buf.readVarInt();
		int s = buf.readVarInt();
		chunks = new LinkedHashSet<>(s);

		for (int i = 0; i < s; i++) {
			int x = buf.readVarInt();
			int z = buf.readVarInt();
			chunks.add(XZ.of(x, z));
		}
	}

	void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action);
		buf.writeVarInt(chunks.size());

		for (XZ pos : chunks) {
			buf.writeVarInt(pos.x);
			buf.writeVarInt(pos.z);
		}
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			CommandSourceStack source = player.createCommandSourceStack();
			ClaimedChunkPlayerData data = FTBChunksAPI.INSTANCE.getManager().getData(player);
			Consumer<XZ> consumer;
			Instant time = Instant.now();

			switch (action) {
				case 0:
					consumer = pos -> {
						ClaimResult result = data.claim(source, pos.dim(player.level), false);

						if (result.isSuccess()) {
							result.setClaimedTime(time);
						}
					};
					break;
				case 1:
					consumer = pos -> data.unclaim(source, pos.dim(player.level), false);
					break;
				case 2:
					consumer = pos -> {
						ClaimResult result = data.load(source, pos.dim(player.level), false);

						if (result.isSuccess()) {
							result.setForceLoadedTime(time);
						}
					};
					break;
				case 3:
					consumer = pos -> data.unload(source, pos.dim(player.level), false);
					break;
				default:
					FTBChunks.LOGGER.warn("Unknown chunk action ID: " + action);
					return;
			}

			for (XZ pos : chunks) {
				consumer.accept(pos);
				//FIXME: FTBChunksAPIImpl.manager.map.queueSend(player.world, pos, p -> p == player);
			}

			SendGeneralDataPacket.send(data, player);
		});

		context.get().setPacketHandled(true);
	}
}