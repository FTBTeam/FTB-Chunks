package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.client.VisibleClientPlayers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class SendVisiblePlayerListPacket extends BaseS2CMessage {
	public final List<UUID> uuids;

	private SendVisiblePlayerListPacket(List<UUID> uuids) {
		this.uuids = uuids;
	}

	SendVisiblePlayerListPacket(FriendlyByteBuf buf) {
		uuids = new ArrayList<>();
		int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			uuids.add(buf.readUUID());
		}
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_VISIBLE_PLAYER_LIST;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(uuids.size());
		uuids.forEach(buf::writeUUID);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		VisibleClientPlayers.updatePlayerList(uuids);
	}

	public static void syncToLevel(Level level) {
		syncToPlayers(FTBChunksAPI.getManager().getMinecraftServer().getPlayerList().getPlayers().stream().filter(p -> p.level == level).toList());
	}

	public static void syncToAll() {
		syncToPlayers(FTBChunksAPI.getManager().getMinecraftServer().getPlayerList().getPlayers());
	}

	public static void syncToPlayers(List<ServerPlayer> players) {
		List<VisiblePlayerItem> playerList;

		if (players == null) {
			players = FTBChunksAPI.getManager().getMinecraftServer().getPlayerList().getPlayers();
		}
		playerList = players.stream()
				.map(player -> new VisiblePlayerItem(player, FTBChunksAPI.getManager().getData(player)))
				.collect(Collectors.toList());

		for (VisiblePlayerItem recipient : playerList) {
			List<UUID> playerIds = new ArrayList<>();

			for (VisiblePlayerItem other : playerList) {
				if (recipient.player.hasPermissions(2) || other.data.canUse(recipient.player, FTBChunksTeamData.LOCATION_MODE)) {
					playerIds.add(other.player.getUUID());
				}
			}

			new SendVisiblePlayerListPacket(playerIds).sendTo(recipient.player);
		}
	}

	private record VisiblePlayerItem(ServerPlayer player, FTBChunksTeamData data) {
	}
}