package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class TeleportFromMapPacket extends BasePacket {
	public final int x, z;
	public final ResourceKey<Level> dimension;

	public TeleportFromMapPacket(int _x, int _z, ResourceKey<Level> d) {
		x = _x;
		z = _z;
		dimension = d;
	}

	TeleportFromMapPacket(FriendlyByteBuf buf) {
		x = buf.readVarInt();
		z = buf.readVarInt();
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
	}

	@Override
	public PacketID getId() {
		return FTBChunksNet.TELEPORT_FROM_MAP;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeResourceLocation(dimension.location());
	}

	private int getHeight(@Nullable ChunkAccess chunk, int blockX, int blockZ, int topY) {
		if (chunk == null) {
			return 70;
		}

		BlockPos.MutableBlockPos currentBlockPos = new BlockPos.MutableBlockPos();

		for (int by = topY; by > 0; by--) {
			currentBlockPos.set(blockX, by, blockZ);
			BlockState state = chunk.getBlockState(currentBlockPos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK) {
				for (; by > 0; by--) {
					currentBlockPos.set(blockX, by, blockZ);
					state = chunk.getBlockState(currentBlockPos);

					if (state.isAir()) {
						break;
					}
				}
			}

			if (!state.isAir()) {
				return by;
			}
		}

		return 70;
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer p = (ServerPlayer) context.getPlayer();
		int topY = p.level.getHeight() + 1; //getActualHeight
		int y = getHeight(p.level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true), x, z, topY) + 2;

		ServerLevel world = p.getServer().getLevel(dimension);

		if (world != null && p.hasPermissions(2)) {
			p.teleportTo(world, x + 0.5D, y + 0.1D, z + 0.5D, p.yRot, p.xRot);
		}
	}
}