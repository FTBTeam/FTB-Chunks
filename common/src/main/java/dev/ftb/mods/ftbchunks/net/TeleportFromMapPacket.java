package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

public record TeleportFromMapPacket(BlockPos pos, boolean unknownY, ResourceKey<Level> dimension) implements CustomPacketPayload {
	public static final Type<TeleportFromMapPacket> TYPE = new Type<>(FTBChunksAPI.id("teleport_from_map_packet"));

	public static final StreamCodec<FriendlyByteBuf, TeleportFromMapPacket> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TeleportFromMapPacket::pos,
			ByteBufCodecs.BOOL, TeleportFromMapPacket::unknownY,
			ResourceKey.streamCodec(Registries.DIMENSION), TeleportFromMapPacket::dimension,
			TeleportFromMapPacket::new
	);

	@Override
	public Type<TeleportFromMapPacket> type() {
		return TYPE;
	}

	public static void handle(TeleportFromMapPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> {
			ServerPlayer p = (ServerPlayer) context.getPlayer();
			ServerLevel level = p.level().getServer().getLevel(message.dimension);

			if (level != null && p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				int x1 = message.pos.getX();
				int y1 = message.pos.getY();
				int z1 = message.pos.getZ();

				if (message.unknownY) {
					ChunkAccess chunkAccess = level.getChunkAt(message.pos);

					int topY = chunkAccess.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x1, z1);
					if (topY == chunkAccess.getMinY() - 1) {
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

                // TODO: [21.8] false was a guess, no clue what the set does. Looks like how it keeps a specific velocity?
				p.teleportTo(level, x1 + 0.5D, y1 + 0.1D, z1 + 0.5D, Set.of(), p.getYRot(), p.getXRot(), false);
			}
		});
	}

}
