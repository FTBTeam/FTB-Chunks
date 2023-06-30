package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * @author LatvianModder
 */
public class TeleportFromMapPacket extends BaseC2SMessage {
	private final BlockPos pos;
	private final boolean unknownY;
	private final ResourceKey<Level> dimension;

	public TeleportFromMapPacket(BlockPos pos, boolean unknownY, ResourceKey<Level> dimension) {
		this.pos = pos;
		this.unknownY = unknownY;
		this.dimension = dimension;
	}

	TeleportFromMapPacket(FriendlyByteBuf buf) {
		pos = buf.readBlockPos();
		unknownY = buf.readBoolean();
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.TELEPORT_FROM_MAP;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeBoolean(unknownY);
		buf.writeResourceLocation(dimension.location());
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer p = (ServerPlayer) context.getPlayer();
		ServerLevel level = p.getServer().getLevel(dimension);

		if (level != null && p.hasPermissions(2)) {
			int x1 = pos.getX();
			int y1 = pos.getY();
			int z1 = pos.getZ();

			if (unknownY) {
				ChunkAccess chunkAccess = level.getChunkAt(pos);

				int topY = chunkAccess.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x1, z1);
				if (topY == chunkAccess.getMinBuildHeight() - 1) {
					return;
				}

				BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(x1, topY + 2, z1);
				int water = HeightUtils.getHeight(level, chunkAccess, blockPos);

				if (blockPos.getY() == HeightUtils.UNKNOWN) {
					blockPos.setY(70);
				} else if (water != HeightUtils.UNKNOWN) {
					blockPos.setY(Math.max(blockPos.getY(), water));
				}

				y1 = blockPos.getY() + 1;
			}

			p.teleportTo(level, x1 + 0.5D, y1 + 0.1D, z1 + 0.5D, p.getYRot(), p.getXRot());
		}
	}
}