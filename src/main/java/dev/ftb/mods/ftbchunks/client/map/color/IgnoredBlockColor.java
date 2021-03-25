package dev.ftb.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * @author LatvianModder
 */
public class IgnoredBlockColor implements BlockColor {
	@Override
	public Color4I getBlockColor(BlockAndTintGetter world, BlockPos pos) {
		return Color4I.BLACK;
	}

	@Override
	public boolean isIgnored() {
		return true;
	}
}
