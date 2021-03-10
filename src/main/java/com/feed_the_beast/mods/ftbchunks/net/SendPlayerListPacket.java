package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.impl.AllyMode;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.impl.KnownFakePlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendPlayerListPacket {
	public static class NetPlayer implements Comparable<NetPlayer> {
		public static final int FAKE = 1;
		public static final int ALLY = 2;
		public static final int BANNED = 4;
		public static final int ALLY_BACK = 8;

		public final UUID uuid;
		public final String name;
		public int flags;

		public NetPlayer(UUID u, String n, int f) {
			uuid = u;
			name = n;
			flags = f;
		}

		public boolean isFake() {
			return (flags & FAKE) != 0;
		}

		public boolean isAlly() {
			return (flags & ALLY) != 0;
		}

		public boolean isAllyBack() {
			return isFake() || (flags & ALLY_BACK) != 0;
		}

		public boolean isBanned() {
			return (flags & BANNED) != 0;
		}

		@Override
		public int compareTo(NetPlayer o) {
			int i = Boolean.compare(isFake(), o.isFake());
			return i == 0 ? name.compareToIgnoreCase(o.name) : i;
		}
	}

	public final List<NetPlayer> players;
	public final int allyMode;

	public SendPlayerListPacket(List<NetPlayer> p, int a) {
		players = p;
		allyMode = a;
	}

	SendPlayerListPacket(FriendlyByteBuf buf) {
		int s = buf.readVarInt();
		players = new ArrayList<>(s);

		for (int i = 0; i < s; i++) {
			long most = buf.readLong();
			long least = buf.readLong();
			String name = buf.readUtf(50);
			int flags = buf.readVarInt();
			players.add(new NetPlayer(new UUID(most, least), name, flags));
		}

		allyMode = buf.readUnsignedByte();
	}

	public static void send(ServerPlayer player) {
		ClaimedChunkPlayerDataImpl self = FTBChunksAPIImpl.manager.getData(player);
		int allyMode = FTBChunksConfig.allyMode == AllyMode.FORCED_ALL ? 2 : FTBChunksConfig.allyMode == AllyMode.FORCED_NONE ? 3 : 0;

		List<NetPlayer> players = new ArrayList<>();
		ClaimedChunkPlayerDataImpl server = FTBChunksAPIImpl.manager.getServerData();

		for (ClaimedChunkPlayerDataImpl p : FTBChunksAPIImpl.manager.playerData.values()) {
			if (p == server || p == self) {
				continue;
			}

			if (FTBChunksAPIImpl.manager.knownFakePlayers.containsKey(p.getUuid())) {
				continue;
			}

			int flags = 0;

			if (self.allies.contains(p.getUuid())) {
				flags |= NetPlayer.ALLY;
			}

			if (p.allies.contains(self.getUuid())) {
				flags |= NetPlayer.ALLY_BACK;
			}

			players.add(new NetPlayer(p.getUuid(), p.getName(), flags));
		}

		for (KnownFakePlayer p : FTBChunksAPIImpl.manager.knownFakePlayers.values()) {
			players.add(new NetPlayer(p.uuid, p.name, NetPlayer.FAKE | (p.banned ? NetPlayer.BANNED : 0) | (self.allies.contains(p.uuid) ? NetPlayer.ALLY : 0)));
		}

		players.sort(null);

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendPlayerListPacket(players, allyMode));
	}

	void write(FriendlyByteBuf buf) {
		buf.writeVarInt(players.size());

		for (NetPlayer p : players) {
			buf.writeLong(p.uuid.getMostSignificantBits());
			buf.writeLong(p.uuid.getLeastSignificantBits());
			buf.writeUtf(p.name, 50);
			buf.writeVarInt(p.flags);
		}

		buf.writeByte(allyMode);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.openPlayerList(this));
		context.get().setPacketHandled(true);
	}
}