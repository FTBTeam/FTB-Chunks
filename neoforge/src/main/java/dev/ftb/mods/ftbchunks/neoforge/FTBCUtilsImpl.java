package dev.ftb.mods.ftbchunks.neoforge;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IBaseRailBlockExtension;

public class FTBCUtilsImpl {
	public static boolean isRail(Block block) {
		return block instanceof IBaseRailBlockExtension;
	}
}
