package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.CustomMinYRegistryImpl;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class HeightUtils {
	public static final int UNKNOWN = Short.MIN_VALUE + 1;

	public static boolean isWater(@Nullable BlockState state) {
		if (state == null) {
			// shouldn't happen, but https://github.com/FTBTeam/FTB-Mods-Issues/issues/1599
			return false;
		}

		if (state.getBlock() == Blocks.WATER) {
			return true;
		}

		return state instanceof BlockStateFTBC ftbc ? ftbc.ftbc$isWater() : state.getFluidState().getType().isSame(Fluids.WATER);
	}

	public static boolean skipBlock(Level level, BlockState state) {
		if (level.isClientSide()) {
			return state.isAir() || FTBChunksClient.INSTANCE.skipBlock(state);
		} else {
			return false;
		}
	}

	public static int getHeight(Level level, @Nullable ChunkAccess chunkAccess, BlockPos.MutableBlockPos pos) {
		if (chunkAccess == null) {
			return UNKNOWN;
		}

		int startY = CustomMinYRegistryImpl.getInstance(level.isClientSide()).getMinYAt(level, pos);
		int bottomY = Mth.clamp(startY, chunkAccess.getMinY(), chunkAccess.getMaxY());

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

					if (skipBlock(level, state)) {
						continue outer;
					}
				}
			}

			boolean water = isWater(state);

			if (water && currentWaterY == UNKNOWN) {
				currentWaterY = by;
			}

			if (!water && !skipBlock(level, state)) {
				return currentWaterY;
			}
		}

		pos.setY(UNKNOWN);
		return UNKNOWN;
	}
}
