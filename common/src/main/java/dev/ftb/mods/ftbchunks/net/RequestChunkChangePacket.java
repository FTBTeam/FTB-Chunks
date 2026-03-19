package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Function;

public record RequestChunkChangePacket(ChunkChangeOp action, Set<XZ> chunks, boolean tryAdminChanges, Optional<UUID> teamId) implements CustomPacketPayload {
	public static final Type<RequestChunkChangePacket> TYPE = new Type<>(FTBChunksAPI.id("request_chunk_change_packet"));

	public static final StreamCodec<FriendlyByteBuf, RequestChunkChangePacket> STREAM_CODEC = StreamCodec.composite(
			NetworkHelper.enumStreamCodec(ChunkChangeOp.class), RequestChunkChangePacket::action,
			XZ.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), RequestChunkChangePacket::chunks,
			ByteBufCodecs.BOOL, RequestChunkChangePacket::tryAdminChanges,
			UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), RequestChunkChangePacket::teamId,
			RequestChunkChangePacket::new);

	@Override
	public Type<RequestChunkChangePacket> type() {
		return TYPE;
	}

	public static void handle(RequestChunkChangePacket message, PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		CommandSourceStack source = player.createCommandSourceStack();

		ChunkTeamData chunkTeamData = null;
		if (message.teamId().isPresent()) {
			Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(message.teamId().get());
			if (team.isEmpty()) {
				player.sendSystemMessage(Component.translatable("ftbteams.team_not_found", message.teamId, ChatFormatting.RED));
				return;
			}
			chunkTeamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team.get());
		}
		if (chunkTeamData == null) {
			chunkTeamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		}
		if (chunkTeamData == null) {
			return;
		}

		ChunkTeamData data = chunkTeamData;

		Function<XZ, ClaimResult> consumer = switch (message.action) {
			case CLAIM -> pos -> data.claim(source, pos.dim(player.level()), false);
			case UNCLAIM -> pos -> data.unclaim(source, pos.dim(player.level()), false, message.tryAdminChanges);
			case LOAD -> pos -> data.forceLoad(source, pos.dim(player.level()), false, message.tryAdminChanges);
			case UNLOAD -> pos -> data.unForceLoad(source, pos.dim(player.level()), false, message.tryAdminChanges);
		};

		Map<String,Integer> problems = new HashMap<>();

		int changed = 0;
		for (XZ pos : message.chunks) {
			ClaimResult r = consumer.apply(pos);
			if (!r.isSuccess()) {
				FTBChunks.LOGGER.debug("{} tried to {} @ {}:{}:{} but got result {}",
						player.getScoreboardName(), message.action.name, player.level().dimension().identifier(), pos.x(), pos.z(), r);
				problems.put(r.getResultId(), problems.getOrDefault(r.getResultId(), 0) + 1);
			} else {
				changed++;
			}
		}

		Server2PlayNetworking.send(player, new ChunkChangeResponsePacket(message.chunks.size(), changed, problems));

		SendGeneralDataPacket.send(data, player);

		if (message.teamId.isPresent()) {
			SendGeneralDataPacket.send(chunkTeamData, data.getTeam().getOnlineMembers());
		}
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
