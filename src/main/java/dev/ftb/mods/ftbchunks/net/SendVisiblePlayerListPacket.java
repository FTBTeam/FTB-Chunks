package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.impl.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.impl.PlayerLocation;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendVisiblePlayerListPacket {
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

		for (ServerPlayer player : FTBChunksAPIImpl.manager.server.getPlayerList().getPlayers()) {
			VisiblePlayerListItem item = new VisiblePlayerListItem();
			item.player = player;
			item.data = FTBChunksAPIImpl.manager.getData(player);
			item.location = new PlayerLocation(player);
			playerList.add(item);
		}

		for (VisiblePlayerListItem self : playerList) {
			ResourceKey<Level> dim = self.player.level.dimension();
			List<PlayerLocation> players = new ArrayList<>();

			for (VisiblePlayerListItem other : playerList) {
				if (other.player.level == self.player.level && self.data.canUse(other.player, self.data.locationMode, false)) {
					players.add(other.location);
				}
			}

			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> self.player), new SendVisiblePlayerListPacket(players, dim));
		}
	}

	void write(FriendlyByteBuf buf) {
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

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateVisiblePlayerList(this));
		context.get().setPacketHandled(true);
	}
}