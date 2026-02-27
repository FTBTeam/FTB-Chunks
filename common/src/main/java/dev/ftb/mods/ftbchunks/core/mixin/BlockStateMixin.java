package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockState.class)
public abstract class BlockStateMixin implements BlockStateFTBC {
	@Unique
	@Nullable
	private Boolean ftbc$cachedIsWater;

	@Override
	public boolean ftbc$isWater() {
		if (ftbc$cachedIsWater == null) {
			ftbc$cachedIsWater = ((BlockState) (Object) this).getFluidState().getType().isSame(Fluids.WATER);
		}

		return ftbc$cachedIsWater;
	}
}
