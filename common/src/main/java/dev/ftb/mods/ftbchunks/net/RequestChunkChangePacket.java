package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimResults;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class RequestChunkChangePacket extends BaseC2SMessage {
	private static final String[] ACTION_NAMES = {"claim", "unclaim", "load", "unload"};
	public static final int CLAIM = 0;
	public static final int UNCLAIM = 1;
	public static final int LOAD = 2;
	public static final int UNLOAD = 3;

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
			buf.writeVarInt(pos.x());
			buf.writeVarInt(pos.z());
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		CommandSourceStack source = player.createCommandSourceStack();
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		Function<XZ, ClaimResult> consumer;

		switch (action) {
			case CLAIM -> consumer = pos -> data.claim(source, pos.dim(player.level()), false);
			case UNCLAIM -> consumer = pos -> data.unclaim(source, pos.dim(player.level()), false);
			case LOAD -> consumer = pos -> data.load(source, pos.dim(player.level()), false);
			case UNLOAD -> consumer = pos -> data.unload(source, pos.dim(player.level()), false);
			default -> {
				FTBChunks.LOGGER.warn("Unknown chunk action ID: " + action);
				return;
			}
		}

		EnumMap<ClaimResults,Integer> problems = new EnumMap<>(ClaimResults.class);
		int changed = 0;
		for (XZ pos : chunks) {
			ClaimResult r = consumer.apply(pos);

			if (!r.isSuccess()) {
				FTBChunks.LOGGER.debug(String.format("%s tried to %s @ %s:%d:%d but got result %s", player.getScoreboardName(), ACTION_NAMES[action], player.level().dimension().location(), pos.x(), pos.z(), r));
				if (r instanceof ClaimResults cr) {
					problems.put(cr, problems.getOrDefault(cr, 0) + 1);
				}
			} else {
				changed++;
			}
		}

		new ChunkChangeResponsePacket(chunks.size(), changed, problems).sendTo(player);

		SendGeneralDataPacket.send(data, player);
	}
}