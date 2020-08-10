package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.impl.PlayerLocation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendVisiblePlayerListPacket
{
	public final List<PlayerLocation> players;
	public final String dim;

	public SendVisiblePlayerListPacket(List<PlayerLocation> p, String d)
	{
		players = p;
		dim = d;
	}

	SendVisiblePlayerListPacket(PacketBuffer buf)
	{
		int s = buf.readVarInt();
		players = new ArrayList<>(s);

		for (int i = 0; i < s; i++)
		{
			PlayerLocation p = new PlayerLocation();
			long most = buf.readLong();
			long least = buf.readLong();
			p.uuid = new UUID(most, least);
			p.name = buf.readString(Short.MAX_VALUE);
			p.x = buf.readVarInt();
			p.z = buf.readVarInt();
			players.add(p);
		}

		dim = buf.readString(Short.MAX_VALUE);
	}

	public static void sendAll()
	{
		List<VisiblePlayerListItem> playerList = new ArrayList<>();

		for (ServerPlayerEntity player : FTBChunksAPIImpl.manager.server.getPlayerList().getPlayers())
		{
			VisiblePlayerListItem item = new VisiblePlayerListItem();
			item.player = player;
			item.data = FTBChunksAPIImpl.manager.getData(player);
			item.location = new PlayerLocation(player);
			playerList.add(item);
		}

		for (VisiblePlayerListItem self : playerList)
		{
			String dim = ChunkDimPos.getID(self.player.world);
			List<PlayerLocation> players = new ArrayList<>();

			for (VisiblePlayerListItem other : playerList)
			{
				if (other.player.world == self.player.world && self.data.canUse(other.player, self.data.locationMode, false))
				{
					players.add(other.location);
				}
			}

			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> self.player), new SendVisiblePlayerListPacket(players, dim));
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(players.size());

		for (PlayerLocation p : players)
		{
			buf.writeLong(p.uuid.getMostSignificantBits());
			buf.writeLong(p.uuid.getLeastSignificantBits());
			buf.writeString(p.name, Short.MAX_VALUE);
			buf.writeVarInt(p.x);
			buf.writeVarInt(p.z);
		}

		buf.writeString(dim, Short.MAX_VALUE);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateVisiblePlayerList(this));
		context.get().setPacketHandled(true);
	}
}