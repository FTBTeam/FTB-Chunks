package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.data.HeightUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * @author LatvianModder
 */
public class TeleportFromMapPacket extends BaseC2SMessage {
	public final int x, y, z;
	public final boolean unknownY;
	public final ResourceKey<Level> dimension;

	public TeleportFromMapPacket(int _x, int _y, int _z, boolean uy, ResourceKey<Level> d) {
		x = _x;
		y = _y;
		z = _z;
		unknownY = uy;
		dimension = d;
	}

	TeleportFromMapPacket(FriendlyByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		unknownY = buf.readBoolean();
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.TELEPORT_FROM_MAP;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(unknownY);
		buf.writeResourceLocation(dimension.location());
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer p = (ServerPlayer) context.getPlayer();
		ServerLevel level = p.getServer().getLevel(dimension);

		if (level != null && p.hasPermissions(2)) {
			int y1 = y;

			if (unknownY) {
				ChunkAccess chunkAccess = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);

				if (chunkAccess == null) {
					return;
				}

				int topY = chunkAccess.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

				if (topY == chunkAccess.getMinBuildHeight() - 1) {
					return;
				}

				BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(x, topY + 2, z);
				int water = HeightUtils.getHeight(level, chunkAccess, blockPos);

				if (blockPos.getY() == HeightUtils.UNKNOWN) {
					blockPos.setY(70);
				} else if (water != HeightUtils.UNKNOWN) {
					blockPos.setY(Math.max(blockPos.getY(), water));
				}

				y1 = blockPos.getY() + 1;
			}

			p.teleportTo(level, x + 0.5D, y1 + 0.1D, z + 0.5D, p.getYRot(), p.getXRot());
		}
	}
}