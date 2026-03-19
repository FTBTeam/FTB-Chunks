package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public record SendGeneralDataPacket(GeneralChunkData data) implements CustomPacketPayload {
    public static final Type<SendGeneralDataPacket> TYPE = new Type<>(FTBChunksAPI.id("send_general_data_packet"));

    public static final StreamCodec<FriendlyByteBuf, SendGeneralDataPacket> STREAM_CODEC = StreamCodec.composite(
	    GeneralChunkData.STREAM_CODEC, SendGeneralDataPacket::data,
	    SendGeneralDataPacket::new
    );

	@Override
    public Type<SendGeneralDataPacket> type() {
        return TYPE;
	}

    public static void handle(SendGeneralDataPacket message, PacketContext context) {
		context.enqueue(() -> FTBChunksClient.INSTANCE.updateGeneralData(message.data));
	}

	public static void send(ChunkTeamData teamData, ServerPlayer player) {
		send(teamData, List.of(player));
	}

	public static void send(ChunkTeamData teamData, Collection<ServerPlayer> players) {
		var cc = teamData.getClaimedChunks();
		int loaded = (int) cc.stream().filter(ClaimedChunk::isForceLoaded).count();
		SendGeneralDataPacket data = new SendGeneralDataPacket(new GeneralChunkData(cc.size(), loaded, teamData.getMaxClaimChunks(), teamData.getMaxForceLoadChunks()));

		players.forEach(player ->
				teamData.getTeamManager().getTeamForPlayer(player).ifPresent(team -> {
					if (team.getId().equals(teamData.getTeam().getId())) {
						Server2PlayNetworking.send(player, data);
					}
				}));
	}

	public record GeneralChunkData(int claimed, int loaded, int maxClaimChunks, int maxForceLoadChunks) {
		public static final GeneralChunkData NONE = new GeneralChunkData(0, 0, 0, 0);

		public static StreamCodec<FriendlyByteBuf, GeneralChunkData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, GeneralChunkData::claimed,
				ByteBufCodecs.VAR_INT, GeneralChunkData::loaded,
				ByteBufCodecs.VAR_INT, GeneralChunkData::maxClaimChunks,
				ByteBufCodecs.VAR_INT, GeneralChunkData::maxForceLoadChunks,
				GeneralChunkData::new
		);
	}
}
