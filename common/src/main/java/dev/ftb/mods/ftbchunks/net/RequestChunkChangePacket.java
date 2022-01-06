package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class RequestChunkChangePacket extends BaseC2SMessage {
	private static final String[] ACTION_NAMES = {"claim", "unclaim", "load", "unload"};

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

	@Override
	public MessageType getType() {
		return FTBChunksNet.REQUEST_CHUNK_CHANGE;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action);
		buf.writeVarInt(chunks.size());

		for (XZ pos : chunks) {
			buf.writeVarInt(pos.x);
			buf.writeVarInt(pos.z);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		CommandSourceStack source = player.createCommandSourceStack();
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		Function<XZ, ClaimResult> consumer;
		long now = System.currentTimeMillis();

		switch (action) {
			case 0:
				consumer = pos -> {
					ClaimResult result = data.claim(source, pos.dim(player.level), false);

					if (result.isSuccess()) {
						result.setClaimedTime(now);
					}

					return result;
				};
				break;
			case 1:
				consumer = pos -> data.unclaim(source, pos.dim(player.level), false);
				break;
			case 2:
				consumer = pos -> {
					ClaimResult result = data.load(source, pos.dim(player.level), false);

					if (result.isSuccess()) {
						result.setForceLoadedTime(now);
					}

					return result;
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
			ClaimResult r = consumer.apply(pos);

			if (!r.isSuccess()) {
				FTBChunks.LOGGER.debug(String.format("%s tried to %s @ %s:%d:%d but got result %s", player.getScoreboardName(), ACTION_NAMES[action], player.level.dimension().location(), pos.x, pos.z, r));
			}
		}

		SendGeneralDataPacket.send(data, player);
	}
}