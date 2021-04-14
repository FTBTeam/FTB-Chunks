package dev.ftb.mods.ftbchunks.fabric;

import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;

public class FTBCUtilsImpl {
	public static boolean isRail(Block block) {
		return block instanceof BaseRailBlock;
	}
}
