package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
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

	private int getHeight(@Nullable IChunk chunk, int blockX, int blockZ, int topY)
	{
		if (chunk == null)
		{
			return 70;
		}

		BlockPos.Mutable currentBlockPos = new BlockPos.Mutable();

		for (int by = topY; by > 0; by--)
		{
			currentBlockPos.setPos(blockX, by, blockZ);
			BlockState state = chunk.getBlockState(currentBlockPos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK)
			{
				for (; by > 0; by--)
				{
					currentBlockPos.setPos(blockX, by, blockZ);
					state = chunk.getBlockState(currentBlockPos);

					if (state.getBlock().isAir(state, chunk.getWorldForge(), currentBlockPos))
					{
						break;
					}
				}
			}

			if (!state.isAir(chunk.getWorldForge(), currentBlockPos))
			{
				return by;
			}
		}

		return 70;
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			int topY = p.world.func_234938_ad_() + 1; //getActualHeight
			int y = getHeight(p.world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true), x, z, topY) + 2;

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