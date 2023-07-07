package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class BlockStateMixin implements BlockStateFTBC {
	private Boolean cachedFTBCIsWater;

	@Override
	public boolean getFTBCIsWater() {
		if (cachedFTBCIsWater == null) {
			cachedFTBCIsWater = ((BlockState) (Object) this).getFluidState().getType().isSame(Fluids.WATER);
		}

		return cachedFTBCIsWater;
	}
}
