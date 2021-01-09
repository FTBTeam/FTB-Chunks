package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.core.BlockStateFTBC;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author LatvianModder
 */
@Mixin(BlockState.class)
public abstract class BlockStateMixin implements BlockStateFTBC
{
	private Boolean cachedFTBCIsWater;

	@Override
	public boolean getFTBCIsWater()
	{
		if (cachedFTBCIsWater == null)
		{
			cachedFTBCIsWater = ((BlockState) (Object) this).getFluidState().getType().isSame(Fluids.WATER);
		}

		return cachedFTBCIsWater;
	}
}
