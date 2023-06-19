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

/**
 * @author LatvianModder
 */
public class SendGeneralDataPacket extends BaseS2CMessage {
	public final int claimed;
	public final int loaded;
	public final int maxClaimChunks;
	public final int maxForceLoadChunks;

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

	private SendGeneralDataPacket(int claimed, int loaded, int maxClaimChunks, int maxForceLoadChunks) {
		this.claimed = claimed;
		this.loaded = loaded;
		this.maxClaimChunks = maxClaimChunks;
		this.maxForceLoadChunks = maxForceLoadChunks;
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_GENERAL_DATA;
	}

	SendGeneralDataPacket(FriendlyByteBuf buf) {
		claimed = buf.readVarInt();
		loaded = buf.readVarInt();
		maxClaimChunks = buf.readVarInt();
		maxForceLoadChunks = buf.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(claimed);
		buf.writeVarInt(loaded);
		buf.writeVarInt(maxClaimChunks);
		buf.writeVarInt(maxForceLoadChunks);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateGeneralData(this);
	}
}