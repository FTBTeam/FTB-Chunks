package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.client.VisibleClientPlayers;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerVisibilityPacket extends BaseS2CMessage {
	private final List<UUID> uuids;

	private PlayerVisibilityPacket(List<UUID> uuids) {
		this.uuids = uuids;
	}

	PlayerVisibilityPacket(FriendlyByteBuf buf) {
		uuids = buf.readList(FriendlyByteBuf::readUUID);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_VISIBLE_PLAYER_LIST;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(uuids, FriendlyByteBuf::writeUUID);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		VisibleClientPlayers.updatePlayerList(uuids);
	}

	public static void syncToLevel(Level level) {
		if (level instanceof ServerLevel serverLevel) {
			syncToPlayers(serverLevel.getPlayers(p -> true));
		}
	}

	public static void syncToAll() {
		syncToPlayers(ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getPlayerList().getPlayers());
	}

	public static void syncToPlayers(List<ServerPlayer> players) {
		List<VisiblePlayerItem> playerList;

		if (players == null) {
			players = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getPlayerList().getPlayers();
		}
		playerList = players.stream()
				.map(player -> new VisiblePlayerItem(player, ClaimedChunkManagerImpl.getInstance().getOrCreateData(player)))
				.collect(Collectors.toList());

		boolean override = FTBChunksWorldConfig.LOCATION_MODE_OVERRIDE.get();
		for (VisiblePlayerItem recipient : playerList) {
			List<UUID> playerIds = new ArrayList<>();

			for (VisiblePlayerItem other : playerList) {
				if (override || recipient.player.hasPermissions(2) || other.data.canPlayerUse(recipient.player, FTBChunksProperties.LOCATION_MODE)) {
					playerIds.add(other.player.getUUID());
				}
			}

			new PlayerVisibilityPacket(playerIds).sendTo(recipient.player);
		}
	}

	private record VisiblePlayerItem(ServerPlayer player, ChunkTeamDataImpl data) {
	}
}