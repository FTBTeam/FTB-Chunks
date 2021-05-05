package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.data.PlayerLocation;
import dev.ftb.mods.ftblibrary.net.snm.BaseS2CPacket;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class SendVisiblePlayerListPacket extends BaseS2CPacket {
	public final List<PlayerLocation> players;
	public final ResourceKey<Level> dim;

	public SendVisiblePlayerListPacket(List<PlayerLocation> p, ResourceKey<Level> d) {
		players = p;
		dim = d;
	}

	SendVisiblePlayerListPacket(FriendlyByteBuf buf) {
		int s = buf.readVarInt();
		players = new ArrayList<>(s);

		for (int i = 0; i < s; i++) {
			PlayerLocation p = new PlayerLocation();
			long most = buf.readLong();
			long least = buf.readLong();
			p.uuid = new UUID(most, least);
			p.name = buf.readUtf(Short.MAX_VALUE);
			p.x = buf.readVarInt();
			p.z = buf.readVarInt();
			players.add(p);
		}

		dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
	}

	public static void sendAll() {
		List<VisiblePlayerListItem> playerList = new ArrayList<>();

		for (ServerPlayer player : FTBChunksAPI.getManager().teamManager.server.getPlayerList().getPlayers()) {
			VisiblePlayerListItem item = new VisiblePlayerListItem();
			item.player = player;
			item.data = FTBChunksAPI.getManager().getData(player);
			item.location = new PlayerLocation(player);
			playerList.add(item);
		}

		for (VisiblePlayerListItem self : playerList) {
			ResourceKey<Level> dim = self.player.level.dimension();
			List<PlayerLocation> players = new ArrayList<>();

			for (VisiblePlayerListItem other : playerList) {
				if (other.player.level == self.player.level && self.data.canUse(other.player, FTBChunksTeamData.LOCATION_MODE)) {
					players.add(other.location);
				}
			}

			new SendVisiblePlayerListPacket(players, dim).sendTo(self.player);
		}
	}

	@Override
	public PacketID getId() {
		return FTBChunksNet.SEND_VISIBLE_PLAYER_LIST;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(players.size());

		for (PlayerLocation p : players) {
			buf.writeLong(p.uuid.getMostSignificantBits());
			buf.writeLong(p.uuid.getLeastSignificantBits());
			buf.writeUtf(p.name, Short.MAX_VALUE);
			buf.writeVarInt(p.x);
			buf.writeVarInt(p.z);
		}

		buf.writeResourceLocation(dim.location());
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateVisiblePlayerList(this);
	}
}