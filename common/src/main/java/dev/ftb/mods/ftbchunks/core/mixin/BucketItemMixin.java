package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.FluidItemFTBC;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements FluidItemFTBC {
	@Override
	@Accessor("content")
	public abstract Fluid getFluidFTBC();
}
