package dev.ftb.mods.ftbchunks.core;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Comparator;

public class ChunkLoadingHelper {
	public static final TicketType<ChunkPos> FTBCHUNKS_FORCE_LOADED = TicketType.create(FTBChunks.MOD_ID + ":force_loaded", Comparator.comparingLong(ChunkPos::toLong));

	public static boolean anyPlayerCloseEnoughForSpawning(ResourceKey<Level> dimension, ChunkPos pos) {
		return FTBChunksAPI.isChunkForceLoaded(dimension, pos.x, pos.z, 1);
	}

	public static boolean isPositionEntityTickingChunk(ResourceKey<Level> dimension, ChunkPos pos) {
		return FTBChunksAPI.isChunkForceLoaded(dimension, pos.x, pos.z, 2);
	}

	public static boolean isPositionEntityTickingBlock(ResourceKey<Level> dimension, BlockPos pos) {
		int x = pos.getX() >> 4;
		int z = pos.getZ() >> 4;
		return FTBChunksAPI.isChunkForceLoaded(dimension, x, z, 3);
	}

	public static boolean shouldTickBlocksAt(ResourceKey<Level> dimension, long pos) {
		int x = (int) pos;
		int z = (int) (pos >> 32);
		return FTBChunksAPI.isChunkForceLoaded(dimension, x, z, 4);
	}

	public static boolean isPositionTickingWithEntitiesLoaded(ResourceKey<Level> dimension, long pos) {
		int x = (int) pos;
		int z = (int) (pos >> 32);
		return FTBChunksAPI.isChunkForceLoaded(dimension, x, z, 5);
	}
}
