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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Sent by client to claim/unclaim/load/unload one or more chunks.
 * <ul>
 *     <li>If {@code tryAdminChanges} is true, the requesting player must be an admin</li>
 *     <li>If a non-empty {@code teamId} is supplied, the requesting player must either be an admin, or be a member of that team</li>
 * </ul>
 *
 * @param action the action to carry out
 * @param chunks positions of the chunks to change
 * @param tryAdminChanges if true, chunks owned by a team other than the operating team can be affected
 * @param teamId if non-empty, operate as the team for this ID; otherwise, operate as the requesting player's team
 */
public record RequestChunkChangePacket(ChunkChangeOp action, Set<XZ> chunks, boolean tryAdminChanges, Optional<UUID> teamId) implements CustomPacketPayload {
	public static final Type<RequestChunkChangePacket> TYPE = new Type<>(FTBChunksAPI.rl("request_chunk_change_packet"));

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

	public static void handle(RequestChunkChangePacket message, NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		CommandSourceStack source = player.createCommandSourceStack();

		ChunkTeamData data = getTeamData(player, message);
		if (data == null) {
			return;
		}

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
						player.getScoreboardName(), message.action.name, player.level().dimension().location(), pos.x(), pos.z(), r);
				problems.put(r.getResultId(), problems.getOrDefault(r.getResultId(), 0) + 1);
			} else {
				changed++;
			}
		}

		NetworkManager.sendToPlayer(player, new ChunkChangeResponsePacket(message.chunks.size(), changed, problems));

		SendGeneralDataPacket.send(data, player);

		if (message.teamId.isPresent()) {
			SendGeneralDataPacket.send(data, data.getTeam().getOnlineMembers());
		}
	}

	@Nullable
	private static ChunkTeamData getTeamData(ServerPlayer player, RequestChunkChangePacket message) {
		return message.teamId().map(teamId ->
				FTBTeamsAPI.api().getManager().getTeamByID(teamId).map(team -> {
					ChunkTeamData data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
					if (!player.hasPermissions(Commands.LEVEL_GAMEMASTERS) && !data.getTeam().getMembers().contains(player.getUUID())) {
						// player must be an admin to make changes as a different team!
						return null;
					}
					return data;
				}).orElseGet(() -> {
					player.sendSystemMessage(Component.translatable("ftbteams.team_not_found", teamId, ChatFormatting.RED));
					return null;
				})
		).orElseGet(() -> ClaimedChunkManagerImpl.getInstance().getOrCreateData(player));
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