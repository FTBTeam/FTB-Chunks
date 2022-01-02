package dev.ftb.mods.ftbchunks.core;

import net.minecraft.core.SectionPos;

public interface ClientboundSectionBlocksUpdatePacketFTBC {
	SectionPos getSectionPosFTBC();

	short[] getPositionsFTBC();
}
