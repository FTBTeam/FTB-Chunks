package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.impl.map.MapChunk;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class TeleportFromMapPacket
{
	public final int x, z;

	public TeleportFromMapPacket(int _x, int _z)
	{
		x = _x;
		z = _z;
	}

	TeleportFromMapPacket(PacketBuffer buf)
	{
		x = buf.readVarInt();
		z = buf.readVarInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(x);
		buf.writeVarInt(z);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			int topY = p.world.getActualHeight() + 1;
			int y = MapChunk.getHeight(p.world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true), new BlockPos.Mutable(), x, z, topY) + 2;

			try
			{
				p.server.getCommandManager().handleCommand(p.getCommandSource(), "/teleport " + x + " " + y + " " + z);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		});
		context.get().setPacketHandled(true);
	}
}