package dev.ftb.mods.ftbchunks.core;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

public interface ClientboundSectionBlocksUpdatePacketFTBC {
	SectionPos getSectionPosFTBC();

	short[] getPositionsFTBC();

	BlockState[] getStatesFTBC();
}
