package dev.ftb.mods.ftbchunks.net;

import com.google.common.collect.Sets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Set;
import java.util.function.Function;

public class RequestChunkChangePacket extends BaseC2SMessage {
	private final ChunkChangeOp action;
	private final Set<XZ> chunks;

	public RequestChunkChangePacket(ChunkChangeOp action, Set<XZ> chunks) {
		this.action = action;
		this.chunks = chunks;
	}

	RequestChunkChangePacket(FriendlyByteBuf buf) {
		action = buf.readEnum(ChunkChangeOp.class);
		chunks = buf.readCollection(Sets::newHashSetWithExpectedSize, buf1 -> XZ.of(buf1.readVarInt(), buf1.readVarInt()));
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.REQUEST_CHUNK_CHANGE;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeEnum(action);
		buf.writeCollection(chunks, (buf1, xz) -> {
			buf1.writeVarInt(xz.x());
			buf1.writeVarInt(xz.z());
		});
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		CommandSourceStack source = player.createCommandSourceStack();
		ChunkTeamData data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		Function<XZ, ClaimResult> consumer = switch (action) {
			case CLAIM -> pos -> data.claim(source, pos.dim(player.level()), false);
			case UNCLAIM -> pos -> data.unclaim(source, pos.dim(player.level()), false);
			case LOAD -> pos -> data.forceLoad(source, pos.dim(player.level()), false);
			case UNLOAD -> pos -> data.unForceLoad(source, pos.dim(player.level()), false);
		};

		EnumMap<ClaimResult.StandardProblem,Integer> problems = new EnumMap<>(ClaimResult.StandardProblem.class);
		int changed = 0;
		for (XZ pos : chunks) {
			ClaimResult r = consumer.apply(pos);
			if (!r.isSuccess()) {
				FTBChunks.LOGGER.debug(String.format("%s tried to %s @ %s:%d:%d but got result %s", player.getScoreboardName(),
						action.name, player.level().dimension().location(), pos.x(), pos.z(), r));
				if (r instanceof ClaimResult.StandardProblem cr) {
					problems.put(cr, problems.getOrDefault(cr, 0) + 1);
				}
			} else {
				changed++;
			}
		}

		new ChunkChangeResponsePacket(chunks.size(), changed, problems).sendTo(player);

		SendGeneralDataPacket.send(data, player);
	}

	public enum ChunkChangeOp {
		CLAIM("claim"),
		UNCLAIM("unclaim"),
		LOAD("load"),
		UNLOAD("unload");

		private final String name;

		ChunkChangeOp(String name) {
			this.name = name;
		}

		public static ChunkChangeOp create(boolean isLeftMouse, boolean isShift) {
			return isShift ?
					(isLeftMouse ? ChunkChangeOp.LOAD : ChunkChangeOp.UNLOAD) :
					(isLeftMouse ? ChunkChangeOp.CLAIM : ChunkChangeOp.UNCLAIM);

		}
	}
}