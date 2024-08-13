package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public record RequestChunkChangePacket(ChunkChangeOp action, Set<XZ> chunks, boolean tryAdminChanges, Optional<UUID> teamId) implements CustomPacketPayload {
	public static final Type<RequestChunkChangePacket> TYPE = new Type<>(FTBChunksAPI.rl("request_chunk_change_packet"));


	public static final StreamCodec<FriendlyByteBuf, RequestChunkChangePacket> STREAM_CODEC = StreamCodec.composite(
			NetworkHelper.enumStreamCodec(ChunkChangeOp.class), RequestChunkChangePacket::action,
			XZ.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), RequestChunkChangePacket::chunks,
			ByteBufCodecs.BOOL, RequestChunkChangePacket::tryAdminChanges,
			UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), RequestChunkChangePacket::teamId,
			RequestChunkChangePacket::new
	);

	@Override
	public Type<RequestChunkChangePacket> type() {
		return TYPE;
	}

	public static void handle(RequestChunkChangePacket message, NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		CommandSourceStack source = player.createCommandSourceStack();

		ChunkTeamData chunkTeamData = null;
		if(message.teamId().isPresent()) {
			Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(message.teamId().get());
			if(team.isEmpty()) {
				return;
			}
			chunkTeamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team.get());
		}
		if(chunkTeamData == null) {
			chunkTeamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		}

		ChunkTeamData data = chunkTeamData;

		Function<XZ, ClaimResult> consumer = switch (message.action) {
			case CLAIM -> pos -> data.claim(source, pos.dim(player.level()), false);
			case UNCLAIM -> pos -> data.unclaim(source, pos.dim(player.level()), false, message.tryAdminChanges);
			case LOAD -> pos -> data.forceLoad(source, pos.dim(player.level()), false, message.tryAdminChanges);
			case UNLOAD -> pos -> data.unForceLoad(source, pos.dim(player.level()), false, message.tryAdminChanges);
		};

		EnumMap<ClaimResult.StandardProblem,Integer> problems = new EnumMap<>(ClaimResult.StandardProblem.class);
		int changed = 0;
		for (XZ pos : message.chunks) {
			ClaimResult r = consumer.apply(pos);
			if (!r.isSuccess()) {
				FTBChunks.LOGGER.debug(String.format("%s tried to %s @ %s:%d:%d but got result %s", player.getScoreboardName(),
						message.action.name, player.level().dimension().location(), pos.x(), pos.z(), r));
				if (r instanceof ClaimResult.StandardProblem cr) {
					problems.put(cr, problems.getOrDefault(cr, 0) + 1);
				}
			} else {
				changed++;
			}
		}

		NetworkManager.sendToPlayer(player, new ChunkChangeResponsePacket(message.chunks.size(), changed, problems));

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