package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class SendGeneralDataPacket extends BaseS2CMessage {
	private final GeneralChunkData data;

	private SendGeneralDataPacket(int claimed, int loaded, int maxClaimChunks, int maxForceLoadChunks) {
		data = new GeneralChunkData(claimed, loaded, maxClaimChunks, maxForceLoadChunks);
	}

	SendGeneralDataPacket(FriendlyByteBuf buf) {
		data = GeneralChunkData.fromNetwork(buf);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_GENERAL_DATA;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		data.toNetwork(buf);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateGeneralData(data);
	}

	public static void send(FTBChunksTeamData teamData, ServerPlayer player) {
		send(teamData, List.of(player));
	}

	public static void send(FTBChunksTeamData teamData, Collection<ServerPlayer> players) {
		Collection<ClaimedChunk> cc = teamData.getClaimedChunks();
		int loaded = (int) cc.stream().filter(ClaimedChunk::isForceLoaded).count();
		SendGeneralDataPacket data = new SendGeneralDataPacket(cc.size(), loaded, teamData.getMaxClaimChunks(), teamData.getMaxForceLoadChunks());

		players.forEach(player ->
				teamData.getTeamManager().getTeamForPlayer(player).ifPresent(team -> {
					if (team.getId().equals(teamData.getTeamId())) {
						data.sendTo(player);
					}
				}));
	}

	public record GeneralChunkData(int claimed, int loaded, int maxClaimChunks, int maxForceLoadChunks) {
		public static GeneralChunkData fromNetwork(FriendlyByteBuf buf) {
			return new GeneralChunkData(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
		}

		public void toNetwork(FriendlyByteBuf buf) {
			buf.writeVarInt(claimed);
			buf.writeVarInt(loaded);
			buf.writeVarInt(maxClaimChunks);
			buf.writeVarInt(maxForceLoadChunks);
		}
	}
}