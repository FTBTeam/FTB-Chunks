package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class HeightUtils {
	public static final int UNKNOWN = Short.MIN_VALUE + 1;

	public static boolean isWater(BlockState state) {
		if (state.getBlock() == Blocks.WATER) {
			return true;
		}

		return state instanceof BlockStateFTBC ? ((BlockStateFTBC) state).getFTBCIsWater() : state.getFluidState().getType().isSame(Fluids.WATER);
	}

	public static boolean skipBlock(BlockState state) {
		return state.isAir() || FTBChunks.PROXY.skipBlock(state);
	}

	public static int getHeight(Level level, @Nullable ChunkAccess chunkAccess, BlockPos.MutableBlockPos pos) {
		if (chunkAccess == null) {
			return UNKNOWN;
		}

		int bottomY = chunkAccess.getMinBuildHeight();
		int topY = pos.getY();
		boolean hasCeiling = level.dimensionType().hasCeiling();
		int currentWaterY = UNKNOWN;

		outer:
		for (int by = topY; by >= bottomY; by--) {
			pos.setY(by);
			BlockState state = chunkAccess.getBlockState(pos);

			if (hasCeiling && (by == topY || state.getBlock() == Blocks.BEDROCK)) {
				for (; by > bottomY; by--) {
					pos.setY(by);
					state = chunkAccess.getBlockState(pos);

					if (skipBlock(state)) {
						continue outer;
					}
				}
			}

			boolean water = isWater(state);

			if (water && currentWaterY == UNKNOWN) {
				currentWaterY = by;
			}

			if (!water && !skipBlock(state)) {
				return currentWaterY;
			}
		}

		pos.setY(UNKNOWN);
		return UNKNOWN;
	}
}
