package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class TeleportFromMapPacket
{
	public final int x, z;
	public final RegistryKey<World> dimension;

	public TeleportFromMapPacket(int _x, int _z, RegistryKey<World> d)
	{
		x = _x;
		z = _z;
		dimension = d;
	}

	TeleportFromMapPacket(PacketBuffer buf)
	{
		x = buf.readVarInt();
		z = buf.readVarInt();
		dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeResourceLocation(dimension.location());
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
			currentBlockPos.set(blockX, by, blockZ);
			BlockState state = chunk.getBlockState(currentBlockPos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK)
			{
				for (; by > 0; by--)
				{
					currentBlockPos.set(blockX, by, blockZ);
					state = chunk.getBlockState(currentBlockPos);

					if (state.getBlock().isAir(state, chunk.getWorldForge(), currentBlockPos))
					{
						break;
					}
				}
			}

			if (!state.isAir())
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
			int topY = p.level.getHeight() + 1; //getActualHeight
			int y = getHeight(p.level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true), x, z, topY) + 2;

			ServerWorld world = p.getServer().getLevel(dimension);

			if (world != null && p.hasPermissions(2))
			{
				p.teleportTo(world, x + 0.5D, y + 0.1D, z + 0.5D, p.yRot, p.xRot);
			}
		});
		context.get().setPacketHandled(true);
	}
}