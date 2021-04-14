package dev.ftb.mods.ftbchunks.forge;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.extensions.IAbstractRailBlock;

public class FTBCUtilsImpl {
	public static boolean isRail(Block block) {
		return block instanceof IAbstractRailBlock;
	}
}
